package com.walmartlabs.concord.plugins.hashivault;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2025 Walmart Inc., Concord Authors
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

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.walmartlabs.concord.runtime.v2.sdk.MapBackedVariables;
import com.walmartlabs.concord.runtime.v2.sdk.Variables;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractHashiVaultTask {

    private Injector injector;

    protected abstract TaskParams createParams(Variables input, TaskParams.SecretExporter secretExporter);

    protected HashiVaultTaskResult executeCommon(Variables input, TaskParams.SecretExporter secretExporter) {
        var params = createParams(input, secretExporter);
        return getDelegate().execute(params);
    }

    protected HashiVaultTaskCommon getDelegate() {
        if (injector == null) {
            injector = Guice.createInjector(new HashiDepsModule());
        }

        return injector.getInstance(HashiVaultTaskCommon.class);
    }

    public Map<String, Object> readKV(String path, TaskParams.SecretExporter secretExporter) {
        var input = getVariables(TaskParams.Action.READKV, path, Map.of());
        var params = createParams(input, secretExporter);
        return getDelegate().execute(params).data();
    }

    public String readKV(String path, String field, TaskParams.SecretExporter secretExporter) {
        var input = getVariables(TaskParams.Action.READKV, path, Map.of(TaskParams.KEY_KEY, field));
        var params = createParams(input, secretExporter);
        return getDelegate().execute(params).data();
    }

    public void writeKV(String path, Map<String, Object> kvPairs, TaskParams.SecretExporter secretExporter) {
        var input = getVariables(TaskParams.Action.WRITEKV, path, Map.of(TaskParams.KV_PAIRS_KEY, kvPairs));
        var params = createParams(input, secretExporter);
        getDelegate().execute(params);
    }

    private Variables getVariables(TaskParams.Action action, String path, Map<String, Object> extra) {
        var input = new HashMap<String, Object>();
        input.put(TaskParams.ACTION_KEY, action.toString());
        input.put(TaskParams.PATH_KEY, path);
        input.putAll(extra);

        return new MapBackedVariables(input);
    }

}
