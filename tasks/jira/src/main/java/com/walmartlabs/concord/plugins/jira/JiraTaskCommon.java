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

import com.walmartlabs.concord.common.ConfigurationUtils;
import com.walmartlabs.concord.sdk.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

import static com.walmartlabs.concord.plugins.jira.TaskParams.*;

public class JiraTaskCommon {

    private static final Logger log = LoggerFactory.getLogger(JiraTaskCommon.class);

    // IN params
    private static final String JIRA_ISSUE_ID_KEY = "issueId";
    private static final String JIRA_COMPONENT_ID_KEY = "componentId";
    public static final String JIRA_ISSUE_STATUS_KEY = "issueStatus";
    private static final String JIRA_PASSWORD_KEY = "password";
    private static final int DEFAULT_START_AT = 0;
    private static final int DEFAULT_MAX_RESULTS = 50;

    private static final String SECRET_NAME_KEY = "name";
    private static final String ORG_KEY = "org";
    private static final String USERNAME_KEY = "username";
    private static final String BASIC_KEY = "basic";
    private static final String SECRET_KEY = "secret";

    private final JiraSecretService secretService;

    public JiraTaskCommon(JiraSecretService secretService) {
        this.secretService = secretService;
    }

    public Map<String, Object> execute(TaskParams in) {
        String jiraUrl = formatUrl(in.jiraUrl());
        log.info("Using JIRA URL: {}", jiraUrl);

        Action action = in.action();
        switch (action) {
            case CREATEISSUE: {
                log.info("Starting 'CreateIssue' Action");
                return createIssue((CreateIssueParams)in);
            }
            case ADDCOMMENT: {
                log.info("Starting 'AddComment' Action");
                addComment((AddCommentParams)in);
                break;
            }
            case ADDATTACHMENT: {
                log.info("Starting 'AddAttachment' Action");
                addAttachment((AddAttachmentParams)in);
                break;
            }
            case CREATECOMPONENT: {
                log.info("Starting 'CreateComponent' Action");
                return createComponent((CreateComponentParams)in);
            }
            case DELETECOMPONENT: {
                log.info("Starting 'DeleteComponent' Action");
                deleteComponent((DeleteComponentParams)in);
                break;
            }
            case TRANSITION: {
                log.info("Starting 'Transition' Action");
                transition((TransitionParams)in);
                break;
            }
            case DELETEISSUE: {
                log.info("Starting 'DeleteIssue' Action");
                deleteIssue((DeleteIssueParams)in);
                break;
            }
            case UPDATEISSUE: {
                log.info("Starting 'UpdateIssue' Action");
                updateIssue((UpdateIssueParams)in);
                break;
            }
            case CREATESUBTASK: {
                log.info("Starting 'CreateSubTask' Action");
                return createSubTask((CreateSubTaskParams)in);
            }
            case CURRENTSTATUS: {
                log.info("Starting 'CurrentStatus' Action");
                return currentStatus((CurrentStatusParams)in);
            }
            case GETISSUES: {
                log.info("Starting 'GetIssues' Action");
                return getIssues((GetIssuesParams)in);
            }
            default:
                throw new IllegalArgumentException("Unsupported action type: " + action);
        }
        return Collections.emptyMap();
    }


