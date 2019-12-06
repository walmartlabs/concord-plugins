package com.walmartlabs.concord.plugins.msteams;

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

public class Constants {

    public static final int DEFAULT_CONNECT_TIMEOUT = 30_000;
    public static final int DEFAULT_SO_TIMEOUT = 30_000;
    public static final int DEFAULT_RETRY_COUNT = 5;

    public static final String MSTEAMS_PARAMS_KEY = "msteamsParams";
    public static final String DEFAULT_THEME_COLOR = "11B00A";
    public static final String TEAM_ID_KEY = "teamId";
    public static final String TENANT_ID_KEY = "tenantId";
    public static final String WEBHOOK_URL_KEY = "webhookUrl";
    public static final String ROOT_WEBHOOK_URL_KEY = "rootWebhookUrl";
    public static final String WEBHOOK_ID_KEY = "webhookId";
    public static final String WEBHOOKTYPE_ID_KEY = "webhookTypeId";
    public static final String ACTION_KEY = "action";
    public static final String MESSAGE_TITLE_KEY = "title";
    public static final String MESSAGE_TEXT_KEY = "text";
    public static final String MESSAGE_THEME_COLOR_KEY = "themeColor";
    public static final String MESSAGE_SECTIONS_KEY = "sections";
    public static final String MESSAGE_POTENTIAL_ACTION_KEY = "potentialAction";
    public static final String IGNORE_ERRORS_KEY = "ignoreErrors";

    public static final String PROXY_ADDRESS_KEY = "proxyAddress";
    public static final String PROXY_PORT_KEY = "proxyPort";
    public static final String CONNECTION_TIMEOUT_KEY = "connectTimeout";
    public static final String SO_TIMEOUT_KEY = "soTimeout";
    public static final String RETRY_COUNT_KEY = "retryCount";

    public static final int TOO_MANY_REQUESTS_ERROR = 429;
    public static final int TEAMS_SUCCESS_STATUS_CODE = 200;
    public static final int DEFAULT_RETRY_AFTER = 1;
    public static final int DEFAULT_PROXY_PORT = 8080;

    public static final String[] ALL_IN_PARAMS = {
            Constants.ACTION_KEY,
            Constants.TEAM_ID_KEY,
            Constants.WEBHOOK_ID_KEY,
            Constants.WEBHOOK_URL_KEY,
            Constants.MESSAGE_TITLE_KEY,
            Constants.MESSAGE_TEXT_KEY,
            Constants.MESSAGE_THEME_COLOR_KEY,
            Constants.MESSAGE_SECTIONS_KEY,
            Constants.MESSAGE_POTENTIAL_ACTION_KEY,
            Constants.IGNORE_ERRORS_KEY
    };

    private Constants() {
    }
}
