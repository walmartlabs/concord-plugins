package com.walmartlabs.concord.plugins.terraform;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2020 Walmart Inc., Concord Authors
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

import com.walmartlabs.concord.plugins.terraform.actions.*;
import com.walmartlabs.concord.plugins.terraform.backend.Backend;
import com.walmartlabs.concord.sdk.Context;
import com.walmartlabs.concord.sdk.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public final class TerraformTaskCommon {

    private static final Logger log = LoggerFactory.getLogger(TerraformTaskCommon.class);

    public static TerraformActionResult execute(Terraform terraform,
                                                Action action,
                                                Backend backend,
                                                Path workDir,
                                                Map<String, Object> cfg,
                                                Map<String, String> env) throws Exception {
        log.info("Starting {}...", action);

        try {
            backend.lock();

            switch (action) {
                case PLAN: {
                    PlanAction a = new PlanAction(workDir, cfg, env);
                    return a.exec(terraform, backend);
                }
                case APPLY: {
                    ApplyAction a = new ApplyAction(workDir, cfg, env);
                    return a.exec(terraform, backend);
                }
                case DESTROY: {
                    DestroyAction a = new DestroyAction(workDir, cfg, env);
                    return a.exec(terraform, backend);
                }
                case OUTPUT: {
                    OutputAction a = new OutputAction(workDir, cfg, env, false);
                    return a.exec(terraform, backend);
                }
                default:
                    throw new IllegalArgumentException("Unsupported action type: " + action);
            }
        } finally {
            backend.unlock();
        }
    }

    public static Map<String, String> getEnv(Map<String, Object> cfg, Backend backend) throws Exception {
        Map<String, String> m = new HashMap<>();

        // default env
        m.putAll(MapUtils.getMap(cfg, TaskConstants.DEFAULT_ENV_KEY, Collections.emptyMap()));

        // backend-specific env
        m.putAll(backend.prepareEnv(cfg));

        // user env
        Map<String, String> extraEnv = MapUtils.getMap(cfg, TaskConstants.EXTRA_ENV_KEY, Collections.emptyMap());
        m.putAll(extraEnv);

        return m;
    }

    public static Action getAction(Map<String, Object> cfg) {
        Object v = cfg.get(TaskConstants.ACTION_KEY);
        if (v == null) {
            throw new IllegalArgumentException("'" + TaskConstants.ACTION_KEY + "' is required");
        }

        if (v instanceof String) {
            String s = (String) v;
            return Action.valueOf(s.trim().toUpperCase());
        }

        throw new IllegalArgumentException("'" + TaskConstants.ACTION_KEY + "' must be a string. Allowed values: " +
                Arrays.stream(Action.values()).map(Enum::name).collect(Collectors.joining(", ")));
    }

    public static void put(Map<String, Object> m, String k, Context ctx) {
        Object v = ctx.getVariable(k);
        if (v == null) {
            return;
        }

        m.put(k, v);
    }

    private TerraformTaskCommon() {
    }
}
