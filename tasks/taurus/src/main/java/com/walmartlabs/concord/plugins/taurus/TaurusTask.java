package com.walmartlabs.concord.plugins.taurus;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2018 Walmart Inc.
 * -----
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =====
 */

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.walmartlabs.concord.sdk.Context;
import com.walmartlabs.concord.sdk.InjectVariable;
import com.walmartlabs.concord.sdk.MapUtils;
import com.walmartlabs.concord.sdk.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.*;

@Named("taurus")
public class TaurusTask implements Task {

    private static final Logger log = LoggerFactory.getLogger(TaurusTask.class);

    private static final String ACTION_KEY = "action";
    private static final String CONFIGS_KEY = "configs";
    private static final String DOWNLOAD_PLUGINS = "downloadPlugins";
    private static final String IGNORE_ERRORS_KEY = "ignoreErrors";
    private static final String NO_SYS_CONFIG_KEY = "noSysConfig";
    private static final String PROXY_KEY = "proxy";
    private static final String QUIET_KEY = "quiet";
    private static final String USE_FAKE_HOME_KEY = "useFakeHome";
    private static final String VERBOSE_KEY = "verbose";

    private static final String[] ALL_IN_PARAMS = {
            ACTION_KEY,
            CONFIGS_KEY,
            IGNORE_ERRORS_KEY,
            NO_SYS_CONFIG_KEY,
            PROXY_KEY,
            QUIET_KEY,
            USE_FAKE_HOME_KEY,
            VERBOSE_KEY
    };

    private static final String TMP_CONFIG_DIR = ".bzt/tmp";
    private static final String RUN_ACTION_LOG_PREFIX = "\u001b[35mrun\u001b[0m";

    @InjectVariable("taurusParams")
    private Map<String, Object> defaults;

    @Override
    public void execute(Context ctx) throws Exception {
        Path workDir = Paths.get((String) ctx.getVariable(com.walmartlabs.concord.sdk.Constants.Context.WORK_DIR_KEY));
        Map<String, Object> cfg = createCfg(ctx);

        boolean useFakeHome = MapUtils.getBoolean(cfg, USE_FAKE_HOME_KEY, true);
        boolean downloadPlugins = MapUtils.getBoolean(cfg, DOWNLOAD_PLUGINS, false);

        // we're going to use a fake JMeter's PluginsManagerCMD script unless users specifically
        // ask us to download JMX plugins
        Taurus taurus = new Taurus(workDir, useFakeHome, !downloadPlugins);

        Action action = getAction(cfg);
        switch (action) {
            case RUN: {
                Taurus.Result r = run(taurus, workDir, cfg);
                ObjectMapper om = new ObjectMapper();
                ctx.setVariable("result", om.convertValue(r, Map.class));
                break;
            }
            default:
                throw new IllegalArgumentException("Unsupported action type: " + action);
        }
    }

