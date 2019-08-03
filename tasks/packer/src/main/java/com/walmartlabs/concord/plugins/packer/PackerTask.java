package com.walmartlabs.concord.plugins.packer;

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

import static com.walmartlabs.concord.plugins.packer.Utils.getPath;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.walmartlabs.concord.plugins.packer.actions.BuildAction;
import com.walmartlabs.concord.plugins.packer.actions.BuildResult;
import com.walmartlabs.concord.sdk.Context;
import com.walmartlabs.concord.sdk.InjectVariable;
import com.walmartlabs.concord.sdk.MapUtils;
import com.walmartlabs.concord.sdk.Task;

@Named("packer")
public class PackerTask implements Task {

    private static final Logger log = LoggerFactory.getLogger(PackerTask.class);

    private final ObjectMapper objectMapper;

    @InjectVariable("packerParams")
    private Map<String, Object> defaults;

    @Inject
    public PackerTask() {
        this.objectMapper = new ObjectMapper();
    }

    public void execute(Context ctx) throws Exception {
        Map<String, Object> cfg = createCfg(ctx);
        Map<String, String> env = getEnv(cfg);

        Path workDir = getPath(cfg, com.walmartlabs.concord.sdk.Constants.Context.WORK_DIR_KEY, null);
        if (workDir == null) {
            throw new IllegalArgumentException("Can't determine the current '" + com.walmartlabs.concord.sdk.Constants.Context.WORK_DIR_KEY + "'");
        }

        boolean debug = MapUtils.get(cfg, com.walmartlabs.concord.plugins.packer.Constants.DEBUG_KEY, false, Boolean.class);

        Map<String, String> baseEnv = Collections.emptyMap();

        Packer packer = new Packer(workDir, debug, baseEnv);
        if (debug) {
            packer.exec(workDir, "version", "version");
        }

        log.info("Starting build...");

        BuildAction buildAction = new BuildAction(cfg, env);
        BuildResult buildResult = buildAction.exec(packer);
        ctx.setVariable(com.walmartlabs.concord.plugins.packer.Constants.RESULT_KEY, objectMapper.convertValue(buildResult, Map.class));
    }

    private Map<String, Object> createCfg(Context ctx) {
        Map<String, Object> m = new HashMap<>(defaults != null ? defaults : Collections.emptyMap());

        put(m, com.walmartlabs.concord.sdk.Constants.Context.WORK_DIR_KEY, ctx);
        for (String k : com.walmartlabs.concord.plugins.packer.Constants.ALL_IN_PARAMS) {
            put(m, k, ctx);
        }

        return m;
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> getEnv(Map<String, Object> cfg) {
        Map<String, String> defaultEnv = (Map<String, String>) cfg.getOrDefault(
                com.walmartlabs.concord.plugins.packer.Constants.DEFAULT_ENV_KEY, Collections.emptyMap());
        Map<String, String> extraEnv = (Map<String, String>) cfg.getOrDefault(
                com.walmartlabs.concord.plugins.packer.Constants.EXTRA_ENV_KEY, Collections.emptyMap());

        Map<String, String> m = new HashMap<>(defaultEnv);
        m.putAll(extraEnv);

        return m;
    }

    private static void put(Map<String, Object> m, String k, Context ctx) {
        Object v = ctx.getVariable(k);
        if (v == null) {
            return;
        }

        m.put(k, v);
    }
}
