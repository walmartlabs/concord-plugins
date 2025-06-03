package com.walmartlabs.concord.plugins.hashivault;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2021 Walmart Inc., Concord Authors
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

    private static final String DEFAULT_NAMESPACE = null;
    private static final int DEFAULT_ENGINE_VERSION = 2;
    private static final boolean DEFAULT_VERIFY_SSL = true;
    private static final long DEFAULT_RETRY_COUNT = 3;
    private static final long DEFAULT_RETRY_INTERVAL_MS = 5_000;

    public static final String DEFAULT_PARAMS_KEY = "hashivaultParams";
    public static final String TX_ID_KEY = "txId";

    public static final String ACTION_KEY = "action";
    public static final String API_TOKEN_KEY = "apiToken";
    public static final String API_TOKEN_SECRET_KEY = "apiTokenSecret";
    public static final String ORG_KEY = "org";
    public static final String NAME_KEY = "name";
    public static final String PASSWORD_KEY = "password";
    public static final String BASE_URL_KEY = "baseUrl";
    public static final String NAMESPACE_KEY = "namespace";
    public static final String VERIFY_SSL_KEY = "verifySsl";
    public static final String ENGINE_VERSION_KEY = "engineVersion";
    public static final String PATH_KEY = "path";
    public static final String KEY_KEY = "key";
    public static final String KV_PAIRS_KEY = "kvPairs";
    public static final String RETRY_COUNT_KEY = "retryCount";
    public static final String RETRY_INTERVAL_MS_KEY = "retryIntervalMs";
    public static final String DRY_RUN_KEY = "dryRun";

    protected final Variables variables;

    private TaskParams(Variables variables) {
        this.variables = variables;
    }

    public static TaskParams of(Variables input,
                                Map<String, Object> defaults,
                                SecretExporter secretExporter,
                                boolean dryRunMode) {

        Map<String, Object> variablesMap = new HashMap<>(defaults != null ? defaults : Collections.emptyMap());
        variablesMap.putAll(input.toMap());
        variablesMap.putIfAbsent(DRY_RUN_KEY, dryRunMode);

        if (variablesMap.containsKey(API_TOKEN_SECRET_KEY)) {
            Map<String, Object> tokenSecret = MapUtils.assertMap(variablesMap, API_TOKEN_SECRET_KEY);
            variablesMap.put(API_TOKEN_KEY, exportToken(secretExporter, tokenSecret));
        }

        Variables variables = new MapBackedVariables(variablesMap);
        TaskParams p = new TaskParams(variables);

        return switch (p.action()) {
            case READKV, WRITEKV -> new TaskParams(variables);
        };
    }

    public static TaskParams of(Variables input, Map<String, Object> defaults, SecretExporter secretExporter) {
        return of(input, defaults, secretExporter, false);
    }

    private static String exportToken(SecretExporter secretExporter, Map<String, Object> secret) {
        String o = MapUtils.assertString(secret, ORG_KEY);
        String n = MapUtils.assertString(secret, NAME_KEY);
        String p = MapUtils.getString(secret, PASSWORD_KEY);

        try {
            return secretExporter.exportAsString(o, n, p);
        } catch (Exception e) {
            throw new HashiVaultTaskException("Error retrieving API token from Concord secret: " + e.getMessage());
        }
    }

    public Action action() {
        String action = variables.getString(ACTION_KEY, Action.READKV.name());
        try {
            return Action.valueOf(action.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unsupported action type: " + action);
        }
    }

    public String apiToken() {
        return variables.assertString(API_TOKEN_KEY);
    }

    public String baseUrl() {
        String url = variables.assertString(BASE_URL_KEY);
        if (url.endsWith("/")) {
            url = url.substring(0, url.lastIndexOf('/'));
        }
        return url;
    }

    public String ns() {
        return variables.getString(NAMESPACE_KEY, DEFAULT_NAMESPACE);
    }

    public boolean verifySsl() {
        return variables.getBoolean(VERIFY_SSL_KEY, DEFAULT_VERIFY_SSL);
    }

    public int retryCount() {
        return variables.getNumber(RETRY_COUNT_KEY, DEFAULT_RETRY_COUNT).intValue();
    }

    public int retryIntervalMs() {
        return variables.getNumber(RETRY_INTERVAL_MS_KEY, DEFAULT_RETRY_INTERVAL_MS).intValue();
    }

    public int engineVersion() {
        if (path().matches("^/?cubbyhole.*")) {
            return 1;
        }

        return variables.getInt(ENGINE_VERSION_KEY, DEFAULT_ENGINE_VERSION);
    }

    public String path() {
        return variables.assertString(PATH_KEY);
    }

    public boolean hasKeyField() {
        return variables.has(KEY_KEY);
    }

    public String key() {
        return variables.getString(KEY_KEY);
    }

    public Map<String, Object> kvPairs() {
        if (action() == Action.WRITEKV) {
            return variables.assertMap(KV_PAIRS_KEY);
        }

        return variables.getMap(KV_PAIRS_KEY, Collections.emptyMap());
    }

    public boolean dryRun() {
        return variables.getBoolean(DRY_RUN_KEY, false);
    }

    public enum Action {
        READKV,
        WRITEKV
    }

    public interface SecretExporter {
        String exportAsString(String o, String n, String p) throws Exception;
    }
}
