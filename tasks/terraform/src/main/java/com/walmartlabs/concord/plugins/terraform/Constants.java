package com.walmartlabs.concord.plugins.terraform;

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

    public static final String ACTION_KEY = "action";
    public static final String BACKEND_KEY = "backend";
    public static final String BACKEND_REMOTE_KEY = "remote";
    public static final String DEBUG_KEY = "debug";
    public static final String DEFAULT_ENV_KEY = "defaultEnv";
    public static final String DESTROY_KEY = "destroy";
    public static final String DIR_KEY = "dir";
    public static final String EXTRA_ENV_KEY = "extraEnv";
    public static final String EXTRA_VARS_KEY = "extraVars";
    public static final String GIT_SSH_KEY = "gitSsh";
    public static final String HOSTNAME_KEY = "hostname";
    public static final String IGNORE_ERRORS_KEY = "ignoreErrors";
    public static final String IGNORE_LOCAL_BINARY_KEY = "ignoreLocalBinary";
    public static final String MODULE_KEY = "module";
    public static final String PLAN_KEY = "plan";
    public static final String RESULT_KEY = "result";
    public static final String SAVE_OUTPUT_KEY = "saveOutput";
    public static final String STATE_ID_KEY = "stateId";
    public static final String TOKEN_KEY = "token";
    public static final String TOOL_URL_KEY = "toolUrl";
    public static final String TOOL_VERSION_KEY = "toolVersion";
    public static final String VARS_FILES = "varFiles";
    public static final String VERBOSE_KEY = "verbose";

    public static final String TF_CLI_CONFIG_FILE_KEY = "TF_CLI_CONFIG_FILE";

    public static final String[] ALL_IN_PARAMS = {ACTION_KEY, BACKEND_KEY, DEBUG_KEY, DEFAULT_ENV_KEY, DESTROY_KEY,
            DIR_KEY, EXTRA_ENV_KEY, EXTRA_VARS_KEY, VARS_FILES, GIT_SSH_KEY, IGNORE_ERRORS_KEY, IGNORE_LOCAL_BINARY_KEY,
            MODULE_KEY, PLAN_KEY, RESULT_KEY, SAVE_OUTPUT_KEY, STATE_ID_KEY, VERBOSE_KEY, TOOL_VERSION_KEY, TOOL_URL_KEY};

    private Constants() {
    }
}