    Map<String, Object> createIssue(CreateIssueParams in) {
        String projectKey = in.projectKey();
        String summary = in.summary();
        String description = in.description();
        String requestorUid = in.requestorUid();
        String issueType = in.issueType();
        String issuePriority = in.issuePriority();
        Map<String, Object> assignee = in.assignee();
        List<String> labels = in.labels();
        List<String> components = in.components();
        Map<String, String> customFieldsTypeKv = in.customFieldsTypeKv();
        Map<String, Object> customFieldsTypeAtt = in.customFieldsTypeAtt();

        String issueId;

        try {
            //Build JSON data
            Map<String, Object> objProj = Collections.singletonMap("key", projectKey);
            Map<String, Object> objPriority = Collections.singletonMap("name", issuePriority);
            Map<String, Object> objIssueType = Collections.singletonMap("name", issueType);

            Map<String, Object> objMain = new HashMap<>();
            objMain.put("project", objProj);
            objMain.put("summary", summary);
            objMain.put("description", description);

            if (requestorUid != null) {
                objMain.put("reporter", Collections.singletonMap("name", requestorUid));
            }

            objMain.put("priority", objPriority);
            objMain.put("issuetype", objIssueType);

            if (labels != null && !labels.isEmpty()) {
                objMain.put("labels", labels);
            }

            if (components != null && !components.isEmpty()) {
                objMain.put("components", components);
            }

            if (assignee != null && !assignee.isEmpty()) {
                objMain.put("assignee", assignee);
            }

            objMain.putAll(customFieldsTypeKv);
            objMain.putAll(customFieldsTypeAtt);

            Map<String, Object> objFields = Collections.singletonMap("fields", objMain);

            log.info("Creating new issue in '{}'...", projectKey);

            Map<String, Object> results = getClient(in)
                    .url(in.jiraUrl() + "issue/")
                    .jiraAuth(buildAuth(in))
                    .successCode(201)
                    .post(objFields);

            issueId = results.get("key").toString().replace("\"", "");
            log.info("Issue #{} created in Project# '{}'", issueId, projectKey);
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while creating an issue: " + e.getMessage(), e);
        }

        return Collections.singletonMap(JIRA_ISSUE_ID_KEY, issueId);
    }

    Map<String, Object> createSubTask(CreateSubTaskParams in) {
        return createIssue(in);
    }

    Map<String, Object> createComponent(CreateComponentParams in) {
        String projectKey = in.projectKey();
        String componentName = in.componentName();

        try {
            Map<String, Object> m = new HashMap<>();
            m.put("name", componentName);
            m.put("project", projectKey);

            Map<String, Object> results = getClient(in)
                    .url(in.jiraUrl() + "component/")
                    .jiraAuth(buildAuth(in))
                    .successCode(201)
                    .post(m);

            String componentId = results.get("id").toString();
            componentId = componentId.replace("\"", "");
            log.info("Component '{}' created successfully and its Id is '{}'", componentName, componentId);
            return Collections.singletonMap(JIRA_COMPONENT_ID_KEY, componentId);
        } catch (Exception e) {
            throw new RuntimeException("Exception occurred while creating a component: " + e.getMessage(), e);
        }
    }

    void deleteComponent(DeleteComponentParams in) {
        int componentId = in.componentId();

        try {
            getClient(in)
                    .url(in.jiraUrl() + "component/" + componentId)
                    .jiraAuth(buildAuth(in))
                    .successCode(204)
                    .delete();

            log.info("Component# '{}' removed successfully.", componentId);
        } catch (Exception e) {
            throw new RuntimeException("Exception occurred while deleting a component: " + e.getMessage(), e);
        }
    }

    void addAttachment(AddAttachmentParams in) {
        String issueKey = in.issueKey();
        String filePath = in.filePath();

        File file = new File(filePath);
        if (!file.exists()) {
            throw new IllegalArgumentException("File not found: " + filePath);
        }

        try {
            getClient(in)
                    .url(in.jiraUrl() + "issue/" + issueKey + "/attachments")
                    .successCode(200)
                    .jiraAuth(buildAuth(in))
                    .post(file);
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while attaching a file: " + e.getMessage(), e);
        }
    }

