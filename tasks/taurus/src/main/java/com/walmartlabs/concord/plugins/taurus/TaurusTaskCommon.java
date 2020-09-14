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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.walmartlabs.concord.plugins.taurus.TaskParams.Action;
import static com.walmartlabs.concord.plugins.taurus.TaskParams.RunParams;

public class TaurusTaskCommon {

    private static final Logger log = LoggerFactory.getLogger(TaurusTaskCommon.class);

    private static final String CONFIGS_KEY = "configs";

    private static final String TMP_CONFIG_DIR = ".bzt/tmp";
    private static final String RUN_ACTION_LOG_PREFIX = "\u001b[35mrun\u001b[0m";

    private final Path workDir;

    public TaurusTaskCommon(Path workDir) {
        this.workDir = workDir;
    }

    public Taurus.Result execute(TaskParams in) throws Exception {

        // we're going to use a fake JMeter's PluginsManagerCMD script unless users specifically
        // ask us to download JMX plugins
        Taurus taurus = new Taurus(workDir, in.useFakeHome(), !in.downloadPlugins());

        Action action = in.action();
        switch (action) {
            case RUN: {
                return run(taurus, (RunParams)in);
            }
            default:
                throw new IllegalArgumentException("Unsupported action type: " + action);
        }
    }

    private Taurus.Result run(Taurus taurus, RunParams in) throws IOException {
        List<Path> configPaths = processConfigs(workDir, in);

        try {
            List<String> args = new ArrayList<>();

            for (Path p : configPaths) {
                args.add(p.toAbsolutePath().toString());
            }

            if (in.verbose()) {
                args.add("--verbose");
            } else if (in.quiet()) {
                args.add("--quiet");
            }

            if (in.noSysConfig()) {
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
            if (in.ignoreErrors()) {
                log.warn("Finished with a generic error (networking or internal Taurus errors). For details check for the 'ERROR' in logs: {}", e.getMessage());
                return Taurus.Result.error(e.getMessage());
            }

            throw new RuntimeException("Error occurred while executing Taurus: " + e.getMessage(), e);
        }
    }

    private static Map<String, Object> createDefaultConfig(Path workDir, RunParams in) {
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
        String proxy = in.proxy();
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

    private static List<Path> processConfigs(Path workDir, RunParams in) throws IOException {
        List<Path> result = new ArrayList<>();
        result.add(processConfig(workDir, createDefaultConfig(workDir, in)));

        List<Object> configs = in.configs();
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

}
