package com.walmartlabs.concord.plugins.jenkins;

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

public final class Constants {

    public static final String API_TOKEN_KEY = "apiToken";
    public static final String BASE_URL_KEY = "baseUrl";
    public static final String CONNECTION_TIMEOUT_KEY = "connectTimeout";
    public static final String DEBUG_KEY = "debug";
    public static final String JOB_NAME_KEY = "jobName";
    public static final String JOB_TIMEOUT_KEY = "jobTimeout";
    public static final String PARAMETERS_KEY = "parameters";
    public static final String READ_TIMEOUT_KEY = "readTimeout";
    public static final String SYNC_KEY = "sync";
    public static final String USERNAME_KEY = "username";
    public static final String WRITE_TIMEOUT_KEY = "writeTimeout";

    public static final String[] ALL_IN_PARAMS = {API_TOKEN_KEY, BASE_URL_KEY, CONNECTION_TIMEOUT_KEY, DEBUG_KEY,
            READ_TIMEOUT_KEY, WRITE_TIMEOUT_KEY, JOB_NAME_KEY, JOB_TIMEOUT_KEY, PARAMETERS_KEY, SYNC_KEY, USERNAME_KEY};

    private Constants() {
    }
}
