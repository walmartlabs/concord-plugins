package com.walmartlabs.concord.plugins.terraform;

import com.walmartlabs.concord.plugins.terraform.actions.*;
import com.walmartlabs.concord.plugins.terraform.backend.Backend;
import com.walmartlabs.concord.sdk.Context;
import com.walmartlabs.concord.sdk.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public final class TerraformTaskCommon {

    private static final Logger log = LoggerFactory.getLogger(TerraformTaskCommon.class);

    public static TerraformActionResult execute(Terraform terraform, Action action, Backend backend, Map<String, Object> cfg, Map<String, String> env) throws Exception {
        log.info("Starting {}...", action);

        try {
            backend.lock();

            switch (action) {
                case PLAN: {
                    PlanAction a = new PlanAction(cfg, env);
                    return a.exec(terraform, backend);
                }
                case APPLY: {
                    ApplyAction a = new ApplyAction(cfg, env);
                    return a.exec(terraform, backend);
                }
                case DESTROY: {
                    DestroyAction a = new DestroyAction(cfg, env);
                    return a.exec(terraform, backend);
                }
                case OUTPUT: {
                    OutputAction a = new OutputAction(cfg, env, false);
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
