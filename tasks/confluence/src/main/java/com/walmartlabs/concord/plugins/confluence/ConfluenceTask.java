package com.walmartlabs.concord.plugins.confluence;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2019 Walmart Inc.
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.walmartlabs.concord.sdk.Context;
import com.walmartlabs.concord.sdk.InjectVariable;
import com.walmartlabs.concord.sdk.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.walmartlabs.concord.sdk.ContextUtils.*;

@Named("confluence")
public class ConfluenceTask implements Task {

    private static final Logger log = LoggerFactory.getLogger(ConfluenceTask.class);

    private static final String ACTION_KEY = "action";
    private static final String IGNORE_ERRORS_KEY = "ignoreErrors";
    private static final String OVERWRITE_CONTENT_KEY = "overWrite";
    private static final String CONFLUENCE_PAGE_TITLE = "pageTitle";
    private static final String CONFLUENCE_PAGE_CONTENT = "pageContent";
    private static final String CONFLUENCE_PAGE_CONTENT_TEMPLATE = "template";
    private static final String CONFLUENCE_PAGE_CONTENT_TEMPLATE_PARAMS = "templateParams";
    private static final String CONFLUENCE_PAGE_UPDATE = "pageUpdate";
    private static final String CONFLUENCE_PAGE_COMMENT = "pageComment";
    private static final String CONFLUENCE_SPACE_KEY = "spaceKey";
    private static final String CONFLUENCE_PAGE_ID = "pageId";
    private static final String CONFLUENCE_CHILDPAGE_TITLE = "childPageTitle";
    private static final String CONFLUENCE_CHILDPAGE_CONTENT = "childPageContent";
    private static final String CONFLUENCE_PARENTPAGE_ID = "parentPageId";
    private static final String CONFLUENCE_ATTACHMENT_PATH = "attachmentPath";
    private static final String CONFLUENCE_ATTACHMENT_COMMENT = "attachmentComment";

    @InjectVariable("confluenceParams")
    private Map<String, Object> defaults;

    @Override
    public void execute(Context ctx) {
        Action action = getAction(ctx);

        Map<String, Object> cfg = createCfg(ctx);
        String confluenceUri = cfg.get("apiUrl").toString();
        String pageViewInfoUrl = cfg.get("pageViewInfoUrl").toString();

        log.info("Starting '{}' action...", action);
        log.info("Using confluence Url {}", confluenceUri);

        switch (action) {
            case CREATEPAGE: {
                createPage(ctx, confluenceUri, pageViewInfoUrl);
                break;
            }
            case UPDATEPAGE: {
                updatePage(ctx, confluenceUri, pageViewInfoUrl);
                break;
            }
            case ADDCOMMENTSTOPAGE: {
                addCommentsToPage(ctx, confluenceUri, pageViewInfoUrl);
                break;
            }
            case UPLOADATTACHMENT: {
                uploadAttachment(ctx, confluenceUri, pageViewInfoUrl);
                break;
            }
            case CREATECHILDPAGE: {
                createChildPage(ctx, confluenceUri, pageViewInfoUrl);
                break;
            }
            case GETPAGECONTENT: {
                getPageContent(ctx, confluenceUri);
                break;
            }
            case DELETEPAGE: {
                deletePage(ctx, confluenceUri);
                break;
            }
            default:
                throw new IllegalArgumentException("Unsupported action type: " + action);
        }
    }

