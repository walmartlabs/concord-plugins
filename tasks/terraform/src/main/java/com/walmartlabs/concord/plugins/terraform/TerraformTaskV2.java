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

import com.walmartlabs.concord.ApiClient;
import com.walmartlabs.concord.plugins.terraform.actions.TerraformActionResult;
import com.walmartlabs.concord.plugins.terraform.backend.Backend;
import com.walmartlabs.concord.plugins.terraform.backend.BackendFactoryV2;
import com.walmartlabs.concord.runtime.v2.sdk.*;
import com.walmartlabs.concord.sdk.Constants;
import com.walmartlabs.concord.sdk.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.net.URI;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.walmartlabs.concord.plugins.terraform.TerraformTaskCommon.getAction;
import static com.walmartlabs.concord.plugins.terraform.TerraformTaskCommon.getEnv;

@Named("terraform")
public class TerraformTaskV2 implements Task {

    private static final Logger log = LoggerFactory.getLogger(TerraformTaskV2.class);

    private final Context ctx;
    private final ApiClient apiClient;
    private final SecretService secretService;
    private final LockService lockService;
    private final DependencyManager dependencyManager;

    @Inject
    public TerraformTaskV2(Context ctx, ApiClient apiClient, SecretService secretService, LockService lockService, DependencyManager dependencyManager) {
        this.ctx = ctx;
        this.apiClient = apiClient;
        this.secretService = secretService;
        this.lockService = lockService;
        this.dependencyManager = dependencyManager;
    }

    @Override
    public TaskResult execute(Variables input) throws Exception {
        Path workDir = ctx.workingDirectory();
        Map<String, Object> cfg = createCfg(workDir, input, ctx.defaultVariables());
        boolean debug = MapUtils.get(cfg, TaskConstants.DEBUG_KEY, false, Boolean.class);
        Action action = getAction(cfg);

        // configure the state backend and populate the environment with necessary parameters
        Backend backend = new BackendFactoryV2(ctx, apiClient, lockService).getBackend(cfg);
        Map<String, String> env = getEnv(cfg, backend);

        // configure the environment to support Terraform's git modules using Concord Secrets for authentication
        GitSshWrapper.SecretProvider secretProvider = (orgName, secretName, password) -> {
            SecretService.KeyPair result = secretService.exportKeyAsFile(orgName, secretName, password);
            return result.privateKey();
        };
        GitSshWrapper gitSshWrapper = GitSshWrapper.createFrom(secretProvider, workDir, cfg, debug);
        Map<String, String> baseEnv = gitSshWrapper.updateEnv(workDir, new HashMap<>());

        // configure the Terraform's binary
        TerraformBinaryResolver binaryResolver = new TerraformBinaryResolver(cfg, workDir, debug,
                url -> dependencyManager.resolve(URI.create(url)));

        Terraform terraform = new Terraform(binaryResolver, debug, baseEnv);
        if (debug) {
            terraform.exec(workDir, "version", "version");
        }

        log.info("Starting {}...", action);

        try {
            TerraformActionResult result = TerraformTaskCommon.execute(terraform, action, backend, cfg, env);
            return convertResult(result);
        } finally {
            gitSshWrapper.cleanup();
        }
    }

    private Map<String, Object> createCfg(Path workDir, Variables input, Variables defaults) {
        Map<String, Object> m = new HashMap<>(defaults != null ? defaults.toMap() : Collections.emptyMap());

        m.put(Constants.Context.WORK_DIR_KEY, workDir.toAbsolutePath().toString());

        for (String k : TaskConstants.ALL_IN_PARAMS) {
            Object v = input.get(k);
            if (v != null) {
                m.put(k, v);
            }
        }

        return m;
    }

    private TaskResult convertResult(TerraformActionResult src) {
        TaskResult dst = new TaskResult(src.isOk(), src.getError());

        if (src.getData() != null) {
            dst.value("data", src.getData());
        }

        if (src.getOutput() != null) {
            dst.value("output", src.getOutput());
        }

        if (src.getHasChanges() != null) {
            dst.value("hasChanges", src.getOutput());
        }

        if (src.getPlanPath() != null) {
            dst.value("planPath", src.getPlanPath());
        }

        return dst;
    }
}
