package com.walmartlabs.concord.plugins.hashivault.v2;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2021 Walmart Inc., Concord Authors
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

import com.walmartlabs.concord.plugins.hashivault.HashiVaultTaskCommon;
import com.walmartlabs.concord.plugins.hashivault.HashiVaultTaskResult;
import com.walmartlabs.concord.plugins.hashivault.TaskParams;
import com.walmartlabs.concord.runtime.v2.sdk.*;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


@Named("hashivault")
@DryRunReady
public class HashiVaultTask implements Task {

    private final Map<String, Object> defaults;
    private final SecretService secretService;
    private final boolean dryRunMode;

    @Inject
    public HashiVaultTask(Context ctx, SecretService secretService) {
        this.secretService = secretService;
        this.defaults = ctx.variables().getMap(TaskParams.DEFAULT_PARAMS_KEY, Collections.emptyMap());
        this.dryRunMode = ctx.processConfiguration().dryRun();
    }

    @Override
    public TaskResult.SimpleResult execute(Variables input) {
        final TaskParams params = createParams(input);
        final HashiVaultTaskCommon delegate = new HashiVaultTaskCommon(dryRunMode);
        final HashiVaultTaskResult result = delegate.execute(params);
        final Map<String, Object> data = new HashMap<>(1);
        data.put("data", result.data());

        return TaskResult.of(result.ok(), result.error(), data);
    }

    private TaskParams createParams(Variables input) {
        final SecretExporterV2 exporterV2 = new SecretExporterV2(secretService);
        return TaskParams.of(input, defaults, exporterV2);
    }

    public Map<String, Object> readKV(String path) {
        Map<String, Object> vars = new HashMap<>();
        vars.put(TaskParams.ACTION_KEY, TaskParams.Action.READKV.toString());
        vars.put(TaskParams.PATH_KEY, path);

        final Variables input = new MapBackedVariables(vars);
        final TaskParams params = createParams(input);
        final HashiVaultTaskResult result = new HashiVaultTaskCommon().execute(params);
        return result.data();
    }

    public String readKV(String path, String field) {
        Map<String, Object> vars = new HashMap<>();
        vars.put(TaskParams.ACTION_KEY, TaskParams.Action.READKV.toString());
        vars.put(TaskParams.PATH_KEY, path);
        vars.put(TaskParams.KEY_KEY, field);

        final Variables input = new MapBackedVariables(vars);
        final TaskParams params = createParams(input);
        final HashiVaultTaskResult result = new HashiVaultTaskCommon().execute(params);
        return result.data();
    }

    public void writeKV(String path, Map<String, Object> kvPairs) {
        Map<String, Object> vars = new HashMap<>();
        vars.put(TaskParams.ACTION_KEY, TaskParams.Action.WRITEKV.toString());
        vars.put(TaskParams.PATH_KEY, path);
        vars.put(TaskParams.KV_PAIRS_KEY, kvPairs);

        final Variables input = new MapBackedVariables(vars);
        final TaskParams params = createParams(input);
        final HashiVaultTaskCommon delegate = new HashiVaultTaskCommon();
        delegate.execute(params);
    }

    private static class SecretExporterV2 implements TaskParams.SecretExporter {
        private final SecretService secretService;

        SecretExporterV2(SecretService secretService) {
            this.secretService = secretService;
        }

        @Override
        public String exportAsString(String o, String n, String p) throws Exception {
            return secretService.exportAsString(o, n, p);
        }
    }
}
