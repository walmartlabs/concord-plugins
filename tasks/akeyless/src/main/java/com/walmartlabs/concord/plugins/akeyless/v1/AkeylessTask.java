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
import com.walmartlabs.concord.plugins.akeyless.model.TaskParams;
import com.walmartlabs.concord.plugins.akeyless.model.TaskParamsImpl;
import com.walmartlabs.concord.sdk.*;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.HashMap;
import java.util.Map;

@Named("akeyless")
public class AkeylessTask implements Task {

    @InjectVariable(TaskParams.DEFAULT_PARAMS_KEY)
    private Map<String, Object> defaults;
    private final SecretService secretService;
    private final AkeylessCommon delegate;

    @Inject
    public AkeylessTask(SecretService secretService) {
        this.secretService = secretService;
        this.delegate = new AkeylessCommon();
    }

    @Override
    public void execute(Context ctx) {
        final TaskParams params = createParams(ctx, ctx.toMap());
        final AkeylessTaskResult result = delegate.execute(params);

        ctx.setVariable("result", result.getData());
    }

    /**
     * @param path Secret path to read
     * @return only the secret value
     */
    public String getSecret(@InjectVariable("context") Context ctx, String path) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("action", TaskParams.Action.GETSECRET.toString());
        vars.put("path", path);
        TaskParams params = createParams(ctx, vars);

        return delegate.execute(params).getData().get(path);
    }

    private TaskParams createParams(Context ctx, Map<String, Object> input) {
        input.put("txId", ContextUtils.getTxId(ctx).toString());
        input.put("sessionToken", ContextUtils.getSessionToken(ctx));

        SecretExporter secretExporter = new SecretExporterV1(
                ctx, ContextUtils.getTxId(ctx), ContextUtils.getWorkDir(ctx), secretService);

        return TaskParamsImpl.of(input, defaults, null, secretExporter);
    }
}
