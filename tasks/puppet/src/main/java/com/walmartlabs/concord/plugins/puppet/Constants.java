package com.walmartlabs.concord.plugins.puppet;

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

    private Constants() {
    }

    /**
     * Variable names that may be provided by Concord process Context
     */
    public static class Keys {
        public static final String PARAMS_KEY = "puppetParams";
        public static final String ACTION_KEY = "action";
        public static final String QUERY_STRING_KEY = "queryString";
        public static final String API_TOKEN_KEY = "apiToken";
        public static final String DEBUG_KEY = "debug";
        public static final String IGNORE_ERRORS_KEY = "ignoreErrors";
        public static final String USERNAME_KEY = "username";
        public static final String PASSWORD_KEY = "password";
        public static final String OUT_VARIABLE_KEY = "result";

        // Puppet Endpoints
        public static final String DATABASE_URL_KEY = "databaseUrl";
        public static final String RBAC_URL_KEY = "rbacUrl";

        // RBAC Config Keys
        public static final String TOKEN_LIFETIME_KEY = "tokenLife";
        public static final String TOKEN_LABEL_KEY = "tokenLabel";
        public static final String TOKEN_DESCRIPTION_KEY = "tokenDescription";

        // HTTP Connection Keys
        public static final String CONNECT_TIMEOUT_KEY = "connectTimeout";
        public static final String READ_TIMEOUT_KEY = "readTimeout";
        public static final String WRITE_TIMEOUT_KEY = "writeTimeout";
        public static final String HTTP_VERSION_KEY = "httpVersion";
        public static final String HTTP_RETRIES_KEY = "httpRetries";

        // Certificate info
        public static final String VALIDATE_CERTS_KEY = "validateCerts";
        public static final String VALIDATE_CERTS_NOT_AFTER_KEY = "validateCertsNotAfter";
        public static final String CERTIFICATE_KEY = "certificate";
        public static final String CERTIFICATE_SECRET_KEY = "secret";
        public static final String CERTIFICATE_ORG_KEY = "org";
        public static final String CERTIFICATE_NAME_KEY = "name";
        public static final String CERTIFICATE_PASSWORD_KEY = "password";
        public static final String CERTIFICATE_TEXT_KEY = "text";
        public static final String CERTIFICATE_PATH_KEY = "path";

        // Concord provided vars
        public static final String TX_ID = "txId";
        public static final String WORK_DIR = "workDir";

        /**
         * All root-level input parameters for the task
         * <p>
         * Don't include sub-keys like certificate members
         *
         * <pre>
         * - task: puppet
         *   in:
         *     dataBaseUrl:  # include
         *     certificate:  # include
         *       secret:     # don't include
         *         org:      # don't include
         * </pre>
         */
        private static final String[] ALL_IN_PARAMS = {ACTION_KEY, QUERY_STRING_KEY, API_TOKEN_KEY, DEBUG_KEY,
                IGNORE_ERRORS_KEY, USERNAME_KEY, PASSWORD_KEY, DATABASE_URL_KEY, RBAC_URL_KEY, TOKEN_LIFETIME_KEY, TOKEN_LABEL_KEY,
                TOKEN_DESCRIPTION_KEY, CONNECT_TIMEOUT_KEY, READ_TIMEOUT_KEY, WRITE_TIMEOUT_KEY, VALIDATE_CERTS_KEY,
                CERTIFICATE_KEY, TX_ID, WORK_DIR};

        public static String[] getAllInParams() {
            return ALL_IN_PARAMS.clone();
        }

        private Keys() {
        }
    }


    public static class ApiConst {
        public static final String AUTHENTICATION_TOKEN = "X-Authentication";

        private ApiConst() {
        }
    }

    public static class Actions {
        public static final String DB_QUERY = "pql";
        public static final String CREATE_API_TOKEN = "createApiToken";
        public static final String NONE = "none";

        private Actions() {
        }
    }
}