    void addComment(AddCommentParams in) {
        String issueKey = in.issueKey();
        String comment = in.comment();
        boolean debug = in.debug();

        try {
            Map<String, Object> m = Collections.singletonMap("body", comment);

            getClient(in)
                    .url(in.jiraUrl() + "issue/" + issueKey + "/comment")
                    .jiraAuth(buildAuth(in))
                    .successCode(201)
                    .post(m);

            if (debug) {
                log.info("Comment '{}' added to Issue #{}", comment, issueKey);
            } else {
                log.info("Comment added to Issue #{}", issueKey);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while adding a comment: " + e.getMessage(), e);
        }
    }

    void transition(TransitionParams in) {
        String issueKey = in.issueKey();
        String transitionId = Integer.toString(in.transitionId(-1));
        String transitionComment = in.transitionComment();
        Map<String, String> transitionFieldsTypeKv = in.transitionFieldsTypeKv();
        Map<String, String> transitionFieldsTypeAtt = in.transitionFieldsTypeAtt();

        try {
            //Build JSON data
            Map<String, Object> objId = Collections.singletonMap("id", transitionId);
            Map<String, Object> objTransition = Collections.singletonMap("transition", objId);
            Map<String, Object> objBody = Collections.singletonMap("body", transitionComment);
            Map<String, Object> objAdd = Collections.singletonMap("add", objBody);
            ArrayList<Map<String, Object>> commentsArray = new ArrayList<>();
            commentsArray.add(objAdd);
            Map<String, Object> objComment = Collections.singletonMap("comment", commentsArray);
            Map<String, Object> objupdate = Collections.singletonMap("update", objComment);

            Map<String, Object> objMain = new HashMap<>();
            transitionFieldsTypeKv.forEach((k, v) -> objMain.put(k, String.valueOf(v)));
            objMain.putAll(transitionFieldsTypeAtt);

            Map<String, Object> objFields = Collections.singletonMap("fields", objMain);
            objupdate = ConfigurationUtils.deepMerge(objFields, ConfigurationUtils.deepMerge(objTransition, objupdate));

            getClient(in)
                    .url(in.jiraUrl() + "issue/" + issueKey + "/transitions")
                    .jiraAuth(buildAuth(in))
                    .successCode(204)
                    .post(objupdate);

            log.info("Transition is successful on Issue #{} to transitionId #{}", issueKey, transitionId);
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while doing a transition: " + e.getMessage(), e);
        }
    }

    void deleteIssue(DeleteIssueParams in) {
        String issueKey = in.issueKey();

        try {
            getClient(in)
                    .url(in.jiraUrl() + "issue/" + issueKey)
                    .jiraAuth(buildAuth(in))
                    .successCode(204)
                    .delete();

            log.info("Issue #{} deleted successfully.", issueKey);
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while deleting an issue: " + e.getMessage(), e);
        }
    }

    void updateIssue(UpdateIssueParams in) {
        String issueKey = in.issueKey();
        Map<String, Object> fields = in.fields();

        log.info("Updating {} fields for issue #{}", fields, issueKey);

        try {
            getClient(in)
                    .url(in.jiraUrl() + "issue/" + issueKey)
                    .jiraAuth(buildAuth(in))
                    .successCode(204)
                    .put(Map.of("fields", fields));

            log.info("Issue #{} updated successfully.", issueKey);
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while updating an issue: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> getIssues(GetIssuesParams in) {
        String projectKey = in.projectKey();
        String issueType = in.issueType();
        String issueStatus = in.issueStatus();
        String statusOperator = in.statusOperator();

        try {
            String jqlQuery = configureStatus(projectKey, issueType, issueStatus, statusOperator);

            log.info("Fetching full list of issue IDs from project '{}' of type '{}' and with status '{} {}'...",
                    projectKey, issueType, statusOperator, issueStatus);

            List<String> issueList = new LinkedList<>();
            int startAt = DEFAULT_START_AT;
            int maxResults = DEFAULT_MAX_RESULTS;

            while (true) {
                Map<String, Object> objMain = new HashMap<>();

                objMain.put("jql", jqlQuery);
                objMain.put("startAt", startAt);
                objMain.put("maxResults", maxResults);

                List<String> fieldList = Collections.singletonList("key");
                objMain.put("fields", fieldList);

                Map<String, Object> results = getClient(in)
                        .url(in.jiraUrl() + "search")
                        .jiraAuth(buildAuth(in))
                        .successCode(200)
                        .post(objMain);

                List<Map<String, Object>> issueMap = (List<Map<String, Object>>) results.get("issues");

                for (Map<String, Object> issue : issueMap) {
                    String key = (String) issue.get("key");
                    issueList.add(key);
                }
                if (issueMap.size() < maxResults) {
                    break;
                }
                startAt += maxResults;
            }

            if (issueList.isEmpty()) {
                throw new RuntimeException("Zero Issues found in project '" + projectKey + "' of type '" + issueType + "' " +
                        "and with status '" + statusOperator + " " + issueStatus + "'");
            }

            Map<String, Object> result = new HashMap<>();
            result.put("issueList", issueList);
            result.put("issueCount", issueList.size());
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Exception occurred while fetching issue ids from project '" + projectKey + "'", e);
        }
    }

    private String buildAuth(TaskParams in) {
        JiraCredentials c = credentials(in);
        return c.authHeaderValue();
    }

    private JiraCredentials credentials(TaskParams in) {
        Map<String, Object> auth = in.auth();
        if (auth == null) {
            String uid = in.uid();
            String pwd = in.pwd();

            return new JiraCredentials(uid, pwd);
        }

        Map<String, Object> basic = MapUtils.getMap(auth, BASIC_KEY, null);
        if (basic == null) {
            Map<String, Object> secret = MapUtils.assertMap(auth, SECRET_KEY);
            return getSecretData(secret);
        }

        String username = MapUtils.assertString(basic, USERNAME_KEY);
        String password = MapUtils.assertString(basic, JIRA_PASSWORD_KEY);

        return new JiraCredentials(username, password);
    }

    private JiraCredentials getSecretData(Map<String, Object> input) {
        String secretName = MapUtils.assertString(input, SECRET_NAME_KEY);
        String org = MapUtils.getString(input, ORG_KEY);
        String password = MapUtils.getString(input, JIRA_PASSWORD_KEY);

        try {
            return getSecretService().exportCredentials(org, secretName, password);
        } catch (Exception e) {
             throw new RuntimeException("Error export credentials: " + e.getMessage());
        }
    }

    private Map<String, Object> currentStatus(CurrentStatusParams in) {
        try {
            Map<String, Object> results = getClient(in)
                    .url(formatUrl(in.jiraUrl()) + "issue/" + in.issueKey() + "?fields=status")
                    .jiraAuth(buildAuth(in))
                    .successCode(200)
                    .get();

            Map<String, Object> fields = MapUtils.get(results, "fields", null);
            if (fields != null) {
                Map<String, Object> statusInfo = MapUtils.get(fields, "status", null);
                if (statusInfo != null) {
                    String status = MapUtils.assertString(statusInfo, "name");
                    return Collections.singletonMap(JIRA_ISSUE_STATUS_KEY, status);
                }
            }

            throw new IllegalStateException("Unexpected data received from JIRA: " + fields);
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while getting the current status: " + e.getMessage(), e);
        }
    }

    private static String formatUrl(String s) {
        if (s == null) {
            return null;
        }

        if (s.endsWith("/")) {
            return s;
        }

        return s + "/";
    }

    private String configureStatus(String projectKey, String issueType, String issueStatus, String statusOperator) {
        String jqlQuery = "project = " + projectKey + " AND " + "issuetype = " + issueType;
        if (issueStatus != null && !issueStatus.isEmpty()) {
            if ("=".equals(statusOperator)) {
                jqlQuery = jqlQuery + " AND " + "status = " + issueStatus;
            } else if ("!=".equals(statusOperator)) {
                jqlQuery = jqlQuery + " AND " + "status != " + issueStatus;
            } else {
                throw new IllegalArgumentException("Invalid statusOperator. Allowed values are only '=', '!=' ");
            }
        }
        return jqlQuery;
    }

    JiraHttpClient getClient(TaskParams in) {
        try {
            return getNativeClient(in);
        } catch (NoClassDefFoundError e) {
            // client2 may not exist
            log.info("Error while creating jira http client: {}", e.getMessage());
            log.info("Add com.walmartlabs.concord.client2 to classpath?");
        }

        throw new IllegalStateException("Unexpected error while creating JiraHttpClient.");
    }

    JiraHttpClient getNativeClient(TaskParams in) {
        return new NativeJiraHttpClient(in);
    }

    JiraSecretService getSecretService() {
        return secretService;
    }

}
