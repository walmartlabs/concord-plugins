package com.walmartlabs.concord.plugins.jira.v2;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2020 Walmart Inc., Concord Authors
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

import com.walmartlabs.concord.plugins.jira.JiraCredentials;
import com.walmartlabs.concord.plugins.jira.JiraSecretService;
import com.walmartlabs.concord.plugins.jira.JiraTaskCommon;
import com.walmartlabs.concord.plugins.jira.TaskParams;
import com.walmartlabs.concord.runtime.v2.sdk.*;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.HashMap;
import java.util.Map;

import static com.walmartlabs.concord.plugins.jira.Constants.PARAMS_KEY;

@Named("jira")
@DryRunReady
public class JiraTaskV2 implements Task {

    private final Context context;
    private final JiraTaskCommon delegate;
    private final Map<String, Object> globalDefaults;

    @Inject
    public JiraTaskV2(Context context) {
        this.context = context;
        this.globalDefaults = context.variables().getMap(PARAMS_KEY, Map.of());
        this.delegate = new JiraTaskCommon(new V2SecretService(context.secretService()), context.processConfiguration().dryRun());
    }

    @Override
    public TaskResult execute(Variables input) {
        var policyDefaults = new HashMap<>(context.defaultVariables().toMap());
        policyDefaults.put("txId", context.processInstanceId());

        var result = getDelegate().execute(TaskParams.of(input.toMap(), globalDefaults, policyDefaults));

        return TaskResult.success()
                .values(result);
    }

    JiraTaskCommon getDelegate() {
        return delegate;
    }

    static class V2SecretService implements JiraSecretService {
        private final SecretService secretService;

        public V2SecretService(SecretService secretService) {
            this.secretService = secretService;
        }

        @Override
        public JiraCredentials exportCredentials(String orgName, String secretName, String password) throws Exception {
            var creds = secretService.exportCredentials(orgName, secretName, password);
            return new JiraCredentials(creds.username(), creds.password());
        }
    }

}
