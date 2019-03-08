package com.walmartlabs.concord.plugins.terraform;

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
import com.walmartlabs.concord.plugins.terraform.actions.ApplyAction;
import com.walmartlabs.concord.plugins.terraform.actions.ApplyResult;
import com.walmartlabs.concord.plugins.terraform.actions.PlanAction;
import com.walmartlabs.concord.plugins.terraform.actions.PlanResult;
import com.walmartlabs.concord.plugins.terraform.backend.Backend;
import com.walmartlabs.concord.plugins.terraform.backend.ConcordBackend;
import com.walmartlabs.concord.plugins.terraform.backend.DummyBackend;
import com.walmartlabs.concord.sdk.*;

import javax.inject.Inject;
import javax.inject.Named;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static com.walmartlabs.concord.plugins.terraform.Utils.getPath;

@Named("terraform")
public class TerraformTask implements Task {

    private static final String DEFAULT_BACKEND = "concord";

    private final LockService lockService;
    private final ObjectStorage objectStorage;
    private final SecretService secretService;
    private final ObjectMapper objectMapper;

    @InjectVariable("terraformParams")
    private Map<String, Object> defaults;

    @Inject
    public TerraformTask(LockService lockService, ObjectStorage objectStorage, SecretService secretService) {
        this.lockService = lockService;
        this.objectStorage = objectStorage;
        this.secretService = secretService;
        this.objectMapper = new ObjectMapper();
    }

    public void execute(Context ctx) throws Exception {
        String instanceId = (String) ctx.getVariable(com.walmartlabs.concord.sdk.Constants.Context.TX_ID_KEY);

        Map<String, Object> cfg = createCfg(ctx);
        Map<String, String> env = getEnv(cfg);

        Path workDir = getPath(cfg, com.walmartlabs.concord.sdk.Constants.Context.WORK_DIR_KEY, null);
        if (workDir == null) {
            throw new IllegalArgumentException("Can't determine the current '" + com.walmartlabs.concord.sdk.Constants.Context.WORK_DIR_KEY + "'");
        }

        boolean debug = MapUtils.get(cfg, Constants.DEBUG_KEY, false, Boolean.class);

        Action action = getAction(cfg);
        Backend backend = getBackend(cfg);

        GitSshWrapper gitSshWrapper = GitSshWrapper.createFrom(secretService, ctx, instanceId, workDir, cfg, debug);
        Map<String, String> baseEnv = gitSshWrapper.updateEnv(workDir, new HashMap<>());

        Terraform terraform = new Terraform(workDir, debug, baseEnv);
        if (debug) {
            terraform.exec(workDir, "version", "version");
        }

        try {
            backend.lock(ctx);

            switch (action) {
                case PLAN: {
                    PlanAction a = new PlanAction(ctx, cfg, env);
                    PlanResult result = a.exec(terraform, backend);
                    ctx.setVariable(Constants.RESULT_KEY, objectMapper.convertValue(result, Map.class));
                    break;
                }
                case APPLY: {
                    ApplyAction a = new ApplyAction(ctx, cfg, env);
                    ApplyResult result = a.exec(terraform, backend);
                    ctx.setVariable(Constants.RESULT_KEY, objectMapper.convertValue(result, Map.class));
                    break;
                }
                default:
                    throw new IllegalArgumentException("Unsupported action type: " + action);
            }
        } finally {
            backend.unlock(ctx);
            gitSshWrapper.cleanup();
        }
    }

    private Backend getBackend(Map<String, Object> cfg) {
        boolean debug = MapUtils.get(cfg, Constants.DEBUG_KEY, false, Boolean.class);
        String s = MapUtils.getString(cfg, Constants.BACKEND_KEY, DEFAULT_BACKEND);
        switch (s) {
            case "none": {
                return new DummyBackend();
            }
            case "concord": {
                return new ConcordBackend(debug, lockService, objectStorage, objectMapper);
            }
            default: {
                throw new IllegalArgumentException("Unknown backend type: " + s);
            }
        }
    }

    private Map<String, Object> createCfg(Context ctx) {
        Map<String, Object> m = new HashMap<>(defaults != null ? defaults : Collections.emptyMap());

        put(m, com.walmartlabs.concord.sdk.Constants.Context.WORK_DIR_KEY, ctx);
        for (String k : Constants.ALL_IN_PARAMS) {
            put(m, k, ctx);
        }

        return m;
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> getEnv(Map<String, Object> cfg) {
        Map<String, String> defaultEnv = (Map<String, String>) cfg.getOrDefault(Constants.DEFAULT_ENV_KEY, Collections.emptyMap());
        Map<String, String> extraEnv = (Map<String, String>) cfg.getOrDefault(Constants.EXTRA_ENV_KEY, Collections.emptyMap());

        Map<String, String> m = new HashMap<>(defaultEnv);
        m.putAll(extraEnv);

        return m;
    }

    private static Action getAction(Map<String, Object> cfg) {
        Object v = cfg.get(Constants.ACTION_KEY);
        if (v == null) {
            throw new IllegalArgumentException("'" + Constants.ACTION_KEY + "' is required");
        }

        if (v instanceof String) {
            String s = (String) v;
            return Action.valueOf(s.trim().toUpperCase());
        }

        throw new IllegalArgumentException("'" + Constants.ACTION_KEY + "' must be a string. Allowed values: " +
                Arrays.stream(Action.values()).map(Enum::name).collect(Collectors.joining(", ")));
    }

    private static void put(Map<String, Object> m, String k, Context ctx) {
        Object v = ctx.getVariable(k);
        if (v == null) {
            return;
        }

        m.put(k, v);
    }

    public enum Action {
        APPLY,
        PLAN
    }
}