    private void createPage(Context ctx, String url, String pageViewInfoUrl) {
        String spaceKey = assertString(ctx, CONFLUENCE_SPACE_KEY);
        String pageTitle = assertString(ctx, CONFLUENCE_PAGE_TITLE);
        String pageContent = getString(ctx, CONFLUENCE_PAGE_CONTENT, null);
        String template = getString(ctx, CONFLUENCE_PAGE_CONTENT_TEMPLATE, null);
        Map<String, Object> templateParams = getMap(ctx, CONFLUENCE_PAGE_CONTENT_TEMPLATE_PARAMS, null);

        boolean ignoreErrors = getBoolean(ctx, IGNORE_ERRORS_KEY, false);
        try {
            //Build JSON data
            Map<String, Object> objMain = new HashMap<>();
            objMain.put("type", Constants.CONFLUENCE_ENTITY_TYPE_PAGE);
            objMain.put("title", pageTitle);
            Map<String, Object> objSpaceKey = Collections.singletonMap("key", spaceKey);
            objMain.put("space", objSpaceKey);

            Object content = createContent(ctx, template, templateParams, pageContent, "content");

            Map<String, Object> objStorage = new HashMap<>();
            objStorage.put("value", content);
            objStorage.put("representation", Constants.CONFLUENCE_ENTITY_REPRESENTATION);
            Map<String, Object> objBody = Collections.singletonMap("storage", objStorage);

            objMain.put("body", objBody);


            log.info("Creating new confluence page under space '{}'...", spaceKey);

            Map<String, Object> results = new ConfluenceClient(ctx)
                    .url(url + "content/")
                    .post(objMain);

            Integer id = Integer.parseInt(results.get("id").toString());
            Result result = Result.ok(id, null, pageViewInfoUrl + id);

            ObjectMapper om = new ObjectMapper();
            ctx.setVariable("result", om.convertValue(result, Map.class));
            ctx.setVariable("id", result.getpageId());


            log.info("Confluence page with title '{}' is created under space '{}' and its Id is: '{}'.",
                    pageTitle, spaceKey, result.getpageId());

        } catch (Exception e) {
            Result result = Result.error(e.getMessage());

            ObjectMapper om = new ObjectMapper();
            ctx.setVariable("result", om.convertValue(result, Map.class));

            if (ignoreErrors) {
                log.warn("Finished with a generic error (networking or internal Confluence errors). For details check for the 'ERROR' in logs: {}", e.getMessage());
            } else {
                throw new RuntimeException("Error occurred while creating a confluence page", e);
            }
        }
    }

    private void updatePage(Context ctx, String url, String pageViewInfoUrl) {
        String spaceKey = assertString(ctx, CONFLUENCE_SPACE_KEY);
        String pageTitle = assertString(ctx, CONFLUENCE_PAGE_TITLE);
        boolean ignoreErrors = getBoolean(ctx, IGNORE_ERRORS_KEY, false);
        boolean overWrite = getBoolean(ctx, OVERWRITE_CONTENT_KEY, false);

        String pageUpdate;
        try {

            //Get confluence page id
            int pageId = Integer.parseInt(Utils.getPageId(ctx, url, pageTitle, spaceKey));
            log.info("Id of page '{}/{}': '{}'.", spaceKey, pageTitle, pageId);

            int version = Integer.parseInt(Utils.getPageCurrentVersion(ctx, url, pageId));
            log.info("Current version of page '{}/{}' is: '{}'.", spaceKey, pageTitle, version);
            log.info("Incrementing the page version accordingly as a part of 'updatePage' action...");

            //Build JSON data
            Map<String, Object> objMain = new HashMap<>();
            objMain.put("id", pageId);
            objMain.put("type", Constants.CONFLUENCE_ENTITY_TYPE_PAGE);
            objMain.put("title", pageTitle);

            Map<String, Object> objSpaceKey = Collections.singletonMap("key", spaceKey);
            objMain.put("space", objSpaceKey);

            if (overWrite) {
                pageUpdate = assertString(ctx, CONFLUENCE_PAGE_UPDATE);
                log.info("Overwriting content of confluence page '{}/{}'...", spaceKey, pageTitle);
            } else {
                pageUpdate = Utils.getPageContent(ctx, url, pageId) + assertString(ctx, CONFLUENCE_PAGE_UPDATE);
                log.info("Appending content of confluence page '{}/{}'...", spaceKey, pageTitle);
            }

            Map<String, Object> objStorage = new HashMap<>();
            objStorage.put("value", pageUpdate);
            objStorage.put("representation", Constants.CONFLUENCE_ENTITY_REPRESENTATION);
            Map<String, Object> objBody = Collections.singletonMap("storage", objStorage);

            objMain.put("body", objBody);

            Map<String, Object> objVersion = Collections.singletonMap("number", version + 1);
            objMain.put("version", objVersion);

            new ConfluenceClient(ctx)
                    .url(url + "content/" + pageId)
                    .put(objMain);

            Result result = Result.ok(null, null, pageViewInfoUrl + pageId);

            ObjectMapper om = new ObjectMapper();
            ctx.setVariable("result", om.convertValue(result, Map.class));

            log.info("Confluence page '{}' is updated.", pageTitle);

        } catch (Exception e) {
            Result result = Result.error(e.getMessage());
            ObjectMapper om = new ObjectMapper();
            ctx.setVariable("result", om.convertValue(result, Map.class));

            if (ignoreErrors) {
                log.warn("Finished with a generic error (networking or internal Confluence errors). For details check for the 'ERROR' in logs: {}", e.getMessage());
            } else {
                throw new RuntimeException("Error occurred while updating a confluence page", e);
            }
        }
    }

