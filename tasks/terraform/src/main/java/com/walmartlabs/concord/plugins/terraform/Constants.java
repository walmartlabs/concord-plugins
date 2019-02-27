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
    public static final String DEBUG_KEY = "debug";
    public static final String DEFAULT_ENV_KEY = "defaultEnv";
    public static final String DIR_OR_PLAN_KEY = "dirOrPlan";
    public static final String EXTRA_ENV_KEY = "extraEnv";
    public static final String EXTRA_VARS_KEY = "extraVars";
    public static final String IGNORE_ERRORS_KEY = "ignoreErrors";
    public static final String RESULT_KEY = "result";
    public static final String STATE_ID_KEY = "stateId";

    public static final String[] ALL_IN_PARAMS = {ACTION_KEY, BACKEND_KEY, DEBUG_KEY, DEFAULT_ENV_KEY, DIR_OR_PLAN_KEY,
            EXTRA_ENV_KEY, EXTRA_VARS_KEY, IGNORE_ERRORS_KEY, RESULT_KEY, STATE_ID_KEY};

    private Constants() {
    }
}
