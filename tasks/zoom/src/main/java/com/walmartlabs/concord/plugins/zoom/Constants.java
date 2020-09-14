package com.walmartlabs.concord.plugins.zoom;

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

    public static final String VAR_ACCESS_TOKEN = "access_token";
    public static final String CHAT_POST_MESSAGE_API = "im/chat/messages";

    public static final int TOO_MANY_REQUESTS_ERROR = 429;
    public static final int ZOOM_SUCCESS_STATUS_CODE = 201;
    public static final int DEFAULT_RETRY_AFTER = 1;
    public static final int DEFAULT_PROXY_PORT = 9080;

    private Constants() {
    }
}