    private Taurus.Result run(Taurus taurus, Path workDir, Map<String, Object> cfg) throws IOException {
        boolean ignoreErrors = MapUtils.getBoolean(cfg, IGNORE_ERRORS_KEY, false);
        boolean verbose = MapUtils.getBoolean(cfg, VERBOSE_KEY, false);
        boolean quiet = MapUtils.getBoolean(cfg, QUIET_KEY, false);
        boolean noSysConfig = MapUtils.getBoolean(cfg, NO_SYS_CONFIG_KEY, false);

        List<Path> configPaths = processConfigs(workDir, cfg);

        try {
            List<String> args = new ArrayList<>();

            for (Path p : configPaths) {
                args.add(p.toAbsolutePath().toString());
            }

            if (verbose) {
                args.add("--verbose");
            } else if (quiet) {
                args.add("--quiet");
            }

            if (noSysConfig) {
                args.add("--no-system-configs");
            }

            log.info("Starting Taurus execution....");
            Taurus.Result r = taurus.exec(args, RUN_ACTION_LOG_PREFIX);
            if (r.getCode() != 0) {
                throw new RuntimeException("Taurus execution finished with code " + r.getCode() + ": " + r.getStderr());
            }

            log.info("No problems occured during taurus execution. Completed with code: " + r.getCode());
            return r;
        } catch (Exception e) {
            if (ignoreErrors) {
                log.warn("Finished with a generic error (networking or internal Taurus errors). For details check for the 'ERROR' in logs: {}", e.getMessage());
                return Taurus.Result.error(e.getMessage());
            }

            throw new RuntimeException("Error occured while executing Taurus: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> createCfg(Context ctx) {
        Map<String, Object> m = new HashMap<>(defaults != null ? defaults : Collections.emptyMap());
        for (String k : ALL_IN_PARAMS) {
            Object v = ctx.getVariable(k);
            if (v != null) {
                m.put(k, v);
            }
        }

        return m;
    }

    private static Action getAction(Map<String, Object> cfg) {
        Object v = cfg.get(ACTION_KEY);

        if (v instanceof String) {
            String s = (String) v;
            return Action.valueOf(s.trim().toUpperCase());
        }

        throw new IllegalArgumentException("'" + ACTION_KEY + "' must be a string");
    }

    private static Map<String, Object> createDefaultConfig(Path workDir, Map<String, Object> cfg) {
        Map<String, Object> settings = new HashMap<>();

        // disable the check for updates
        settings.put("check-updates", false);

        // set default executor
        settings.put("default-executor", Constants.DEFAULT_EXECUTOR);

        // set artifacts directory
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd_HHmmss");
        Path artifactsDir = workDir.resolve("_attachments/taurus_" + fmt.format(new Date()));
        settings.put("artifacts-dir", artifactsDir.toString());

        // set the proxy
        String proxy = MapUtils.getString(cfg, PROXY_KEY);
        if (proxy != null) {
            settings.put("proxy", Collections.singletonMap("address", proxy));
        }

        Map<String, Object> jmeter = new HashMap<>();

        // set the path to JMeter
        Path jmeterPath = workDir.resolve(".bzt/jmeter-taurus/apache-jmeter-" + Version.getJMeterVersion() + "/bin/jmeter");
        jmeter.put("path", jmeterPath.toString());

        // set the JMeter's download link to a local copy
        jmeter.put("download-link", "file://" + workDir + "/apache-jmeter.zip");

        Map<String, Object> modules = Collections.singletonMap("jmeter", jmeter);

        // create the resulting configuration object
        Map<String, Object> result = new HashMap<>();
        result.put("settings", settings);
        result.put("modules", modules);

        return result;
    }

    private static List<Path> processConfigs(Path workDir, Map<String, Object> cfg) throws IOException {
        List<Path> result = new ArrayList<>();
        result.add(processConfig(workDir, createDefaultConfig(workDir, cfg)));

        List<Object> configs = MapUtils.assertList(cfg, CONFIGS_KEY);
        for (Object c : configs) {
            result.add(processConfig(workDir, c));
        }

        return result;
    }

    private static Path processConfig(Path workDir, Object v) throws IOException {
        if (v instanceof String) {
            Path p = workDir.resolve((String) v);
            if (!Files.exists(p)) {
                throw new IllegalArgumentException("'" + CONFIGS_KEY + "' file not found: " + v);
            }
            return p;
        }

        if (v instanceof Map) {
            Path p = Files.createTempFile(Utils.assertDir(workDir, TMP_CONFIG_DIR), "config", ".yml");
            try (OutputStream out = Files.newOutputStream(p, StandardOpenOption.TRUNCATE_EXISTING)) {
                ObjectMapper om = new ObjectMapper(new YAMLFactory());
                om.writeValue(out, v);
            }
            return p;
        }

        throw new IllegalArgumentException("'" + CONFIGS_KEY + "' entries must be relative file paths or JSON/YAML objects. Got: " + v);
    }

    private enum Action {
        RUN
    }
}