    private void addCommentsToPage(Context ctx, String url, String pageViewInfoUrl) {
        int pageId = assertInt(ctx, CONFLUENCE_PAGE_ID);
        String pageComment = assertString(ctx, CONFLUENCE_PAGE_COMMENT);
        boolean ignoreErrors = getBoolean(ctx, IGNORE_ERRORS_KEY, false);

        try {
            //Build JSON data
            Map<String, Object> objMain = new HashMap<>();

            Map<String, Object> objStorage = new HashMap<>();
            objStorage.put("value", pageComment);
            objStorage.put("representation", Constants.CONFLUENCE_ENTITY_REPRESENTATION);
            Map<String, Object> objBody = Collections.singletonMap("storage", objStorage);
            objMain.put("body", objBody);


            Map<String, Object> objContainer = new HashMap<>();
            objContainer.put("type", Constants.CONFLUENCE_ENTITY_TYPE_PAGE);
            objContainer.put("id", pageId);
            objMain.put("container", objContainer);

            objMain.put("type", Constants.CONFLUENCE_ENTITY_TYPE_COMMENT);

            log.info("Adding comments to confluence page# '{}''...", pageId);

            new ConfluenceClient(ctx)
                    .url(url + "content")
                    .post(objMain);

            Result result = Result.ok(null, null, pageViewInfoUrl + pageId);

            ObjectMapper om = new ObjectMapper();
            ctx.setVariable("result", om.convertValue(result, Map.class));

            log.info("Comments added to Confluence page# '{}'.", pageId);

        } catch (Exception e) {
            Result result = Result.error(e.getMessage());

            ObjectMapper om = new ObjectMapper();
            ctx.setVariable("result", om.convertValue(result, Map.class));

            if (ignoreErrors) {
                log.warn("Finished with a generic error (networking or internal Confluence errors). For details check for the 'ERROR' in logs: {}", e.getMessage());
            } else {
                throw new RuntimeException("Error occurred while adding comments a confluence page", e);
            }
        }
    }

