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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.walmartlabs.concord.plugins.confluence.TaskParams.*;

public class ConfluenceTaskCommon {

    private static final Logger log = LoggerFactory.getLogger(ConfluenceTaskCommon.class);

    private final Path workDir;
    private final Map<String, Object> defaultTemplateParams;

    public ConfluenceTaskCommon(Path workDir, Map<String, Object> defaultTemplateParams) {
        this.workDir = workDir;
        this.defaultTemplateParams = defaultTemplateParams;
    }

    public Result execute(TaskParams in) {
        Action action = in.action();

        log.info("Starting '{}' action...", action);
        log.info("Using confluence Url {}", in.apiUrl());

        switch (action) {
            case CREATEPAGE: {
                return createPage((CreatePageParams)in);
            }
            case UPDATEPAGE: {
                return updatePage((UpdatePageParams)in);
            }
            case ADDCOMMENTSTOPAGE: {
                return addCommentsToPage((AddCommentsToPage)in);
            }
            case UPLOADATTACHMENT: {
                return uploadAttachment((UploadAttachmentParams)in);
            }
            case CREATECHILDPAGE: {
                return createChildPage((CreateChildPageParams)in);
            }
            case GETPAGECONTENT: {
                return getPageContent((GetPageParams)in);
            }
            case DELETEPAGE: {
                return deletePage((DeletePageParams)in);
            }
            default:
                throw new IllegalArgumentException("Unsupported action type: " + action);
        }
    }

    private Result createPage(CreatePageParams in) {
        String spaceKey = in.spaceKey();
        String pageTitle = in.pageTitle();
        String pageContent = in.pageContent();
        String template = in.template();
        Map<String, Object> templateParams = in.templateParams();

        try {
            //Build JSON data
            Map<String, Object> objMain = new HashMap<>();
            objMain.put("type", Constants.CONFLUENCE_ENTITY_TYPE_PAGE);
            objMain.put("title", pageTitle);
            Map<String, Object> objSpaceKey = Collections.singletonMap("key", spaceKey);
            objMain.put("space", objSpaceKey);

            Object content = createContent(template, templateParams, pageContent, "content");

            Map<String, Object> objStorage = new HashMap<>();
            objStorage.put("value", content);
            objStorage.put("representation", Constants.CONFLUENCE_ENTITY_REPRESENTATION);
            Map<String, Object> objBody = Collections.singletonMap("storage", objStorage);

            objMain.put("body", objBody);

            log.info("Creating new confluence page under space '{}'...", spaceKey);

            Map<String, Object> results = new ConfluenceClient(in)
                    .url("content/")
                    .post(objMain);

            Long id = Long.parseLong(results.get("id").toString());

            log.info("Confluence page with title '{}' is created under space '{}' and its Id is: '{}'.",
                    pageTitle, spaceKey, id);
            return Result.ok(id, null, in.pageViewInfoUrl() + id);
        } catch (Exception e) {
            if (in.ignoreErrors()) {
                log.warn("Finished with a generic error (networking or internal Confluence errors). For details check for the 'ERROR' in logs: {}", e.getMessage());
            } else {
                throw new RuntimeException("Error occurred while creating a confluence page", e);
            }
            return Result.error(e.getMessage());
        }
    }

