package com.walmartlabs.concord.plugins.akeyless.v2;

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
import com.walmartlabs.concord.runtime.v2.sdk.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Named("akeyless")
public class AkeylessTask implements Task {
    private static final Logger log = LoggerFactory.getLogger(AkeylessTask.class);

    private final Map<String, Object> defaults;
    private final Map<String, Object> policyDefaults;
    private final SecretExporter secretExporter;
    private final AkeylessCommon delegate;

    @Inject
    public AkeylessTask(Context ctx, SecretService secretService) {
        this.secretExporter = secretService::exportAsString;
        this.defaults = ctx.variables().getMap(TaskParams.DEFAULT_PARAMS_KEY, Collections.emptyMap());
        this.defaults.put("sessionToken", ctx.processConfiguration().processInfo().sessionToken());
        this.defaults.put("txId", ctx.processInstanceId().toString());
        this.policyDefaults = ctx.defaultVariables().toMap();
        this.delegate = new AkeylessCommon();
    }

    @Override
    public TaskResult.SimpleResult execute(Variables input) throws Exception {
        final TaskParams params = createParams(input);

        AkeylessTaskResult result = delegate.execute(params);

        return TaskResult.success()
                .values(Collections.singletonMap("data", result.getData()));
    }

    /**
     * @param path Secret path to read
     * @return only the secret value
     */
    public String getSecret(String path) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("action", TaskParams.Action.GETSECRET.toString());
        vars.put("path", path);
        TaskParams params = createParams(new MapBackedVariables(vars));

        return delegate.execute(params).getData().get(path);
    }

    private TaskParams createParams(Variables input) {
        return TaskParamsImpl.of(input.toMap(), defaults, policyDefaults, secretExporter);
    }
}
