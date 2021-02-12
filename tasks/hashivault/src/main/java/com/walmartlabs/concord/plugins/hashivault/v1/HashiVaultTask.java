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
import com.walmartlabs.concord.sdk.Context;
import com.walmartlabs.concord.sdk.InjectVariable;
import com.walmartlabs.concord.sdk.Task;

import javax.inject.Named;
import java.util.HashMap;
import java.util.Map;

@Named("hashivault")
public class HashiVaultTask implements Task {

    @InjectVariable(TaskParams.DEFAULT_PARAMS_KEY)
    private Map<String, Object> defaults;

    @Override
    public void execute(Context ctx) {
        final TaskParams params = TaskParams.of(new MapBackedVariables(ctx.toMap()), defaults);
        final HashiVaultTaskCommon delegate = new HashiVaultTaskCommon();
        final HashiVaultTaskResult result = delegate.execute(params);

        ctx.setVariable("result", result.toMap());
    }

    public Map<String, Object> readKV(String path) {
        final Map<String, Object> input = new HashMap<>(2);
        input.put(TaskParams.ACTION_KEY, TaskParams.Action.READKV.toString());
        input.put(TaskParams.PATH_KEY, path);

        final TaskParams params = TaskParams.of(new MapBackedVariables(input), defaults);
        final HashiVaultTaskCommon delegate = new HashiVaultTaskCommon();
        HashiVaultTaskResult result = delegate.execute(params);

        return result.data();
    }

    public String readKV(String path, String key) {
        final Map<String, Object> input = new HashMap<>(2);
        input.put(TaskParams.ACTION_KEY, TaskParams.Action.READKV.toString());
        input.put(TaskParams.PATH_KEY, path);
        input.put(TaskParams.KEY_KEY, key);

        final TaskParams params = TaskParams.of(new MapBackedVariables(input), defaults);
        final HashiVaultTaskCommon delegate = new HashiVaultTaskCommon();
        HashiVaultTaskResult result = delegate.execute(params);

        return result.data();
    }

    public void writeKV(String path, Map<String, Object> kvPairs) {
        Map<String, Object> input = new HashMap<>(2);
        input.put(TaskParams.ACTION_KEY, TaskParams.Action.WRITEKV.toString());
        input.put(TaskParams.PATH_KEY, path);
        input.put(TaskParams.KV_PAIRS_KEY, kvPairs);

        final TaskParams params = TaskParams.of(new MapBackedVariables(input), defaults);
        final HashiVaultTaskCommon delegate = new HashiVaultTaskCommon();
        delegate.execute(params);
    }
}
