package com.walmartlabs.concord.plugins.taurus;

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

import java.util.*;

public class TaskParams {

    public static TaskParams of(Variables input, Map<String, Object> defaults) {
        Variables variables = merge(input, defaults);

        Action action = new TaskParams(variables).action();
        switch (action) {
            case RUN: {
                return new RunParams(variables);
            }
            default:
                throw new IllegalArgumentException("Unsupported action type: " + action);
        }
    }

    private static final String ACTION_KEY = "action";
    private static final String DOWNLOAD_PLUGINS = "downloadPlugins";
    private static final String USE_FAKE_HOME_KEY = "useFakeHome";
    private static final String JMETER_URL_KEY = "jmeterArchiveUrl";

    protected final Variables variables;

    public TaskParams(Variables variables) {
        this.variables = variables;
    }

    public Action action() {
        String action = variables.assertString(ACTION_KEY);
        try {
            return Action.valueOf(action.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Unknown action: '" + action + "'. Available actions: " + Arrays.toString(Action.values()));
        }
    }

    public boolean useFakeHome() {
        return variables.getBoolean(USE_FAKE_HOME_KEY, true);
    }
    public boolean downloadPlugins() {
        return variables.getBoolean(DOWNLOAD_PLUGINS, false);
    }
    public String jmeterArchiveUrl() {
        return variables.getString(JMETER_URL_KEY, Constants.DEFAULT_JMETER_URL);
    }

    public static class RunParams extends TaskParams {

        private static final String CONFIGS_KEY = "configs";
        private static final String IGNORE_ERRORS_KEY = "ignoreErrors";
        private static final String NO_SYS_CONFIG_KEY = "noSysConfig";
        private static final String PROXY_KEY = "proxy";
        private static final String QUIET_KEY = "quiet";
        private static final String VERBOSE_KEY = "verbose";

        public RunParams(Variables variables) {
            super(variables);
        }

        public boolean ignoreErrors() {
            return variables.getBoolean(IGNORE_ERRORS_KEY, false);
        }

        public boolean verbose() {
            return variables.getBoolean(VERBOSE_KEY, false);
        }

        public boolean quiet() {
            return variables.getBoolean(QUIET_KEY, false);
        }

        public boolean noSysConfig() {
            return variables.getBoolean(NO_SYS_CONFIG_KEY, false);
        }

        public String proxy() {
            return variables.getString(PROXY_KEY);
        }

        public List<Object> configs() {
            return variables.assertList(CONFIGS_KEY);
        }

    }

    private static Variables merge(Variables variables, Map<String, Object> defaults) {
        Map<String, Object> variablesMap = new HashMap<>(defaults != null ? defaults : Collections.emptyMap());
        variablesMap.putAll(variables.toMap());
        return new MapBackedVariables(variablesMap);
    }

    public enum Action {
        RUN
    }
}
