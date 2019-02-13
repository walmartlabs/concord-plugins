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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.okhttp.*;
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
import java.util.concurrent.TimeUnit;

/**
 * Created by ppendha on 6/18/18.
 */
@Named("jira")
public class JiraTask implements Task {

    private static final Logger log = LoggerFactory.getLogger(JiraTask.class);

    public static final String ACTION_KEY = "action";
    public static final String CLIENT_CONNECTTIMEOUT = "connectTimeout";
    public static final String CLIENT_READTIMEOUT = "readTimeout";
    public static final String CLIENT_WRITETIMEOUT = "writeTimeout";
    public static final String JIRA_ASSIGNEE = "assignee";
    public static final String JIRA_COMMENT = "comment";
    public static final String JIRA_COMPONENTID = "componentId";
    public static final String JIRA_COMPONENTNAME = "componentName";
    public static final String JIRA_CUSTOM_FIELDS_ATTR = "customFieldsTypeFieldAttr";
    public static final String JIRA_CUSTOM_FIELDS_KV = "customFieldsTypeKv";
    public static final String JIRA_DESCRIPTION = "description";
    public static final String JIRA_ISSUE_COMPONENTS = "components";
    public static final String JIRA_ISSUE_ID = "issueId";
    public static final String JIRA_ISSUE_KEY = "issueKey";
    public static final String JIRA_ISSUE_LABELS = "labels";
    public static final String JIRA_ISSUE_PRIORITY = "priority";
    public static final String JIRA_ISSUE_TYPE = "issueType";
    public static final String JIRA_PROJECT_KEY = "projectKey";
    public static final String JIRA_PWD = "password";
    public static final String JIRA_REQUESTOR_UID = "requestorUid";
    public static final String JIRA_SUMMARY = "summary";
    public static final String JIRA_TRANSITION_COMMENT = "transitionComment";
    public static final String JIRA_TRANSITION_ID = "transitionId";
    public static final String JIRA_UID = "userId";
    public static final String JIRA_URL = "apiUrl";

    private static final OkHttpClient client = new OkHttpClient();
    private static final Gson gson = new GsonBuilder().create();

    @InjectVariable("jiraParams")
    private Map<String, Object> defaults;

