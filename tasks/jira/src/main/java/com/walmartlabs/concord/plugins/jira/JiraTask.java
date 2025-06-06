package com.walmartlabs.concord.plugins.jira;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2018 Walmart Inc.
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

import com.walmartlabs.concord.runtime.v2.sdk.Variables;
import com.walmartlabs.concord.sdk.Context;
import com.walmartlabs.concord.sdk.ContextUtils;
import com.walmartlabs.concord.sdk.InjectVariable;
import com.walmartlabs.concord.sdk.MapUtils;
import com.walmartlabs.concord.sdk.SecretService;
import com.walmartlabs.concord.sdk.Task;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Map;

import static com.walmartlabs.concord.plugins.jira.Constants.PARAMS_KEY;
import static com.walmartlabs.concord.plugins.jira.JiraTaskCommon.JIRA_ISSUE_STATUS_KEY;
import static com.walmartlabs.concord.sdk.Constants.Context.CONTEXT_KEY;

@Named("jira")
@SuppressWarnings("unused")
public class JiraTask implements Task {

    private final SecretService secretService;

    @InjectVariable(PARAMS_KEY)
    private Map<String, Object> defaults;

    @Inject
    public JiraTask(SecretService secretService) {
        this.secretService = secretService;
    }

    @Override
    public void execute(Context ctx) {
        var jiraSecretService = getSecretService(ctx);

        delegate(jiraSecretService)
                .execute(TaskParams.of(new ContextVariables(ctx), defaults))
                .forEach(ctx::setVariable);
    }

    public String getStatus(@InjectVariable(CONTEXT_KEY) Context ctx, String issueKey) {
        var vars = TaskParams.merge(new ContextVariables(ctx), defaults);
        var jiraSecretService = getSecretService(ctx);
        var result = delegate(jiraSecretService)
                .execute(new TaskParams.CurrentStatusParams(vars) {
                    @Override
                    public String issueKey() {
                        return issueKey;
                    }
                });

        return MapUtils.getString(result, JIRA_ISSUE_STATUS_KEY);
    }

    JiraTaskCommon delegate(JiraSecretService jiraSecretService) {
        return new JiraTaskCommon(jiraSecretService);
    }

    private JiraSecretService getSecretService(Context ctx) {
        return new V1SecretService(secretService, ctx);
    }

    static class V1SecretService implements JiraSecretService {
        private final SecretService secretService;

        public V1SecretService(SecretService secretService, Context ctx) {
            this.secretService = secretService;
            this.ctx = ctx;
        }

        private final Context ctx;

        @Override
        public JiraCredentials exportCredentials(String orgName, String secretName, String password) throws Exception {
            var txId = ContextUtils.getTxId(ctx).toString();
            var workDir = ContextUtils.getWorkDir(ctx).toString();

            var creds = secretService.exportCredentials(ctx, txId, workDir, orgName, secretName, password);
            return new JiraCredentials(creds.get("username"), creds.get("password"));
        }
    }

    private static class ContextVariables implements Variables {

        private final Context context;

        public ContextVariables(Context context) {
            this.context = context;
        }

        @Override
        public Object get(String key) {
            return context.getVariable(key);
        }

        @Override
        public void set(String key, Object value) {
            throw new IllegalStateException("Unsupported");
        }

        @Override
        public boolean has(String key) {
            return context.getVariable(key) != null;
        }

        @Override
        public Map<String, Object> toMap() {
            return context.toMap();
        }
    }
}
