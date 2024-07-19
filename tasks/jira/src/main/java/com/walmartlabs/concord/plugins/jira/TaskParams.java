package com.walmartlabs.concord.plugins.jira;

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

import com.walmartlabs.concord.runtime.v2.sdk.MapBackedVariables;
import com.walmartlabs.concord.runtime.v2.sdk.Variables;

import javax.annotation.Nonnull;
import java.util.*;

public class TaskParams implements JiraClientCfg {

    public static TaskParams of(Variables input, Map<String, Object> defaults) {
        Variables variables = merge(input, defaults);

        Action action = new TaskParams(variables).action();
        switch (action) {
            case ADDCOMMENT: {
                return new AddCommentParams(variables);
            }
            case CREATECOMPONENT: {
                return new CreateComponentParams(variables);
            }
            case CREATEISSUE: {
                return new CreateIssueParams(variables);
            }
            case DELETECOMPONENT: {
                return new DeleteComponentParams(variables);
            }
            case DELETEISSUE: {
                return new DeleteIssueParams(variables);
            }
            case TRANSITION: {
                return new TransitionParams(variables);
            }
            case UPDATEISSUE: {
                return new UpdateIssueParams(variables);
            }
            case CREATESUBTASK: {
                return new CreateSubTaskParams(variables);
            }
            case CURRENTSTATUS: {
                return new CurrentStatusParams(variables);
            }
            case ADDATTACHMENT: {
                return new AddAttachmentParams(variables);
            }
            case GETISSUES: {
                return new GetIssuesParams(variables);
            }
            default:
                throw new IllegalArgumentException("Unsupported action type: " + action);
        }
    }

    private static final String ACTION_KEY = "action";
    private static final String JIRA_URL_KEY = "apiUrl";
    private static final String JIRA_AUTH_KEY = "auth";
    private static final String JIRA_USER_ID_KEY = "userId";
    private static final String JIRA_PASSWORD_KEY = "password";
    private static final String JIRA_HTTP_CLIENT_PROTOCOL_VERSION_KEY = "httpClientProtocolVersion";
    private static final String CLIENT_CONNECTTIMEOUT = "connectTimeout";
    private static final String CLIENT_READTIMEOUT = "readTimeout";
    private static final String CLIENT_WRITETIMEOUT = "writeTimeout";

    protected final Variables variables;

    public TaskParams(Variables variables) {
        this.variables = variables;
    }

