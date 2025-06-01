package com.walmartlabs.concord.plugins.hashivault.v1;

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
import com.walmartlabs.concord.runtime.v2.sdk.MapBackedVariables;
import com.walmartlabs.concord.sdk.*;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.HashMap;
import java.util.Map;

@Named("hashivault")
public class HashiVaultTask implements Task {

    @InjectVariable(TaskParams.DEFAULT_PARAMS_KEY)
    private Map<String, Object> defaults;

    private final SecretService secretService;

    @Inject
    public HashiVaultTask(SecretService secretService) {
        this.secretService = secretService;
    }

    @Override
    public void execute(Context ctx) {
        final TaskParams params = createParams(ctx, ctx.toMap());
        final HashiVaultTaskCommon delegate = new HashiVaultTaskCommon();
        final HashiVaultTaskResult result = delegate.execute(params);

        ctx.setVariable("result", result.toMap());
    }

    private TaskParams createParams(Context ctx, Map<String, Object> input) {
        final MapBackedVariables vars = new MapBackedVariables(input);
        final SecretExporterV1 exporterV1 = new SecretExporterV1(ctx, secretService);
        return TaskParams.of(vars, defaults, exporterV1);
    }

    public Map<String, Object> readKV(@InjectVariable("context") Context ctx, String path) {
        final Map<String, Object> input = new HashMap<>(2);
        input.put(TaskParams.ACTION_KEY, TaskParams.Action.READKV.toString());
        input.put(TaskParams.PATH_KEY, path);

        final TaskParams params = createParams(ctx, input);
        final HashiVaultTaskCommon delegate = new HashiVaultTaskCommon();
        HashiVaultTaskResult result = delegate.execute(params);

        return result.data();
    }

    public String readKV(@InjectVariable("context") Context ctx, String path, String key) {
        final Map<String, Object> input = new HashMap<>(2);
        input.put(TaskParams.ACTION_KEY, TaskParams.Action.READKV.toString());
        input.put(TaskParams.PATH_KEY, path);
        input.put(TaskParams.KEY_KEY, key);

        final TaskParams params = createParams(ctx, input);
        final HashiVaultTaskCommon delegate = new HashiVaultTaskCommon();
        final HashiVaultTaskResult result = delegate.execute(params);

        return result.data();
    }

    public void writeKV(@InjectVariable("context") Context ctx, String path, Map<String, Object> kvPairs) {
        final Map<String, Object> input = new HashMap<>(2);
        input.put(TaskParams.ACTION_KEY, TaskParams.Action.WRITEKV.toString());
        input.put(TaskParams.PATH_KEY, path);
        input.put(TaskParams.KV_PAIRS_KEY, kvPairs);

        final TaskParams params = createParams(ctx, input);
        final HashiVaultTaskCommon delegate = new HashiVaultTaskCommon();
        delegate.execute(params);
    }

    private static class SecretExporterV1 implements TaskParams.SecretExporter {
        private final SecretService secretService;
        private final Context ctx;
        private final String txId;

        SecretExporterV1(Context ctx, SecretService secretService) {
            this.secretService = secretService;
            this.ctx = ctx;
            this.txId = ContextUtils.assertString(ctx, TaskParams.TX_ID_KEY);
        }

        @Override
        public String exportAsString(String o, String n, String p) throws Exception {
            return secretService.exportAsString(ctx, txId, o, n, p);
        }
    }

    protected void setDefaults(Map<String, Object> d) {
        defaults = d;
    }
}