    private Result updatePage(UpdatePageParams in) {
        String spaceKey = in.spaceKey();
        String pageTitle = in.pageTitle();

        String pageUpdate;
        try {
            //Get confluence page id
            long pageId = Long.parseLong(Utils.getPageId(in, pageTitle, spaceKey));
            log.info("Id of page '{}/{}': '{}'.", spaceKey, pageTitle, pageId);

            long version = Long.parseLong(Utils.getPageCurrentVersion(in, pageId));
            log.info("Current version of page '{}/{}' is: '{}'.", spaceKey, pageTitle, version);
            log.info("Incrementing the page version accordingly as a part of 'updatePage' action...");

            //Build JSON data
            Map<String, Object> objMain = new HashMap<>();
            objMain.put("id", pageId);
            objMain.put("type", Constants.CONFLUENCE_ENTITY_TYPE_PAGE);
            objMain.put("title", pageTitle);

            Map<String, Object> objSpaceKey = Collections.singletonMap("key", spaceKey);
            objMain.put("space", objSpaceKey);

            if (in.overWrite()) {
                pageUpdate = in.pageUpdate();
                log.info("Overwriting content of confluence page '{}/{}'...", spaceKey, pageTitle);
            } else {
                pageUpdate = Utils.getPageContent(in, pageId) + in.pageUpdate();
                log.info("Appending content of confluence page '{}/{}'...", spaceKey, pageTitle);
            }

            Map<String, Object> objStorage = new HashMap<>();
            objStorage.put("value", pageUpdate);
            objStorage.put("representation", Constants.CONFLUENCE_ENTITY_REPRESENTATION);
            Map<String, Object> objBody = Collections.singletonMap("storage", objStorage);

            objMain.put("body", objBody);

            Map<String, Object> objVersion = Collections.singletonMap("number", version + 1);
            objMain.put("version", objVersion);

            new ConfluenceClient(in)
                    .url("content/" + pageId)
                    .put(objMain);

            log.info("Confluence page '{}' is updated.", pageTitle);

            return Result.ok(null, null, in.pageViewInfoUrl() + pageId);
        } catch (Exception e) {
            if (in.ignoreErrors()) {
                log.warn("Finished with a generic error (networking or internal Confluence errors). For details check for the 'ERROR' in logs: {}", e.getMessage());
            } else {
                throw new RuntimeException("Error occurred while updating a confluence page", e);
            }
            return Result.error(e.getMessage());
        }
    }

    private Result addCommentsToPage(AddCommentsToPage in) {
        long pageId = in.pageId();

        try {
            //Build JSON data
            Map<String, Object> objMain = new HashMap<>();

            Map<String, Object> objStorage = new HashMap<>();
            objStorage.put("value", in.pageComment());
            objStorage.put("representation", Constants.CONFLUENCE_ENTITY_REPRESENTATION);
            Map<String, Object> objBody = Collections.singletonMap("storage", objStorage);
            objMain.put("body", objBody);


            Map<String, Object> objContainer = new HashMap<>();
            objContainer.put("type", Constants.CONFLUENCE_ENTITY_TYPE_PAGE);
            objContainer.put("id", pageId);
            objMain.put("container", objContainer);

            objMain.put("type", Constants.CONFLUENCE_ENTITY_TYPE_COMMENT);

            log.info("Adding comments to confluence page# '{}''...", pageId);

            new ConfluenceClient(in)
                    .url("content")
                    .post(objMain);

            log.info("Comments added to Confluence page# '{}'.", pageId);
            return Result.ok(null, null, in.pageViewInfoUrl() + pageId);
        } catch (Exception e) {
            if (in.ignoreErrors()) {
                log.warn("Finished with a generic error (networking or internal Confluence errors). For details check for the 'ERROR' in logs: {}", e.getMessage());
            } else {
                throw new RuntimeException("Error occurred while adding comments a confluence page", e);
            }
            return Result.error(e.getMessage());
        }
    }

    private Result uploadAttachment(UploadAttachmentParams in) {
        long pageId = in.pageId();
        String attachmentComment = in.attachmentComment();

        try {
            log.info("Uploading file to confluence page# '{}''...", pageId);
            new ConfluenceClient(in)
                    .url("content/" + pageId + "/child/attachment")
                    .postFormData(attachmentComment, workDir.resolve(in.attachmentPath()).toFile());

            log.info("File uploaded to confluence page# '{}'.", pageId);
            return Result.ok(null, null, in.pageViewInfoUrl() + pageId);
        } catch (Exception e) {
            if (in.ignoreErrors()) {
                log.warn("Finished with a generic error (networking or internal Confluence errors). For details check for the 'ERROR' in logs: {}", e.getMessage());
            } else {
                throw new RuntimeException("Error occurred while uploading a file an confluence page", e);
            }
            return Result.error(e.getMessage());
        }
    }

