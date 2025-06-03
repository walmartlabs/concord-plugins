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

import com.walmartlabs.concord.plugins.hashivault.AbstractHashiVaultTask;
import com.walmartlabs.concord.plugins.hashivault.HashiVaultTaskResult;
import com.walmartlabs.concord.plugins.hashivault.TaskParams;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.DryRunReady;
import com.walmartlabs.concord.runtime.v2.sdk.SecretService;
import com.walmartlabs.concord.runtime.v2.sdk.Task;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import com.walmartlabs.concord.runtime.v2.sdk.Variables;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Map;
import java.util.Optional;

@Named("hashivault")
@DryRunReady
public class HashiVaultTask extends AbstractHashiVaultTask implements Task {

    private final Map<String, Object> defaults;
    private final TaskParams.SecretExporter secretExporter;
    private final boolean dryRunMode;

    @Inject
    public HashiVaultTask(Context ctx, SecretService secretService) {
        this.secretExporter = secretService::exportAsString;
        this.defaults = ctx.variables().getMap(TaskParams.DEFAULT_PARAMS_KEY, Map.of());
        this.dryRunMode = ctx.processConfiguration().dryRun();
    }

    @Override
    public TaskResult.SimpleResult execute(Variables input) {
        final HashiVaultTaskResult result = executeCommon(input, secretExporter);
        final Object data = Optional.ofNullable(result.data()).orElseGet(Map::of);

        return result.ok()
                ? TaskResult.success().values(Map.of("data", data))
                : TaskResult.fail(result.error());
    }

    @Override
    public TaskParams createParams(Variables input, TaskParams.SecretExporter secretExporter) {
        return TaskParams.of(input, defaults, secretExporter, dryRunMode);
    }

    public Map<String, Object> readKV(String path) {
        return super.readKV(path, secretExporter);
    }

    public String readKV(String path, String field) {
        return super.readKV(path, field, secretExporter);
    }

    public void writeKV(String path, Map<String, Object> kvPairs) {
        super.writeKV(path, kvPairs, secretExporter);
    }
}
