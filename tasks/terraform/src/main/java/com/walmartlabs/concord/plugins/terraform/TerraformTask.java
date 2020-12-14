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
import com.walmartlabs.concord.plugins.terraform.actions.TerraformActionResult;
import com.walmartlabs.concord.plugins.terraform.backend.Backend;
import com.walmartlabs.concord.plugins.terraform.backend.BackendFactoryV1;
import com.walmartlabs.concord.sdk.*;

import javax.inject.Inject;
import javax.inject.Named;
import java.net.URI;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.walmartlabs.concord.plugins.terraform.TerraformTaskCommon.*;

@Named("terraform")
public class TerraformTask implements Task {

    private final SecretService secretService;
    private final LockService lockService;
    private final ObjectStorage objectStorage;
    private final ObjectMapper objectMapper;
    private final DependencyManager dependencyManager;

    @InjectVariable("terraformParams")
    private Map<String, Object> defaults;

    @Inject
    public TerraformTask(SecretService secretService, LockService lockService, ObjectStorage objectStorage, DependencyManager dependencyManager) {
        this.secretService = secretService;
        this.lockService = lockService;
        this.objectStorage = objectStorage;
        this.dependencyManager = dependencyManager;
        this.objectMapper = new ObjectMapper();
    }

    public void execute(Context ctx) throws Exception {
        String instanceId = (String) ctx.getVariable(com.walmartlabs.concord.sdk.Constants.Context.TX_ID_KEY);

        Map<String, Object> cfg = createCfg(ctx);

        Path workDir = ContextUtils.getWorkDir(ctx);

        boolean debug = MapUtils.get(cfg, TaskConstants.DEBUG_KEY, false, Boolean.class);
        Action action = getAction(cfg);

        // configure the state backend and populate the environment with necessary parameters
        Backend backend = new BackendFactoryV1(ctx, lockService, objectStorage).getBackend(cfg);
        Map<String, String> env = getEnv(cfg, backend);

        // configure the environment to support Terraform's git modules using Concord Secrets for authentication
        GitSshWrapper.SecretProvider secretProvider = (orgName, secretName, password) -> {
            Map<String, String> result = secretService.exportKeyAsFile(ctx, instanceId, workDir.toAbsolutePath().toString(), orgName, secretName, password);
            return workDir.resolve(result.get("private"));
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

        try {
            TerraformActionResult result = TerraformTaskCommon.execute(terraform, action, backend, workDir, cfg, env);
            ctx.setVariable(TaskConstants.RESULT_KEY, objectMapper.convertValue(result, Map.class));
        } finally {
            gitSshWrapper.cleanup();
        }
    }

    private Map<String, Object> createCfg(Context ctx) {
        Map<String, Object> m = new HashMap<>(defaults != null ? defaults : Collections.emptyMap());

        // default value for `pwd`
        String workDir = ContextUtils.assertString(ctx, com.walmartlabs.concord.sdk.Constants.Context.WORK_DIR_KEY);
        m.put(TaskConstants.PWD_KEY, workDir);

        for (String k : TaskConstants.ALL_IN_PARAMS) {
            put(m, k, ctx);
        }

        return m;
    }
}