    private Result createChildPage(CreateChildPageParams in) {
        String spaceKey = in.spaceKey();
        String childPageTitle = in.childPageTitle();
        String childPageContent = in.childPageContent();
        String template = in.template();
        Map<String, Object> templateParams = in.templateParams();
        long parentPageId = in.parentPageId();

        try {
            //Build JSON data
            Map<String, Object> objMain = new HashMap<>();
            objMain.put("type", Constants.CONFLUENCE_ENTITY_TYPE_PAGE);
            objMain.put("title", childPageTitle);
            Map<String, Object> objSpaceKey = Collections.singletonMap("key", spaceKey);
            objMain.put("space", objSpaceKey);

            Object content = createContent(template, templateParams, childPageContent, "content");

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

            Map<String, Object> results = new ConfluenceClient(in)
                    .url("content/")
                    .post(objMain);

            Long id = Long.parseLong(results.get("id").toString());

            log.info("Child page '{}' is created under parent# '{}/{}' and its Id is: '{}'",
                    childPageTitle, spaceKey, parentPageId, id);

            return Result.ok(null, id, in.pageViewInfoUrl() + id);
        } catch (Exception e) {
            if (in.ignoreErrors()) {
                log.warn("Finished with a generic error (networking or internal Confluence errors). For details check for the 'ERROR' in logs: {}", e.getMessage());
            } else {
                throw new RuntimeException("Error occurred while creating a child page", e);
            }
            return Result.error(e.getMessage());
        }
    }

    private Result deletePage(DeletePageParams in) {
        long pageId = in.pageId();

        try {
            log.info("Deleting confluence page# '{}''...", pageId);
            new ConfluenceClient(in)
                    .url("content/" + pageId)
                    .delete();

            log.info("Confluence page# '{}' is deleted .", pageId);
            return Result.ok(null, null, "Confluence page deleted");
        } catch (Exception e) {
            if (in.ignoreErrors()) {
                log.warn("Finished with a generic error (networking or internal Confluence errors). For details check for the 'ERROR' in logs: {}", e.getMessage());
            } else {
                throw new RuntimeException("Error occurred while deleting an confluence page", e);
            }
            return Result.error(e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Result getPageContent(GetPageParams in) {
        try {
            log.info("Retrieving content confluence page# '{}''...", in.pageId());
            Map<String, Object> results = new ConfluenceClient(in)
                    .url("content/" + in.pageId() + "?expand=body.storage")
                    .get();

            Map<String, Object> bodyList = (Map<String, Object>) results.get("body");
            if (bodyList == null || bodyList.isEmpty()) {
                throw new RuntimeException("Could not find any associated content/body for page# '" + in.pageId() + "'");
            }

            Map<String, Object> storageList = (Map<String, Object>) bodyList.get("storage");
            if (storageList == null || storageList.isEmpty()) {
                throw new RuntimeException("Could not find any associated content/body for page# '" + in.pageId() + "'");
            }

            String data = storageList.get("value").toString();
            log.info("Content retrieved from confluence page# '{}''...", in.pageId());
            return Result.ok(null, null, data);
        } catch (Exception e) {
            if (in.ignoreErrors()) {
                log.warn("Finished with a generic error (networking or internal Confluence errors). For details check for the 'ERROR' in logs: {}", e.getMessage());
            } else {
                throw new RuntimeException("Error occurred while retrieving page content", e);
            }
            return Result.error(e.getMessage());
        }
    }

    private Object createContent(String template, Map<String, Object> params, String content, String key) {
        Object m;
        try {
            Map<String, Object> templateParams = new HashMap<>(defaultTemplateParams);
            templateParams.putAll(params);

            m = Utils.applyTemplate(workDir, template, templateParams).get(key);
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
}
