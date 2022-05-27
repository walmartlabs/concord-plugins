package com.walmartlabs.concord.plugins.akeyless.v1;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2022 Walmart Inc., Concord Authors
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

import com.walmartlabs.concord.plugins.akeyless.AkeylessCommon;
import com.walmartlabs.concord.plugins.akeyless.AkeylessTaskResult;
import com.walmartlabs.concord.plugins.akeyless.SecretExporter;
import com.walmartlabs.concord.plugins.akeyless.TaskParams;
import com.walmartlabs.concord.sdk.Context;
import com.walmartlabs.concord.sdk.InjectVariable;
import com.walmartlabs.concord.sdk.SecretService;
import com.walmartlabs.concord.sdk.Task;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.HashMap;
import java.util.Map;

@Named("akeyless")
public class AkeylessTask implements Task {

    @InjectVariable(TaskParams.DEFAULT_PARAMS_KEY)
    private Map<String, Object> defaults;
    private final SecretExporter secretExporter;
    private final AkeylessCommon delegate;

    @Inject
    public AkeylessTask(Context ctx, SecretService secretService) {
        this.secretExporter = (o, n, p) -> secretService.exportAsString(ctx, o, n, p);
        this.delegate = new AkeylessCommon();
    }

    @Override
    public void execute(Context ctx) {
        final TaskParams params = createParams(ctx.toMap());

        AkeylessTaskResult result = delegate.execute(params);

        ctx.setVariable("result", result.getData());
    }

    /**
     * @param path Secret path to read
     * @return only the secret value
     */
    public String getSecret(String path) {
        Map<String, Object> vars = new HashMap<>();
        vars.put(TaskParams.ACTION_KEY, TaskParams.Action.GETSECRET.toString());
        vars.put(TaskParams.SECRET_PATH_KEY, path);
        TaskParams params = createParams(vars);

        return delegate.execute(params).getData().get(path);
    }

    private TaskParams createParams(Map<String, Object> input) {

        return TaskParams.of(input, defaults, null, secretExporter);
    }
}