    private void uploadAttachment(Context ctx, String url, String pageViewInfoUrl) {
        int pageId = assertInt(ctx, CONFLUENCE_PAGE_ID);
        String attachmentComment = assertString(ctx, CONFLUENCE_ATTACHMENT_COMMENT);
        String attachmentPath = assertString(ctx, CONFLUENCE_ATTACHMENT_PATH);
        boolean ignoreErrors = getBoolean(ctx, IGNORE_ERRORS_KEY, false);

        try {
            Path workDir = Paths.get((String) ctx.getVariable(com.walmartlabs.concord.sdk.Constants.Context.WORK_DIR_KEY));
            attachmentPath = workDir.resolve(attachmentPath).toString();
            File file = new File(attachmentPath);

            log.info("Uploading file to confluence page# '{}''...", pageId);
            new ConfluenceClient(ctx)
                    .url(url + "content/" + pageId + "/child/attachment")
                    .postFormData(attachmentComment, file);

            Result result = Result.ok(null, null, pageViewInfoUrl + pageId);
            ObjectMapper om = new ObjectMapper();
            ctx.setVariable("result", om.convertValue(result, Map.class));

            log.info("File uploaded to confluence page# '{}'.", pageId);

        } catch (Exception e) {
            Result result = Result.error(e.getMessage());

            ObjectMapper om = new ObjectMapper();
            ctx.setVariable("result", om.convertValue(result, Map.class));

            if (ignoreErrors) {
                log.warn("Finished with a generic error (networking or internal Confluence errors). For details check for the 'ERROR' in logs: {}", e.getMessage());
            } else {
                throw new RuntimeException("Error occurred while uploading a file an confluence page", e);
            }
        }
    }

    private void createChildPage(Context ctx, String url, String pageViewInfoUrl) {
        String spaceKey = assertString(ctx, CONFLUENCE_SPACE_KEY);
        String childPageTitle = assertString(ctx, CONFLUENCE_CHILDPAGE_TITLE);
        String childPageContent = getString(ctx, CONFLUENCE_CHILDPAGE_CONTENT, null);
        String template = getString(ctx, CONFLUENCE_PAGE_CONTENT_TEMPLATE, null);
        Map<String, Object> templateParams = getMap(ctx, CONFLUENCE_PAGE_CONTENT_TEMPLATE_PARAMS, null);
        int parentPageId = assertInt(ctx, CONFLUENCE_PARENTPAGE_ID);
        boolean ignoreErrors = getBoolean(ctx, IGNORE_ERRORS_KEY, false);

        try {
            //Build JSON data
            Map<String, Object> objMain = new HashMap<>();
            objMain.put("type", Constants.CONFLUENCE_ENTITY_TYPE_PAGE);
            objMain.put("title", childPageTitle);
            Map<String, Object> objSpaceKey = Collections.singletonMap("key", spaceKey);
            objMain.put("space", objSpaceKey);

            Object content = createContent(ctx, template, templateParams, childPageContent, "content");

            Map<String, Object> objStorage = new HashMap<>();
            objStorage.put("value", content);
            objStorage.put("representation", Constants.CONFLUENCE_ENTITY_REPRESENTATION);
            Map<String, Object> objBody = Collections.singletonMap("storage", objStorage);
            objMain.put("body", objBody);

            Map<String, Object> objAncestors = new HashMap<>();
            objAncestors.put("id", parentPageId);

            ArrayList<Map<String, Object>> ancestorsArray = new ArrayList<>();
            ancestorsArray.add(objAncestors);

            objMain.put("ancestors", ancestorsArray);

            log.info("Creating child page under parent page# '{}/{}'...", spaceKey, parentPageId);

            Map<String, Object> results = new ConfluenceClient(ctx)
                    .url(url + "content/")
                    .post(objMain);

            Integer id = Integer.parseInt(results.get("id").toString());
            Result result = Result.ok(null, id, pageViewInfoUrl + id);

            ObjectMapper om = new ObjectMapper();
            ctx.setVariable("result", om.convertValue(result, Map.class));
            ctx.setVariable("childId", result.childId);

            log.info("Child page '{}' is created under parent# '{}/{}' and its Id is: '{}'",
                    childPageTitle, spaceKey, parentPageId, result.getChildId());

        } catch (Exception e) {
            Result result = Result.error(e.getMessage());

            ObjectMapper om = new ObjectMapper();
            ctx.setVariable("result", om.convertValue(result, Map.class));

            if (ignoreErrors) {
                log.warn("Finished with a generic error (networking or internal Confluence errors). For details check for the 'ERROR' in logs: {}", e.getMessage());
            } else {
                throw new RuntimeException("Error occurred while creating a child page", e);
            }
        }
    }