    @Override
    public void execute(Context ctx) throws Exception {
        Action action = getAction(ctx);
        String jiraUri = getString(defaults, ctx, JIRA_URL, null);

        // add all defaults to ctx if not already present.  Useful to avoid having to set userId, password, projectKey, etc for each individual task call
        if (defaults != null) {
            Set<String> overrideVars = ctx.getVariableNames();
            defaults.forEach((k, v) -> {
                if (!overrideVars.contains(k)) {
                    ctx.setVariable(k, v);
                }
            });
        }

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
            default:
                throw new IllegalArgumentException("Unsupported action type: " + action);
        }
    }

    @SuppressWarnings("unchecked")
    public String createIssue(Context ctx, String url) {
        String uid = ContextUtils.assertString(ctx, JIRA_UID);
        String pwd = ContextUtils.assertString(ctx, JIRA_PWD);
        String projectKey = ContextUtils.assertString(ctx, JIRA_PROJECT_KEY);
        String summary = ContextUtils.assertString(ctx, JIRA_SUMMARY);
        String description = ContextUtils.assertString(ctx, JIRA_DESCRIPTION);
        String requestorUid = ContextUtils.assertString(ctx, JIRA_REQUESTOR_UID);
        String issueType = ContextUtils.assertString(ctx, JIRA_ISSUE_TYPE);
        String issuePriority = ContextUtils.getString(ctx, JIRA_ISSUE_PRIORITY, null);
        Map<String, Object> assignee = ContextUtils.getMap(ctx, JIRA_ASSIGNEE, null);
        List<String> labels = ContextUtils.getList(ctx, JIRA_ISSUE_LABELS, null);
        List<String> components = ContextUtils.getList(ctx, JIRA_ISSUE_COMPONENTS, null);
        Map<String, String> customFieldsTypeKv = ContextUtils.getMap(ctx, JIRA_CUSTOM_FIELDS_KV, null);
        Map<String, String> customFieldsTypeAtt = ContextUtils.getMap(ctx, JIRA_CUSTOM_FIELDS_ATTR, null);

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
                    String k = e.getKey();
                    Object v = e.getValue();
                    objMain.put(k, String.valueOf(v));
                }
            }

            if (customFieldsTypeAtt != null && !customFieldsTypeAtt.isEmpty()) {
                for (Map.Entry<String, String> e : customFieldsTypeAtt.entrySet()) {
                    String k = e.getKey();
                    Object v = e.getValue();
                    objMain.put(k, v);
                }
            }
            Map<String, Object> objFields = Collections.singletonMap("fields", objMain);
            String data = gson.toJson(objFields);

            url = url + "issue/";
            log.info("Creating new issue in '{}'...", projectKey);

            try {
                //set client timeouts
                setClientTimeoutParams(ctx);

                RequestBody body = RequestBody.create(
                        MediaType.parse("application/json; charset=utf-8"), data);
                Request request = new Request.Builder()
                        .url(url)
                        .addHeader("Authorization", Credentials.basic(uid, pwd))
                        .post(body)
                        .build();


                Call call = client.newCall(request);
                Response response = call.execute();
                int statusCode = response.code();
                try (ResponseBody responseBody = response.body()) {
                    String results = null;
                    if (responseBody != null) {
                        results = responseBody.string();
                    }

                    assertResponseCode(statusCode, results, 201);

                    Map<String, Object> objresults = gson.fromJson(results, Map.class);
                    issueId = objresults.get("key").toString();
                    issueId = issueId.replaceAll("\"", "");
                    ctx.setVariable(JIRA_ISSUE_ID, issueId);
                    log.info("Issue #{} created in Project# '{}'", ctx.getVariable(JIRA_ISSUE_ID), projectKey);
                }
            } catch (IOException e) {
                throw new IllegalArgumentException("Failed to create an issue:", e);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Error occurred while creating an issue", e);
        }

        return issueId;
    }

    @SuppressWarnings("unchecked")
    public void createComponent(Context ctx, String url) {
        String uid = ContextUtils.assertString(ctx, JIRA_UID);
        String pwd = ContextUtils.assertString(ctx, JIRA_PWD);
        String projectKey = ContextUtils.assertString(ctx, JIRA_PROJECT_KEY);
        String componentName = ContextUtils.assertString(ctx, JIRA_COMPONENTNAME);

        try {
            //Build JSON data
            Map<String, Object> m = new HashMap<>();
            m.put("name", componentName);
            m.put("project", projectKey);

            String data = gson.toJson(m);
            url = url + "component/";

            //set client timeouts
            setClientTimeoutParams(ctx);

            RequestBody body = RequestBody.create(
                    MediaType.parse("application/json; charset=utf-8"), data);
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", Credentials.basic(uid, pwd))
                    .post(body)
                    .build();


            Call call = client.newCall(request);
            Response response = call.execute();
            int statusCode = response.code();
            try (ResponseBody responseBody = response.body()) {
                String results = null;
                if (responseBody != null) {
                    results = responseBody.string();
                }


                assertResponseCode(statusCode, results, 201);

                Map<String, Object> objresults = gson.fromJson(results, Map.class);
                String componentId = objresults.get("id").toString();
                componentId = componentId.replaceAll("\"", "");
                log.info("Component '{}' created successfully and its Id is '{}'", componentName, componentId);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Exception occurred while creating a component", e);
        }
    }

    public void deleteComponent(Context ctx, String url) throws Exception {
        String uid = ContextUtils.assertString(ctx, JIRA_UID);
        String pwd = ContextUtils.assertString(ctx, JIRA_PWD);
        Integer componentId = ContextUtils.assertInt(ctx, JIRA_COMPONENTID);
        url = url + "component/" + componentId;

        try {
            //set client timeouts
            setClientTimeoutParams(ctx);
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", Credentials.basic(uid, pwd))
                    .delete().build();

            Call call = client.newCall(request);
            Response response = call.execute();
            int statusCode = response.code();
            try (ResponseBody responseBody = response.body()) {
                String results = null;
                if (responseBody != null) {
                    results = responseBody.string();
                }

                assertResponseCode(statusCode, results, 204);

                log.info("Component# '{}' removed successfully.", componentId);
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Exception occurred while deleting a component", e);
        }
    }

    public void addComment(Context ctx, String url) throws Exception {
        String uid = ContextUtils.assertString(ctx, JIRA_UID);
        String pwd = ContextUtils.assertString(ctx, JIRA_PWD);
        String issueKey = ContextUtils.assertString(ctx, JIRA_ISSUE_KEY);
        String comment = ContextUtils.assertString(ctx, JIRA_COMMENT);

        try {
            //Build JSON data
            Map<String, Object> m = Collections.singletonMap("body", comment);
            String data = gson.toJson(m);

            url = url + "issue/" + issueKey + "/comment";

            //set client timeouts
            setClientTimeoutParams(ctx);

            RequestBody body = RequestBody.create(
                    MediaType.parse("application/json; charset=utf-8"), data);
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", Credentials.basic(uid, pwd))
                    .post(body)
                    .build();

            Call call = client.newCall(request);
            Response response = call.execute();
            int statusCode = response.code();
            try (ResponseBody responseBody = response.body()) {
                String results = null;
                if (responseBody != null) {
                    results = responseBody.string();
                }

                assertResponseCode(statusCode, results, 201);

                log.info("Comment '{}' added to Issue #{}", comment, issueKey);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Error occurred while adding a comment", e);
        }
    }

    public void transition(Context ctx, String url) throws Exception {
        String uid = ContextUtils.assertString(ctx, JIRA_UID);
        String pwd = ContextUtils.assertString(ctx, JIRA_PWD);
        String issueKey = ContextUtils.assertString(ctx, JIRA_ISSUE_KEY);
        String transitionId = Integer.toString(ContextUtils.getInt(ctx, JIRA_TRANSITION_ID, -1));
        String transitionComment = ContextUtils.assertString(ctx, JIRA_TRANSITION_COMMENT);
        Map<String, String> transitionFieldsTypeKv = ContextUtils.getMap(ctx, JIRA_CUSTOM_FIELDS_KV, null);
        Map<String, String> transitionFieldsTypeAtt = ContextUtils.getMap(ctx, JIRA_CUSTOM_FIELDS_ATTR, null);

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
                    String k = e.getKey();
                    Object v = e.getValue();
                    objMain.put(k, String.valueOf(v));
                }
            }

            if (transitionFieldsTypeAtt != null && !transitionFieldsTypeAtt.isEmpty()) {
                for (Map.Entry<String, String> e : transitionFieldsTypeAtt.entrySet()) {
                    String k = e.getKey();
                    Object v = e.getValue();
                    objMain.put(k, v);
                }
            }

            Map<String, Object> objFields = Collections.singletonMap("fields", objMain);
            objupdate = ConfigurationUtils.deepMerge(objFields, ConfigurationUtils.deepMerge(objTransition, objupdate));
            String data = gson.toJson(objupdate);
            url = url + "issue/" + issueKey + "/transitions";

            //set client timeouts
            setClientTimeoutParams(ctx);

            RequestBody body = RequestBody.create(
                    MediaType.parse("application/json; charset=utf-8"), data);
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", Credentials.basic(uid, pwd))
                    .post(body)
                    .build();

            Call call = client.newCall(request);
            Response response = call.execute();
            int statusCode = response.code();
            try (ResponseBody responseBody = response.body()) {
                String results = null;
                if (responseBody != null) {
                    results = responseBody.string();
                }

                assertResponseCode(statusCode, results, 204);

                log.info("Transition is successful on Issue #{} to transitionId #{}", issueKey, transitionId);
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Error occurred while doing a transition", e);
        }
    }

    public void deleteIssue(Context ctx, String url) throws Exception {
        String uid = ContextUtils.assertString(ctx, JIRA_UID);
        String pwd = ContextUtils.assertString(ctx, JIRA_PWD);
        String issueKey = ContextUtils.assertString(ctx, JIRA_ISSUE_KEY);

        url = url + "issue/" + issueKey;

        try {
            //set client timeouts
            setClientTimeoutParams(ctx);

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", Credentials.basic(uid, pwd))
                    .delete().build();

            Call call = client.newCall(request);
            Response response = call.execute();
            int statusCode = response.code();
            try (ResponseBody responseBody = response.body()) {
                String results = null;
                if (responseBody != null) {
                    results = responseBody.string();
                }

                assertResponseCode(statusCode, results, 204);

                log.info("Issue #{} deleted successfully.", issueKey);
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Error occurred while deleting an issue", e);
        }
    }

    public void updateIssue(Context ctx, String url) {
        String uid = ContextUtils.assertString(ctx, JIRA_UID);
        String pwd = ContextUtils.assertString(ctx, JIRA_PWD);
        String issueKey = ContextUtils.assertString(ctx, JIRA_ISSUE_KEY);
        Map<String, Object> fields = ContextUtils.assertMap(ctx, "fields");

        log.info("Updating {} fields for issue #{} as {} user", fields, issueKey, uid);

        url = url + "issue/" + issueKey;

        //set client timeouts
        setClientTimeoutParams(ctx);

        RequestBody body = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"),
                gson.toJson(Collections.singletonMap("fields", fields)));

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", Credentials.basic(uid, pwd))
                .put(body)
                .build();

        Call call = client.newCall(request);
        try {
            Response response = call.execute();
            try (ResponseBody responseBody = response.body()) {
                String results = null;
                if (responseBody != null) {
                    results = responseBody.string();
                }

                assertResponseCode(response.code(), results, 204);

                log.info("Issue #{} updated successfully.", issueKey);
            }
        } catch (IOException e) {
            log.error("Error updating an issue: {}", e.getMessage());
            throw new RuntimeException("Error occurred while updating an issue", e);
        }
    }

    private static void assertResponseCode(int code, String result, int successCode) {
        if (code == successCode) {
            return;
        }

        if (code == 400) {
            throw new RuntimeException("input is invalid (e.g. missing required fields, invalid values). Here are the full error details: " + result);
        } else if (code == 401) {
            throw new RuntimeException("User is not authenticated. Here are the full error details: " + result);
        } else if (code == 403) {
            throw new RuntimeException("User does not have permission to perform request. Here are the full error details: " + result);
        } else if (code == 404) {
            throw new RuntimeException("Issue does not exist. Here are the full error details: " + result);
        } else if (code == 500) {
            throw new RuntimeException("Internal Server Error. Here are the full error details" + result);
        } else {
            throw new RuntimeException("Error: " + result);
        }
    }

    private static Action getAction(Context ctx) {
        return Action.valueOf(ContextUtils.assertString(ctx, ACTION_KEY).trim().toUpperCase());
    }

    private void setClientTimeoutParams(Context ctx) {
        long connectTimeout = getLong(defaults, ctx, CLIENT_CONNECTTIMEOUT, 30L);
        long readTimeout = getLong(defaults, ctx, CLIENT_READTIMEOUT, 30L);
        Long writeTimeout = getLong(defaults, ctx, CLIENT_WRITETIMEOUT, 30L);

        client.setConnectTimeout(connectTimeout, TimeUnit.SECONDS);
        client.setReadTimeout(readTimeout, TimeUnit.SECONDS);
        client.setWriteTimeout(writeTimeout, TimeUnit.SECONDS);
    }

    private static Long getLong(Map<String, Object> defaults, Context ctx, String k, Long defaultValue) {
        Object v = getValue(defaults, ctx, k, defaultValue);

        if (v instanceof Integer) {
            v = ((Integer) v).longValue();
        }

        if (!(v instanceof Long)) {
            throw new IllegalArgumentException("'" + k + "': expected a number, got " + v);
        }

        return (Long) v;
    }

    private static String getString(Map<String, Object> defaults, Context ctx, String k, String defaultValue) {
        Object v = getValue(defaults, ctx, k, defaultValue);
        if (!(v instanceof String)) {
            throw new IllegalArgumentException("'" + k + "': expected a string value, got " + v);
        }
        return (String) v;
    }

    private static Object getValue(Map<String, Object> defaults, Context ctx, String k, Object defaultValue) {
        Object v = ctx.getVariable(k);

        if (v == null && defaults != null) {
            v = defaults.get(k);
        }

        if (v == null) {
            v = defaultValue;
        }

        if (v == null) {
            throw new IllegalArgumentException("Mandatory parameter '" + k + "' is required");
        }

        return v;
    }

    private enum Action {
        ADDCOMMENT,
        CREATECOMPONENT,
        CREATEISSUE,
        DELETECOMPONENT,
        DELETEISSUE,
        TRANSITION,
        UPDATEISSUE
    }
}
