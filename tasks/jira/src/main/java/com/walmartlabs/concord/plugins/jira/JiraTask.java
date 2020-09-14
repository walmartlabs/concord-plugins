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
import com.walmartlabs.concord.sdk.*;

import javax.inject.Inject;
import javax.inject.Named;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

import static com.walmartlabs.concord.plugins.jira.JiraTaskCommon.JIRA_ISSUE_STATUS_KEY;

@Named("jira")
@SuppressWarnings("unused")
public class JiraTask implements Task {

    private final SecretService secretService;

    @InjectVariable("jiraParams")
    private Map<String, Object> defaults;

    @Inject
    public JiraTask(SecretService secretService) {
        this.secretService = secretService;
    }

    @Override
    public void execute(Context ctx) {
        Map<String, Object> result = delegate(ctx).execute(TaskParams.of(new ContextVariables(ctx), defaults));

        result.forEach(ctx::setVariable);
    }

    public String getStatus(@InjectVariable("context") Context ctx, String issueKey) {
        Variables vars = TaskParams.merge(new ContextVariables(ctx), defaults);

        Map<String, Object> result = delegate(ctx).execute(new TaskParams.CurrentStatusParams(vars) {
            @Override
            public String issueKey() {
                return issueKey;
            }
        });

        return MapUtils.getString(result, JIRA_ISSUE_STATUS_KEY);
    }

    private JiraTaskCommon delegate(Context ctx) {
        return new JiraTaskCommon((orgName, secretName, password) -> {
            UUID txId = ContextUtils.getTxId(ctx);
            Path workDir = ContextUtils.getWorkDir(ctx);

            Map<String, String> result1 = secretService.exportCredentials(ctx, txId.toString(), workDir.toString(), orgName, secretName, password);
            return new JiraCredentials(result1.get("username"), result1.get("password"));
        });
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
