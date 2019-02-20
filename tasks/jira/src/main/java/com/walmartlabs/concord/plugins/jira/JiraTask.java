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
import com.walmartlabs.concord.sdk.Context;
import com.walmartlabs.concord.sdk.ContextUtils;
import com.walmartlabs.concord.sdk.InjectVariable;
import com.walmartlabs.concord.sdk.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import java.io.IOException;
import java.util.*;

import static com.walmartlabs.concord.sdk.ContextUtils.*;

/**
 * Created by ppendha on 6/18/18.
 */
@Named("jira")
public class JiraTask implements Task {

    private static final Logger log = LoggerFactory.getLogger(JiraTask.class);

    private static final String ACTION_KEY = "action";
    private static final String JIRA_ASSIGNEE = "assignee";
    private static final String JIRA_COMMENT = "comment";
    private static final String JIRA_COMPONENTID = "componentId";
    private static final String JIRA_COMPONENTNAME = "componentName";
    private static final String JIRA_CUSTOM_FIELDS_ATTR = "customFieldsTypeFieldAttr";
    private static final String JIRA_CUSTOM_FIELDS_KV = "customFieldsTypeKv";
    private static final String JIRA_DESCRIPTION = "description";
    private static final String JIRA_ISSUE_COMPONENTS = "components";
    private static final String JIRA_ISSUE_ID = "issueId";
    private static final String JIRA_ISSUE_KEY = "issueKey";
    private static final String JIRA_ISSUE_LABELS = "labels";
    private static final String JIRA_ISSUE_PRIORITY = "priority";
    private static final String JIRA_ISSUE_TYPE = "issueType";
    private static final String JIRA_PARENT_ISSUE_KEY = "parentIssueKey";
    private static final String JIRA_PROJECT_KEY = "projectKey";
    private static final String JIRA_REQUESTOR_UID = "requestorUid";
    private static final String JIRA_SUMMARY = "summary";
    private static final String JIRA_TRANSITION_COMMENT = "transitionComment";
    private static final String JIRA_TRANSITION_ID = "transitionId";
    private static final String JIRA_URL = "apiUrl";

    @InjectVariable("jiraParams")
    private Map<String, Object> defaults;

    @Override
    public void execute(Context ctx) throws Exception {
        Action action = getAction(ctx);

        // add all defaults to ctx if not already present.  Useful to avoid having to set userId, password, projectKey, etc for each individual task call
        if (defaults != null) {
            Set<String> overrideVars = ctx.getVariableNames();
            defaults.forEach((k, v) -> {
                if (!overrideVars.contains(k)) {
                    ctx.setVariable(k, v);
                }
            });
        }

        String jiraUri = assertString(ctx, JIRA_URL);

        log.info("Using Jira Url {}", jiraUri);

        switch (action) {
            case CREATEISSUE: {
                log.info("Starting 'CreateIssue' Action");
                createIssue(ctx, jiraUri);
                break;
            }
            case ADDCOMMENT: {
                log.info("Starting 'AddComment' Action");
                addComment(ctx, jiraUri);
                break;
            }
            case CREATECOMPONENT: {
                log.info("Starting 'CreateComponent' Action");
                createComponent(ctx, jiraUri);
                break;
            }
            case DELETECOMPONENT: {
                log.info("Starting 'DeleteComponent' Action");
                deleteComponent(ctx, jiraUri);
                break;
            }
            case TRANSITION: {
                log.info("Starting 'Transition' Action");
                transition(ctx, jiraUri);
                break;
            }
            case DELETEISSUE: {
                log.info("Starting 'DeleteIssue' Action");
                deleteIssue(ctx, jiraUri);
                break;
            }
            case UPDATEISSUE: {
                log.info("Starting 'UpdateIssue' Action");
                updateIssue(ctx, jiraUri);
                break;
            }
            case CREATESUBTASK: {
                log.info("Starting 'CreateSubTask' Action");
                createSubTask(ctx, jiraUri);
                break;
            }
            default:
                throw new IllegalArgumentException("Unsupported action type: " + action);
        }
    }

