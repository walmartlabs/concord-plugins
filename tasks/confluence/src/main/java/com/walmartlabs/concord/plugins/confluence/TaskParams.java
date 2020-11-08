package com.walmartlabs.concord.plugins.confluence;

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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class TaskParams {

    private static final String ACTION_KEY = "action";
    private static final String IGNORE_ERRORS_KEY = "ignoreErrors";
    private static final String CONFLUENCE_PWD = "password";
    private static final String CONFLUENCE_UID = "userId";
    private static final String CLIENT_CONNECTTIMEOUT = "connectTimeout";
    private static final String CLIENT_READTIMEOUT = "readTimeout";
    private static final String CLIENT_WRITETIMEOUT = "writeTimeout";
    private static final String DEBUG = "debug";

    protected final Variables variables;

    public TaskParams(Variables variables) {
        this.variables = variables;
    }

    public static TaskParams of(Variables input, Map<String, Object> defaults) {
        Variables variables = merge(input, defaults);

        Action action = new TaskParams(variables).action();
        switch (action) {
            case CREATEPAGE: {
                return new CreatePageParams(variables);
            }
            case UPDATEPAGE: {
                return new UpdatePageParams(variables);
            }
            case ADDCOMMENTSTOPAGE: {
                return new AddCommentsToPage(variables);
            }
            case UPLOADATTACHMENT: {
                return new UploadAttachmentParams(variables);
            }
            case CREATECHILDPAGE: {
                return new CreateChildPageParams(variables);
            }
            case GETPAGECONTENT: {
                return new GetPageParams(variables);
            }
            case DELETEPAGE: {
                return new DeletePageParams(variables);
            }
            default:
                throw new IllegalArgumentException("Unsupported action type: " + action);
        }
    }

    public Action action() {
        String action = variables.assertString(ACTION_KEY);
        try {
            return Action.valueOf(action.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Unknown action: '" + action + "'. Available actions: " + Arrays.toString(Action.values()));
        }
    }

    public String apiUrl() {
        return variables.assertString("apiUrl");
    }

    public String userId() {
        return variables.assertString(CONFLUENCE_UID);
    }

    public String password() {
        return variables.assertString(CONFLUENCE_PWD);
    }

    public long connectTimeout() {
        return variables.getNumber(CLIENT_CONNECTTIMEOUT, Constants.CONNECTION_TIMEOUT).longValue();
    }

    public long readTimeout() {
        return variables.getNumber(CLIENT_READTIMEOUT, Constants.READ_TIMEOUT).longValue();
    }

    public long writeTimeout() {
        return variables.getNumber(CLIENT_WRITETIMEOUT, Constants.WRITE_TIMEOUT).longValue();
    }

    public String pageViewInfoUrl() {
        return variables.getString("pageViewInfoUrl");
    }

    public boolean ignoreErrors() {
        return variables.getBoolean(IGNORE_ERRORS_KEY, false);
    }

    public boolean debug() {
        return variables.getBoolean(DEBUG, false);
    }

    public static class CreatePageParams extends TaskParams {

        private static final String CONFLUENCE_SPACE_KEY = "spaceKey";
        private static final String CONFLUENCE_PAGE_TITLE = "pageTitle";
        private static final String CONFLUENCE_PAGE_CONTENT = "pageContent";
        private static final String CONFLUENCE_PAGE_CONTENT_TEMPLATE = "template";
        private static final String CONFLUENCE_PAGE_CONTENT_TEMPLATE_PARAMS = "templateParams";

        public CreatePageParams(Variables variables) {
            super(variables);
        }

        public String spaceKey() {
            return variables.assertString(CONFLUENCE_SPACE_KEY);
        }

        public String pageTitle() {
            return variables.assertString(CONFLUENCE_PAGE_TITLE);
        }

        public String pageContent() {
            return variables.getString(CONFLUENCE_PAGE_CONTENT);
        }

        public String template() {
            return variables.getString(CONFLUENCE_PAGE_CONTENT_TEMPLATE);
        }

        public Map<String, Object> templateParams() {
            return variables.getMap(CONFLUENCE_PAGE_CONTENT_TEMPLATE_PARAMS, Collections.emptyMap());
        }
    }

    public static class CreateChildPageParams extends TaskParams {

        private static final String CONFLUENCE_SPACE_KEY = "spaceKey";
        private static final String CONFLUENCE_PAGE_CONTENT_TEMPLATE = "template";
        private static final String CONFLUENCE_PAGE_CONTENT_TEMPLATE_PARAMS = "templateParams";
        private static final String CONFLUENCE_CHILDPAGE_TITLE = "childPageTitle";
        private static final String CONFLUENCE_CHILDPAGE_CONTENT = "childPageContent";
        private static final String CONFLUENCE_PARENTPAGE_ID = "parentPageId";

        public CreateChildPageParams(Variables variables) {
            super(variables);
        }

        public String spaceKey() {
            return variables.assertString(CONFLUENCE_SPACE_KEY);
        }

        public String childPageTitle() {
            return variables.assertString(CONFLUENCE_CHILDPAGE_TITLE);
        }

        public String childPageContent() {
            return variables.getString(CONFLUENCE_CHILDPAGE_CONTENT, null);
        }

        public String template() {
            return variables.getString(CONFLUENCE_PAGE_CONTENT_TEMPLATE, null);
        }

        public Map<String, Object> templateParams() {
            return variables.getMap(CONFLUENCE_PAGE_CONTENT_TEMPLATE_PARAMS, Collections.emptyMap());
        }

        public int parentPageId() {
            return variables.assertInt(CONFLUENCE_PARENTPAGE_ID);
        }
    }

    public static class UpdatePageParams extends TaskParams {

        private static final String CONFLUENCE_SPACE_KEY = "spaceKey";
        private static final String CONFLUENCE_PAGE_TITLE = "pageTitle";
        private static final String CONFLUENCE_PAGE_UPDATE = "pageUpdate";
        private static final String OVERWRITE_CONTENT_KEY = "overWrite";

        public UpdatePageParams(Variables variables) {
            super(variables);
        }

        public String spaceKey() {
            return variables.assertString(CONFLUENCE_SPACE_KEY);
        }

        public String pageTitle() {
            return variables.assertString(CONFLUENCE_PAGE_TITLE);
        }

        public boolean overWrite() {
            return variables.getBoolean(OVERWRITE_CONTENT_KEY, false);
        }

        public String pageUpdate() {
            return variables.assertString(CONFLUENCE_PAGE_UPDATE);
        }
    }

    public static class DeletePageParams extends TaskParams {

        private static final String CONFLUENCE_PAGE_ID = "pageId";

        public DeletePageParams(Variables variables) {
            super(variables);
        }

        public int pageId() {
            return variables.assertInt(CONFLUENCE_PAGE_ID);
        }
    }

    public static class GetPageParams extends TaskParams {

        private static final String CONFLUENCE_PAGE_ID = "pageId";

        public GetPageParams(Variables variables) {
            super(variables);
        }

        public int pageId() {
            return variables.assertInt(CONFLUENCE_PAGE_ID);
        }
    }

    public static class UploadAttachmentParams extends TaskParams {

        private static final String CONFLUENCE_PAGE_ID = "pageId";
        private static final String CONFLUENCE_ATTACHMENT_COMMENT = "attachmentComment";
        private static final String CONFLUENCE_ATTACHMENT_PATH = "attachmentPath";

        public UploadAttachmentParams(Variables variables) {
            super(variables);
        }


        public int pageId() {
            return variables.assertInt(CONFLUENCE_PAGE_ID);
        }

        public String attachmentComment() {
            return variables.assertString(CONFLUENCE_ATTACHMENT_COMMENT);
        }

        public String attachmentPath() {
            return variables.assertString(CONFLUENCE_ATTACHMENT_PATH);
        }
    }

    public static class AddCommentsToPage extends TaskParams {

        private static final String CONFLUENCE_PAGE_ID = "pageId";
        private static final String CONFLUENCE_PAGE_COMMENT = "pageComment";

        public AddCommentsToPage(Variables variables) {
            super(variables);
        }

        public int pageId() {
            return variables.assertInt(CONFLUENCE_PAGE_ID);
        }

        public String pageComment() {
            return variables.assertString(CONFLUENCE_PAGE_COMMENT);
        }
    }

    private static Variables merge(Variables variables, Map<String, Object> defaults) {
        Map<String, Object> variablesMap = new HashMap<>(defaults != null ? defaults : Collections.emptyMap());
        variablesMap.putAll(variables.toMap());
        return new MapBackedVariables(variablesMap);
    }

    public enum Action {
        CREATEPAGE,
        UPDATEPAGE,
        DELETEPAGE,
        ADDCOMMENTSTOPAGE,
        UPLOADATTACHMENT,
        CREATECHILDPAGE,
        GETPAGECONTENT
    }
}
