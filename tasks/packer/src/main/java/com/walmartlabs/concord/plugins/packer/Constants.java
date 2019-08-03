package com.walmartlabs.concord.plugins.packer;

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

    public static final String DEBUG_KEY = "debug";
    public static final String FORCE_KEY = "force";
    public static final String PARALLEL_BUILDS_KEY = "parallelBuilds";
    public static final String DEFAULT_ENV_KEY = "defaultEnv";
    public static final String DIR_KEY = "dir";
    public static final String EXCEPT_KEY = "except";
    public static final String ONLY_KEY = "only";
    public static final String EXTRA_ENV_KEY = "extraEnv";
    public static final String EXTRA_VARS_KEY = "extraVars";
    public static final String RESULT_KEY = "result";
    public static final String SAVE_OUTPUT_KEY = "saveOutput";
    public static final String VERBOSE_KEY = "verbose";
    public static final String IGNORE_ERRORS_KEY = "ignoreErrors";

    // Plugin constants
    public static final String PACKER_LOG_PREFIX = "\u001b[35mbuild\u001b[0m";

    public static final String[] ALL_IN_PARAMS = { DEBUG_KEY, FORCE_KEY, PARALLEL_BUILDS_KEY, DEFAULT_ENV_KEY, DIR_KEY,
            EXCEPT_KEY, ONLY_KEY, EXTRA_ENV_KEY, EXTRA_VARS_KEY, RESULT_KEY, SAVE_OUTPUT_KEY, VERBOSE_KEY,
            IGNORE_ERRORS_KEY };

    private Constants() {
    }
}