    public String createIssue(Context ctx, String url) {
        String projectKey = assertString(ctx, JIRA_PROJECT_KEY);
        String summary = assertString(ctx, JIRA_SUMMARY);
        String description = assertString(ctx, JIRA_DESCRIPTION);
        String requestorUid = assertString(ctx, JIRA_REQUESTOR_UID);
        String issueType = assertString(ctx, JIRA_ISSUE_TYPE);
        String issuePriority = getString(ctx, JIRA_ISSUE_PRIORITY, null);
        Map<String, Object> assignee = getMap(ctx, JIRA_ASSIGNEE, null);
        List<String> labels = getList(ctx, JIRA_ISSUE_LABELS, null);
        List<String> components = getList(ctx, JIRA_ISSUE_COMPONENTS, null);
        Map<String, String> customFieldsTypeKv = getMap(ctx, JIRA_CUSTOM_FIELDS_KV, null);
        Map<String, Object> customFieldsTypeAtt = getMap(ctx, JIRA_CUSTOM_FIELDS_ATTR, null);

        String issueId;

        try {
            //Build JSON data
            Map<String, Object> objProj = Collections.singletonMap("key", projectKey);
            Map<String, Object> objReporter = Collections.singletonMap("name", requestorUid);
            Map<String, Object> objPriority = Collections.singletonMap("name", issuePriority);
            Map<String, Object> objIssueType = Collections.singletonMap("name", issueType);

            Map<String, Object> objMain = new HashMap<>();
            objMain.put("project", objProj);
            objMain.put("summary", summary);
            objMain.put("description", description);
            objMain.put("reporter", objReporter);
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

            if (customFieldsTypeKv != null && !customFieldsTypeKv.isEmpty()) {
                for (Map.Entry<String, String> e : customFieldsTypeKv.entrySet()) {
                    objMain.put(e.getKey(), String.valueOf(e.getValue()));
                }
            }

            if (customFieldsTypeAtt != null && !customFieldsTypeAtt.isEmpty()) {
                for (Map.Entry<String, Object> e : customFieldsTypeAtt.entrySet()) {
                    objMain.put(e.getKey(), e.getValue());
                }
            }
            Map<String, Object> objFields = Collections.singletonMap("fields", objMain);

            log.info("Creating new issue in '{}'...", projectKey);

            Map<String, Object> results = new JiraClient(ctx)
                    .url(url + "issue/")
                    .successCode(201)
                    .post(objFields);

            issueId = results.get("key").toString();
            issueId = issueId.replaceAll("\"", "");
            ctx.setVariable(JIRA_ISSUE_ID, issueId);
            log.info("Issue #{} created in Project# '{}'", ctx.getVariable(JIRA_ISSUE_ID), projectKey);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error occurred while creating an issue", e);
        }

        return issueId;
    }

    public void createSubTask(Context ctx, String url) {
        String parentKey = assertString(ctx, JIRA_PARENT_ISSUE_KEY);
        Map<String, Object> customFieldsTypeAtt = new HashMap<>(getMap(ctx, JIRA_CUSTOM_FIELDS_ATTR, Collections.emptyMap()));
        customFieldsTypeAtt.put("parent", Collections.singletonMap("key", parentKey));
        ctx.setVariable(JIRA_CUSTOM_FIELDS_ATTR, customFieldsTypeAtt);
        ctx.setVariable(JIRA_ISSUE_TYPE, "Sub-task");

        createIssue(ctx, url);
    }

    public void createComponent(Context ctx, String url) {
        String projectKey = assertString(ctx, JIRA_PROJECT_KEY);
        String componentName = assertString(ctx, JIRA_COMPONENTNAME);

        try {
            Map<String, Object> m = new HashMap<>();
            m.put("name", componentName);
            m.put("project", projectKey);

            Map<String, Object> results = new JiraClient(ctx)
                    .url(url + "component/")
                    .successCode(201)
                    .post(m);

            String componentId = results.get("id").toString();
            componentId = componentId.replaceAll("\"", "");
            log.info("Component '{}' created successfully and its Id is '{}'", componentName, componentId);
        } catch (Exception e) {
            throw new IllegalArgumentException("Exception occurred while creating a component", e);
        }
    }

