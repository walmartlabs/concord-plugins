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

import com.walmartlabs.concord.plugins.hashivault.AbstractHashiVaultTask;
import com.walmartlabs.concord.plugins.hashivault.HashiVaultTaskResult;
import com.walmartlabs.concord.plugins.hashivault.TaskParams;
import com.walmartlabs.concord.runtime.v2.sdk.MapBackedVariables;
import com.walmartlabs.concord.runtime.v2.sdk.Variables;
import com.walmartlabs.concord.sdk.*;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Map;

@Named("hashivault")
public class HashiVaultTask extends AbstractHashiVaultTask implements Task {

    @InjectVariable(TaskParams.DEFAULT_PARAMS_KEY)
    private Map<String, Object> defaults;

    private final SecretService secretService;

    @Inject
    public HashiVaultTask(SecretService secretService) {
        super();
        this.secretService = secretService;
    }

    @Override
    public void execute(Context ctx) {
        final TaskParams.SecretExporter secretExporter = new SecretExporterV1(ctx, secretService);
        final Variables input = new MapBackedVariables(ctx.toMap());
        final HashiVaultTaskResult result = executeCommon(input, secretExporter);

        ctx.setVariable("result", result.toMap());
    }

    @Override
    public TaskParams createParams(Variables input, TaskParams.SecretExporter secretExporter) {
        return TaskParams.of(input, defaults, secretExporter);
    }

    public Map<String, Object> readKV(@InjectVariable("context") Context ctx, String path) {
        return super.readKV(path, new SecretExporterV1(ctx, secretService));
    }

    public String readKV(@InjectVariable("context") Context ctx, String path, String key) {
        return super.readKV(path, key, new SecretExporterV1(ctx, secretService));
    }

    public void writeKV(@InjectVariable("context") Context ctx, String path, Map<String, Object> kvPairs) {
        super.writeKV(path, kvPairs, new SecretExporterV1(ctx, secretService));
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
