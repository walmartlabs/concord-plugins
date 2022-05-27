package com.walmartlabs.concord.plugins.akeyless;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2022 Walmart Inc., Concord Authors
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
import com.walmartlabs.concord.sdk.MapUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class TaskParams {

    public static final String DEFAULT_BASE_API = "https://api.akeyless.io";
    public static final String DEFAULT_PARAMS_KEY = "akeylessParams";
    public static final String TX_ID_KEY = "txId";

    public static final String ACTION_KEY = "action";
    public static final String API_TOKEN_KEY = "apiToken";
    public static final String API_TOKEN_SECRET_KEY = "apiTokenSecret";
    public static final String ORG_KEY = "org";
    public static final String NAME_KEY = "name";
    public static final String PASSWORD_KEY = "password";
    public static final String BASE_URL_KEY = "baseUrl";
    public static final String NAMESPACE_KEY = "namespace";

    public static final String API_BASE_PATH_KEY = "apiBasePath";
    public static final String ACCESS_ID_KEY = "accessId";
    public static final String ACCESS_KEY_KEY = "accessKey";
    public static final String SECRET_PATH_KEY = "secretPath";

    protected final Variables variables;

    private TaskParams(Variables variables) {
        this.variables = variables;
    }

    public static TaskParams of(Map<String, Object> input,
                                Map<String, Object> defaults,
                                Map<String, Object> policyDefaults,
                                SecretExporter secretExporter) {

        Map<String, Object> variablesMap = new HashMap<>(policyDefaults != null ? policyDefaults : Collections.emptyMap());
        variablesMap.putAll(defaults);
        variablesMap.putAll(input);

        if (variablesMap.containsKey(API_TOKEN_SECRET_KEY)) {
            Map<String, Object> tokenSecret = MapUtils.assertMap(variablesMap, API_TOKEN_SECRET_KEY);
            variablesMap.put(API_TOKEN_KEY, exportToken(secretExporter, tokenSecret));
        }

        Variables variables = new MapBackedVariables(variablesMap);
        TaskParams p = new TaskParams(variables);

        switch (p.action()) {
            case GETSECRET:
            case WRITEKV:
                return new TaskParams(variables);
            default:
                throw new IllegalArgumentException("Unsupported action type: " + p.action());
        }
    }

    private static String exportToken(SecretExporter secretExporter, Map<String, Object> secret) {
        String o = MapUtils.assertString(secret, ORG_KEY);
        String n = MapUtils.assertString(secret, NAME_KEY);
        String p = MapUtils.getString(secret, PASSWORD_KEY);

        String token;

        try {
            token = secretExporter.exportAsString(o, n, p);
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving API token from Concord secret: " + e.getMessage());
        }
        return token;
    }


    public Action action() {
        return Action.valueOf(variables.assertString(ACTION_KEY).toUpperCase());
    }

    public String apiBasePath() {
        return variables.getString(API_BASE_PATH_KEY, DEFAULT_BASE_API);
    }

    public String accessId() {
        return variables.assertString(ACCESS_ID_KEY);
    }

    public String accessKey() {
        return variables.assertString(ACCESS_KEY_KEY);
    }

    public String secretPath() {
        return variables.assertString(SECRET_PATH_KEY);
    }

    public enum Action {
        GETSECRET,
        WRITEKV
    }

}