    public void deleteComponent(Context ctx, String url) {
        int componentId = assertInt(ctx, JIRA_COMPONENTID);

        try {
            new JiraClient(ctx)
                    .url(url + "component/" + componentId)
                    .successCode(204)
                    .delete();

            log.info("Component# '{}' removed successfully.", componentId);
        } catch (IOException e) {
            throw new IllegalArgumentException("Exception occurred while deleting a component", e);
        }
    }

    public void addComment(Context ctx, String url) {
        String issueKey = assertString(ctx, JIRA_ISSUE_KEY);
        String comment = assertString(ctx, JIRA_COMMENT);

        try {
            Map<String, Object> m = Collections.singletonMap("body", comment);

            new JiraClient(ctx)
                    .url(url + "issue/" + issueKey + "/comment")
                    .successCode(201)
                    .post(m);

            log.info("Comment '{}' added to Issue #{}", comment, issueKey);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error occurred while adding a comment", e);
        }
    }

    public void transition(Context ctx, String url) {
        String issueKey = assertString(ctx, JIRA_ISSUE_KEY);
        String transitionId = Integer.toString(getInt(ctx, JIRA_TRANSITION_ID, -1));
        String transitionComment = assertString(ctx, JIRA_TRANSITION_COMMENT);
        Map<String, String> transitionFieldsTypeKv = getMap(ctx, JIRA_CUSTOM_FIELDS_KV, null);
        Map<String, String> transitionFieldsTypeAtt = getMap(ctx, JIRA_CUSTOM_FIELDS_ATTR, null);

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
            if (transitionFieldsTypeKv != null && !transitionFieldsTypeKv.isEmpty()) {
                for (Map.Entry<String, String> e : transitionFieldsTypeKv.entrySet()) {
                    objMain.put(e.getKey(), String.valueOf(e.getValue()));
                }
            }

            if (transitionFieldsTypeAtt != null && !transitionFieldsTypeAtt.isEmpty()) {
                for (Map.Entry<String, String> e : transitionFieldsTypeAtt.entrySet()) {
                    objMain.put(e.getKey(), e.getValue());
                }
            }

            Map<String, Object> objFields = Collections.singletonMap("fields", objMain);
            objupdate = ConfigurationUtils.deepMerge(objFields, ConfigurationUtils.deepMerge(objTransition, objupdate));

            new JiraClient(ctx)
                    .url(url + "issue/" + issueKey + "/transitions")
                    .successCode(204)
                    .post(objupdate);

            log.info("Transition is successful on Issue #{} to transitionId #{}", issueKey, transitionId);
        } catch (IOException e) {
            throw new IllegalArgumentException("Error occurred while doing a transition", e);
        }
    }

    public void deleteIssue(Context ctx, String url) {
        String issueKey = assertString(ctx, JIRA_ISSUE_KEY);

        try {
            new JiraClient(ctx)
                    .url(url + "issue/" + issueKey)
                    .successCode(204)
                    .delete();

            log.info("Issue #{} deleted successfully.", issueKey);
        } catch (IOException e) {
            throw new IllegalArgumentException("Error occurred while deleting an issue", e);
        }
    }

    public void updateIssue(Context ctx, String url) {
        String issueKey = assertString(ctx, JIRA_ISSUE_KEY);
        Map<String, Object> fields = ContextUtils.assertMap(ctx, "fields");

        log.info("Updating {} fields for issue #{}", fields, issueKey);

        try {
            new JiraClient(ctx)
                    .url(url + "issue/" + issueKey)
                    .successCode(204)
                    .put(Collections.singletonMap("fields", fields));

            log.info("Issue #{} updated successfully.", issueKey);
        } catch (IOException e) {
            log.error("Error updating an issue: {}", e.getMessage());
            throw new RuntimeException("Error occurred while updating an issue", e);
        }
    }

    private static Action getAction(Context ctx) {
        return Action.valueOf(assertString(ctx, ACTION_KEY).trim().toUpperCase());
    }

    private enum Action {
        ADDCOMMENT,
        CREATECOMPONENT,
        CREATEISSUE,
        DELETECOMPONENT,
        DELETEISSUE,
        TRANSITION,
        UPDATEISSUE,
        CREATESUBTASK
    }
}