    private void deletePage(Context ctx, String url) {
        int pageId = assertInt(ctx, CONFLUENCE_PAGE_ID);
        boolean ignoreErrors = getBoolean(ctx, IGNORE_ERRORS_KEY, false);

        try {
            log.info("Deleting confluence page# '{}''...", pageId);
            new ConfluenceClient(ctx)
                    .url(url + "content/" + pageId)
                    .delete();

            Result result = Result.ok(null, null, "Confluence page deleted");

            ObjectMapper om = new ObjectMapper();
            ctx.setVariable("result", om.convertValue(result, Map.class));

            log.info("Confluence page# '{}' is deleted .", pageId);

        } catch (Exception e) {
            Result result = Result.error(e.getMessage());
            ObjectMapper om = new ObjectMapper();
            ctx.setVariable("result", om.convertValue(result, Map.class));

            if (ignoreErrors) {
                log.warn("Finished with a generic error (networking or internal Confluence errors). For details check for the 'ERROR' in logs: {}", e.getMessage());
            } else {
                throw new RuntimeException("Error occurred while deleting an confluence page", e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void getPageContent(Context ctx, String url) {
        int pageId = assertInt(ctx, CONFLUENCE_PAGE_ID);
        boolean ignoreErrors = getBoolean(ctx, IGNORE_ERRORS_KEY, false);

        try {
            log.info("Retrieving content confluence page# '{}''...", pageId);
            Map<String, Object> results = new ConfluenceClient(ctx)
                    .url(url + "content/" + pageId + "?expand=body.storage")
                    .get();

            Map<String, Object> bodyList = (Map<String, Object>) results.get("body");
            if (!bodyList.isEmpty()) {
                Map<String, Object> storageList = (Map<String, Object>) bodyList.get("storage");
                if (!storageList.isEmpty()) {
                    String data = storageList.get("value").toString();

                    Result result = Result.ok(null, null, data);
                    ObjectMapper om = new ObjectMapper();
                    ctx.setVariable("result", om.convertValue(result, Map.class));
                    log.info("Content retrieved from confluence page# '{}''...", pageId);
                } else {
                    throw new RuntimeException("Could not find any associated content/body for page# '" + pageId + "'");
                }
            } else {
                throw new RuntimeException("Could not find any associated content/body for page# '" + pageId + "'");
            }
        } catch (Exception e) {
            Result result = Result.error(e.getMessage());

            ObjectMapper om = new ObjectMapper();
            ctx.setVariable("result", om.convertValue(result, Map.class));
            if (ignoreErrors) {
                log.warn("Finished with a generic error (networking or internal Confluence errors). For details check for the 'ERROR' in logs: {}", e.getMessage());
            } else {
                throw new RuntimeException("Error occurred while retrieving page content", e);
            }
        }
    }

    private static Action getAction(Context ctx) {
        return Action.valueOf(assertString(ctx, ACTION_KEY).trim().toUpperCase());
    }

    private Map<String, Object> createCfg(Context ctx) {
        Map<String, Object> m = new HashMap<>(defaults != null ? defaults : Collections.emptyMap());
        return m;
    }

    private static Object createContent(Context ctx, String template, Map<String, Object> params, String content, String key) {
        Object m;
        try {
            m = Utils.applyTemplate(ctx, template, params).get(key);
            if (m == null) {
                if (content != null && !content.isEmpty()) {
                    m = content;
                } else {
                    throw new RuntimeException("One of the mandatory parameters 'pageContent' or 'template' is missing. Make sure to pass one of them as input parameter to the action.");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while creating page content.", e);
        }
        return m;
    }

    private enum Action {
        CREATEPAGE,
        UPDATEPAGE,
        DELETEPAGE,
        ADDCOMMENTSTOPAGE,
        UPLOADATTACHMENT,
        CREATECHILDPAGE,
        GETPAGECONTENT
    }
}