    public Action action() {
        String action = variables.assertString(ACTION_KEY);
        try {
            return Action.valueOf(action.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown action: '" + action + "'. Available actions: " + Arrays.toString(Action.values()));
        }
    }

    public String jiraUrl() {
        return variables.assertString(JIRA_URL_KEY);
    }

    @Override
    public long connectTimeout() {
        return variables.getLong(CLIENT_CONNECTTIMEOUT, JiraClientCfg.super.connectTimeout());
    }

    @Override
    public long readTimeout() {
        return variables.getLong(CLIENT_READTIMEOUT, JiraClientCfg.super.readTimeout());
    }

    @Override
    public long writeTimeout() {
        return variables.getLong(CLIENT_WRITETIMEOUT, JiraClientCfg.super.writeTimeout());
    }

    public Map<String, Object> auth() {
        return variables.getMap(JIRA_AUTH_KEY, null);
    }

    public String uid() {
        return variables.assertString(JIRA_USER_ID_KEY);
    }

    public String pwd() {
        return variables.assertString(JIRA_PASSWORD_KEY);
    }

    @Override
    public HttpVersion httpProtocolVersion() {
        return Optional.ofNullable(variables.getString(JIRA_HTTP_CLIENT_PROTOCOL_VERSION_KEY))
                .map(HttpVersion::from)
                .orElse(JiraClientCfg.super.httpProtocolVersion());
    }

    public static class CreateIssueParams extends TaskParams {

        private static final String JIRA_ASSIGNEE_KEY = "assignee";
        private static final String JIRA_CUSTOM_FIELDS_ATTR_KEY = "customFieldsTypeFieldAttr";
        private static final String JIRA_CUSTOM_FIELDS_KV_KEY = "customFieldsTypeKv";
        private static final String JIRA_DESCRIPTION_KEY = "description";
        private static final String JIRA_ISSUE_COMPONENTS_KEY = "components";
        private static final String JIRA_ISSUE_LABELS_KEY = "labels";
        private static final String JIRA_ISSUE_PRIORITY_KEY = "priority";
        private static final String JIRA_ISSUE_TYPE_KEY = "issueType";
        private static final String JIRA_PROJECT_KEY = "projectKey";
        private static final String JIRA_REQUESTOR_UID_KEY = "requestorUid";
        private static final String JIRA_SUMMARY_KEY = "summary";

        public CreateIssueParams(Variables variables) {
            super(variables);
        }

        public String projectKey() {
            return variables.assertString(JIRA_PROJECT_KEY);
        }

        public String summary() {
            return variables.assertString(JIRA_SUMMARY_KEY);
        }
        public String description() {
            return variables.assertString(JIRA_DESCRIPTION_KEY);
        }

        public String requestorUid() {
            return variables.getString(JIRA_REQUESTOR_UID_KEY);
        }

        public String issueType() {
            return variables.assertString(JIRA_ISSUE_TYPE_KEY);
        }

        public String issuePriority() {
            return variables.getString(JIRA_ISSUE_PRIORITY_KEY, null);
        }

        public Map<String, Object> assignee() {
            return variables.getMap( JIRA_ASSIGNEE_KEY, null);
        }

        public List<String> labels() {
            return variables.getList(JIRA_ISSUE_LABELS_KEY, null);
        }

        public List<String> components() {
            return variables.getList(JIRA_ISSUE_COMPONENTS_KEY, null);
        }

        public @Nonnull Map<String, String> customFieldsTypeKv() {
            return variables.getMap(JIRA_CUSTOM_FIELDS_KV_KEY, Map.of());
        }

        public @Nonnull Map<String, Object> customFieldsTypeAtt() {
            return variables.getMap(JIRA_CUSTOM_FIELDS_ATTR_KEY, Map.of());
        }
    }

    public static class AddCommentParams extends TaskParams {

        private static final String JIRA_COMMENT_KEY = "comment";
        private static final String JIRA_ISSUE_KEY = "issueKey";
        private static final String DEBUG_KEY = "debug";

        public AddCommentParams(Variables variables) {
            super(variables);
        }

        public String issueKey() {
            return variables.assertString(JIRA_ISSUE_KEY);
        }

        public String comment() {
            return variables.assertString(JIRA_COMMENT_KEY);
        }

        public boolean debug() {
            return variables.getBoolean(DEBUG_KEY, false);
        }
    }

    public static class AddAttachmentParams extends TaskParams {

        private static final String JIRA_FILE_PATH_KEY = "filePath";
        private static final String JIRA_ISSUE_KEY = "issueKey";

        public AddAttachmentParams(Variables variables) {
            super(variables);
        }

        public String issueKey() {
            return variables.assertString(JIRA_ISSUE_KEY);
        }

        public String filePath() {
            return variables.assertString(JIRA_FILE_PATH_KEY);
        }
    }

    public static class CreateComponentParams extends TaskParams {

        private static final String JIRA_COMPONENTNAME = "componentName";
        private static final String JIRA_PROJECT_KEY = "projectKey";

        public CreateComponentParams(Variables variables) {
            super(variables);
        }

        public String projectKey() {
            return variables.assertString(JIRA_PROJECT_KEY);
        }

        public String componentName() {
            return variables.assertString(JIRA_COMPONENTNAME);
        }
    }

    public static class DeleteComponentParams extends TaskParams {

        private static final String JIRA_COMPONENTID = "componentId";

        public DeleteComponentParams(Variables variables) {
            super(variables);
        }

        public int componentId() {
            return variables.assertInt(JIRA_COMPONENTID);
        }
    }

    public static class TransitionParams extends TaskParams {

        private static final String JIRA_CUSTOM_FIELDS_ATTR_KEY = "customFieldsTypeFieldAttr";
        private static final String JIRA_CUSTOM_FIELDS_KV_KEY = "customFieldsTypeKv";
        private static final String JIRA_ISSUE_KEY = "issueKey";
        private static final String JIRA_TRANSITION_COMMENT_KEY = "transitionComment";
        private static final String JIRA_TRANSITION_ID_KEY = "transitionId";

        public TransitionParams(Variables variables) {
            super(variables);
        }

        public String issueKey() {
            return variables.assertString(JIRA_ISSUE_KEY);
        }

        public int transitionId(int defaultValue) {
            return variables.getInt(JIRA_TRANSITION_ID_KEY, defaultValue);
        }

        public String transitionComment() {
            return variables.assertString(JIRA_TRANSITION_COMMENT_KEY);
        }

        public @Nonnull Map<String, String> transitionFieldsTypeKv() {
            return variables.getMap(JIRA_CUSTOM_FIELDS_KV_KEY, Map.of());
        }

        public @Nonnull Map<String, String> transitionFieldsTypeAtt() {
            return variables.getMap(JIRA_CUSTOM_FIELDS_ATTR_KEY, Map.of());
        }
    }

    public static class DeleteIssueParams extends TaskParams {

        private static final String JIRA_ISSUE_KEY = "issueKey";

        public DeleteIssueParams(Variables variables) {
            super(variables);
        }

        public String issueKey() {
            return variables.assertString(JIRA_ISSUE_KEY);
        }
    }

    public static class UpdateIssueParams extends TaskParams {

        private static final String JIRA_ISSUE_KEY = "issueKey";
        private static final String JIRA_FIELDS_KEY = "fields";

        public UpdateIssueParams(Variables variables) {
            super(variables);
        }

        public String issueKey() {
            return variables.assertString(JIRA_ISSUE_KEY);
        }

        public Map<String, Object> fields() {
            return variables.assertMap(JIRA_FIELDS_KEY);
        }
    }

    public static class CreateSubTaskParams extends CreateIssueParams {

        private static final String JIRA_PARENT_ISSUE_KEY = "parentIssueKey";

        public CreateSubTaskParams(Variables variables) {
            super(variables);
        }

        public String parentKey() {
            return variables.assertString(JIRA_PARENT_ISSUE_KEY);
        }

        @Override
        public String issueType() {
            return "Sub-task";
        }

        @Override
        public @Nonnull Map<String, Object> customFieldsTypeAtt() {
            Map<String, Object> customFieldsTypeAtt = new HashMap<>(super.customFieldsTypeAtt());
            customFieldsTypeAtt.put("parent", Collections.singletonMap("key", parentKey()));

            return customFieldsTypeAtt;
        }
    }

    public static class CurrentStatusParams extends TaskParams {

        private static final String JIRA_ISSUE_KEY = "issueKey";

        public CurrentStatusParams(Variables variables) {
            super(variables);
        }

        public String issueKey() {
            return variables.assertString(JIRA_ISSUE_KEY);
        }
    }

    public static class GetIssuesParams extends TaskParams {

        private static final String JIRA_ISSUE_STATUS_KEY = "issueStatus";
        private static final String JIRA_ISSUE_STATUS_OPERATOR_KEY = "statusOperator";
        private static final String JIRA_ISSUE_TYPE_KEY = "issueType";
        private static final String JIRA_PROJECT_KEY = "projectKey";

        public GetIssuesParams(Variables variables) {
            super(variables);
        }

        public String projectKey() {
            return variables.assertString(JIRA_PROJECT_KEY);
        }

        public String issueType() {
            return variables.assertString(JIRA_ISSUE_TYPE_KEY);
        }

        public String issueStatus() {
            return variables.getString(JIRA_ISSUE_STATUS_KEY);
        }

        public String statusOperator() {
            return variables.getString(JIRA_ISSUE_STATUS_OPERATOR_KEY, "=");
        }
    }

    public static Variables merge(Variables variables, Map<String, Object> defaults) {
        Map<String, Object> variablesMap = new HashMap<>(defaults != null ? defaults : Collections.emptyMap());
        variablesMap.putAll(variables.toMap());
        return new MapBackedVariables(variablesMap);
    }

    public enum Action {
        ADDCOMMENT,
        CREATECOMPONENT,
        CREATEISSUE,
        DELETECOMPONENT,
        DELETEISSUE,
        TRANSITION,
        UPDATEISSUE,
        CREATESUBTASK,
        CURRENTSTATUS,
        ADDATTACHMENT,
        GETISSUES
    }
}
