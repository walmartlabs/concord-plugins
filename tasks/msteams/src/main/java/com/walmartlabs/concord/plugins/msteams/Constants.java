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

    public static final String DEFAULT_THEME_COLOR = "11B00A";
    public static final String VAR_ACCESS_TOKEN = "access_token";

    public static final String API_BOT_FRAMEWORK_SCOPE = "https://api.botframework.com/.default";
    public static final String API_BOT_FRAMEWORK_GRANT_TYPE = "client_credentials";

    public static final int TOO_MANY_REQUESTS_ERROR = 429;
    public static final int TEAMS_SUCCESS_STATUS_CODE = 200;
    public static final int TEAMS_SUCCESS_STATUS_CODE_V2 = 201;
    public static final int DEFAULT_RETRY_AFTER = 1;
    public static final int DEFAULT_PROXY_PORT = 8080;

    private Constants() {
    }
}
