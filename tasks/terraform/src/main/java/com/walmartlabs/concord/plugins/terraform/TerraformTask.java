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
import com.walmartlabs.concord.plugins.terraform.actions.*;
import com.walmartlabs.concord.plugins.terraform.backend.Backend;
import com.walmartlabs.concord.plugins.terraform.backend.BackendManager;
import com.walmartlabs.concord.sdk.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.net.URI;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static com.walmartlabs.concord.plugins.terraform.Utils.getPath;

@Named("terraform")
public class TerraformTask implements Task {

    private static final Logger log = LoggerFactory.getLogger(TerraformTask.class);

    private static final String DEFAULT_TERRAFORM_VERSION = "0.12.5";
    private static final String DEFAULT_TOOL_URL_TEMPLATE = "https://releases.hashicorp.com/terraform/%s/terraform_%s_%s_amd64.zip";

    private final SecretService secretService;
    private final BackendManager backendManager;
    private final ObjectMapper objectMapper;
    private final DependencyManager dependencyManager;

    @InjectVariable("terraformParams")
    private Map<String, Object> defaults;

    @Inject
    public TerraformTask(SecretService secretService, BackendManager backendManager, DependencyManager dependencyManager) {
        this.secretService = secretService;
        this.backendManager = backendManager;
        this.dependencyManager = dependencyManager;
        this.objectMapper = new ObjectMapper();
    }

    public void execute(Context ctx) throws Exception {
        String instanceId = (String) ctx.getVariable(com.walmartlabs.concord.sdk.Constants.Context.TX_ID_KEY);

        Map<String, Object> cfg = createCfg(ctx);

        Path workDir = getPath(cfg, com.walmartlabs.concord.sdk.Constants.Context.WORK_DIR_KEY, null);
        if (workDir == null) {
            throw new IllegalArgumentException("Can't determine the current '" + com.walmartlabs.concord.sdk.Constants.Context.WORK_DIR_KEY + "'");
        }

        boolean debug = MapUtils.get(cfg, Constants.DEBUG_KEY, false, Boolean.class);

        Action action = getAction(cfg);
        Backend backend = backendManager.getBackend(cfg);
        Map<String, String> env = getEnv(ctx, cfg, backend);

        GitSshWrapper gitSshWrapper = GitSshWrapper.createFrom(secretService, ctx, instanceId, workDir, cfg, debug);
        Map<String, String> baseEnv = gitSshWrapper.updateEnv(workDir, new HashMap<>());

        Terraform terraform = new Terraform(workDir, debug, baseEnv, dependencyManager.resolve(new URI(resolveToolUrl(cfg))));
        if (debug) {
            terraform.exec(workDir, "version", "version");
        }

        log.info("Starting {}...", action);

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
                    CommonResult result = a.exec(terraform, backend);
                    ctx.setVariable(Constants.RESULT_KEY, objectMapper.convertValue(result, Map.class));
                    break;
                }
                case DESTROY: {
                    DestroyAction a = new DestroyAction(ctx, cfg, env);
                    CommonResult result = a.exec(terraform, backend);
                    ctx.setVariable(Constants.RESULT_KEY, objectMapper.convertValue(result, Map.class));
                    break;
                }
                case OUTPUT: {
                    OutputAction a = new OutputAction(ctx, cfg, env, false);
                    CommonResult result = a.exec(terraform, backend);
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

    private Map<String, Object> createCfg(Context ctx) {
        Map<String, Object> m = new HashMap<>(defaults != null ? defaults : Collections.emptyMap());

        put(m, com.walmartlabs.concord.sdk.Constants.Context.WORK_DIR_KEY, ctx);
        for (String k : Constants.ALL_IN_PARAMS) {
            put(m, k, ctx);
        }

        return m;
    }

    private Map<String, String> getEnv(Context ctx, Map<String, Object> cfg, Backend backend) throws Exception {
        Map<String, String> m = new HashMap<>();

        // default env
        m.putAll(MapUtils.getMap(cfg, Constants.DEFAULT_ENV_KEY, Collections.emptyMap()));

        // backend-specific env
        m.putAll(backend.prepareEnv(ctx, cfg));

        // user env
        Map<String, String> extraEnv = MapUtils.getMap(cfg, Constants.EXTRA_ENV_KEY, Collections.emptyMap());
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
        PLAN,
        OUTPUT,
        DESTROY;
    }

    // During the init we will download the version of Terraform specified by the user if defined, otherwise
    // we will download the default version. Terraform URLs look like the following:
    //
    // https://releases.hashicorp.com/terraform/0.12.5/terraform_0.12.5_linux_amd64.zip
    // https://releases.hashicorp.com/terraform/0.11.2/terraform_0.11.2_linux_amd64.zip
    //
    // So we can generalize to:
    //
    // https://releases.hashicorp.com/terraform/%s/terraform_%s_linux_amd64.zip
    //
    // We will also allow the user to specify the full URL if they want to download the tool zip from
    // and internal repository manager or other internally managed host.
    //
    private String resolveToolUrl(Map<String, Object> cfg) {
        String toolUrl = MapUtils.getString(cfg, Constants.TOOL_URL_KEY);
        if (toolUrl != null && !toolUrl.isEmpty()) {
            //
            // The user has explicitly specified a URL from where to download the tool.
            //
            return toolUrl;
        }

        String tfOs;
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("mac")) {
            tfOs = "darwin";
        } else if (os.contains("nux")) {
            tfOs = "linux";
        } else if (os.contains("win")) {
            tfOs = "windows";
        } else if (os.contains("sunos")) {
            tfOs = "solaris";
        } else {
            throw new IllegalArgumentException("Your operating system is not supported: " + os);
        }

        //
        // Check to see if the user has specified a version of the tool to use, if not use the default version.
        //
        String toolVersion = MapUtils.getString(cfg, Constants.TOOL_VERSION_KEY, DEFAULT_TERRAFORM_VERSION);

        return String.format(DEFAULT_TOOL_URL_TEMPLATE, toolVersion, toolVersion, tfOs);
    }
}
