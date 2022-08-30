package com.walmartlabs.concord.plugins.akeyless.model;

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

import com.walmartlabs.concord.plugins.akeyless.SecretExporter;
import com.walmartlabs.concord.plugins.akeyless.Util;
import com.walmartlabs.concord.runtime.v2.sdk.MapBackedVariables;
import com.walmartlabs.concord.runtime.v2.sdk.Variables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.BiFunction;

public class TaskParamsImpl implements TaskParams {
    private static final Logger log = LoggerFactory.getLogger(TaskParamsImpl.class);

    public static final String ACTION_KEY = "action";
    public static final String DEBUG_KEY = "debug";
    private static final String ENABLE_CONCORD_SECRETS_CACHE_KEY = "enableConcordSecretCache";
    private static final String SESSION_TOKEN_KEY = "sessionToken";
    private static final String TX_ID_KEY = "txId";
    private static final String AUTH_KEY = "auth";
    private static final String ACCESS_TOKEN = "accessToken";

    private static final Map<String, BiFunction<Variables, SecretExporter, Auth>> authBuilders = createAuthBuilders();
    private static SecretCache secretCache;
    private final SecretExporter secretExporter;

    public static TaskParams of(Map<String, Object> input,
                                Map<String, Object> defaults,
                                Map<String, Object> policyDefaults,
                                SecretExporter secretExporter) {

        Map<String, Object> mergedVars = new HashMap<>(policyDefaults != null ? policyDefaults : Collections.emptyMap());
        mergedVars.putAll(defaults);
        mergedVars.putAll(input);
        MapBackedVariables vars = new MapBackedVariables(mergedVars);

        TaskParams params;

        switch (TaskParamsImpl.action(vars)) {
            case AUTH: {
                params = new TaskParamsImpl(vars, secretExporter);
                break;
            }
            case GETSECRET: {
                params = new GetSecretParamsImpl(vars, secretExporter);
                break;
            }
            case GETSECRETS: {
                params = new GetSecretsParamsImpl(vars, secretExporter);
                break;
            }
            case CREATESECRET: {
                params = new CreateSecretParamsImpl(vars, secretExporter);
                break;
            }
            case UPDATESECRET: {
                params = new UpdateSecretParamsImpl(vars, secretExporter);
                break;
            }
            case DELETEITEM: {
                params = new DeleteItemParamsImpl(vars, secretExporter);
                break;
            }
            default:
                throw new IllegalArgumentException("Unsupported action type: " + action(vars));
        }

        if (params.enableConcordSecretCache()) {
            secretCache = SecretCacheImpl.getInstance(params.sessionId(), params.debug());
        } else {
            secretCache = SecretCacheNoop.getInstance();
        }

        return params;
    }

    private static Map<String, BiFunction<Variables, SecretExporter, Auth>> createAuthBuilders() {
        Map<String, BiFunction<Variables, SecretExporter, Auth>> result = new HashMap<>();
        result.put("apiKey", ApiKeyAuthImpl::of);
        return result;
    }

    final Variables input;

    protected TaskParamsImpl(Variables input, SecretExporter secretExporter) {
        this.input = input;
        this.secretExporter = secretExporter;
    }

    @Override
    public boolean enableConcordSecretCache() {
        return input.getBoolean(ENABLE_CONCORD_SECRETS_CACHE_KEY, TaskParams.super.enableConcordSecretCache());
    }

    @Override
    public String sessionId() {
        /* Session token should always exist in "real" processes since it's required
           to use the Secret API amongst other calls. The fallback here using
           process ID is to support Concord CLI which doesn't have API info. */
        String id = input.getString(SESSION_TOKEN_KEY);

        if (id == null) {
            log.warn("No session token found. Falling back to instance ID");
            id = this.txId();
        }

        return Util.hash(id);
    }

    @Override
    public String txId() {
        return input.assertString(TX_ID_KEY);
    }

    @Override
    public boolean debug() {
        return input.getBoolean(DEBUG_KEY, TaskParams.super.debug());
    }

    @Override
    public Action action() {
        return action(input);
    }

    /**
     * @param o Input value with may be a String or Map of Concord secret
     *          info (org, name, password)
     * @param secretExporter for access Concord's Secrets API
     * @return String value from direct input or exported Secret value
     */
    @SuppressWarnings("unchecked")
    private static String stringOrSecret(Object o, SecretExporter secretExporter) {
        if (o instanceof String) {
            return (String) o;
        }

        if (! (o instanceof Map)) {
            throw new IllegalArgumentException("Invalid data type given for sensitive argument. Must be string or map.");
        }

        ((Map<?, ?>) o).forEach((key, value) -> {
            if (!(key instanceof String)) {
                throw new IllegalArgumentException("Non-string key used for secret definition");
            }

            if (!(value instanceof String)) {
                throw new IllegalArgumentException("Non-string value used for secret definition");
            }
        });

        return exportSecret((Map<String, String>) o, secretExporter);
    }

    private static String exportSecret(Map<String, String> secretInfo, SecretExporter secretExporter) {
        final String o = secretInfo.get("org");
        final String n = secretInfo.get("name");
        final String p = secretInfo.getOrDefault("password", null);

        return secretCache.get(o + n, () -> {
            try {
                return secretExporter.exportAsString(o, n, p);
            } catch (Exception e) {
                throw new RuntimeException(String.format("Error exporting secret '%s/%s': %s", o, n, e.getMessage()), e);
            }
        });
    }

    @Override
    public Auth auth() {
        Map<String, Object> auth = input.assertMap(AUTH_KEY);
        if (auth.isEmpty()) {
            throw new IllegalArgumentException("Empty auth");
        }

        if (auth.size() != 1) {
            throw new IllegalArgumentException("Multiple auth types defined. Only one auth definition is allowed.");
        }

        String authType = auth.keySet().iterator().next();

        BiFunction<Variables, SecretExporter, Auth> builder = authBuilders.get(authType);
        if (builder == null) {
            throw new IllegalArgumentException("Unknown auth type '" + authType + "'. Available: " + authBuilders.keySet());
        }

        Map<String, Object> authTypeParams = new MapBackedVariables(auth).assertMap(authType);
        return builder.apply(new MapBackedVariables(authTypeParams), secretExporter);
    }

    @Override
    public String accessToken() {
        String fromInput = input.getString(ACCESS_TOKEN);

        return Objects.isNull(fromInput) ? null : stringOrSecret(fromInput, secretExporter);
    }

    private static class ApiKeyAuthImpl extends Auth {
        private static final String ACCESS_ID_KEY = "accessId";
        private static final String ACCESS_KEY_KEY = "accessKey";

        private static Auth of(Variables vars, SecretExporter secretExporter) {
            return new Auth()
                    .accessId(stringOrSecret(vars.get(ACCESS_ID_KEY), secretExporter))
                    .accessKey(stringOrSecret(vars.get(ACCESS_KEY_KEY), secretExporter));
        }
    }

    private static class GetSecretParamsImpl extends TaskParamsImpl implements GetSecretParams {
        private static final String PATH_KEY = "path";

        GetSecretParamsImpl(Variables input, SecretExporter secretExporter) {
            super(input, secretExporter);
        }

        @Override
        public String path() {
            return input.assertString(PATH_KEY);
        }
    }

    private static class GetSecretsParamsImpl extends TaskParamsImpl implements GetSecretsParams {
        private static final String PATHS_KEY = "paths";

        GetSecretsParamsImpl(Variables input, SecretExporter secretExporter) {
            super(input, secretExporter);
        }

        @Override
        public List<String> paths() {
            return input.assertList(PATHS_KEY);
        }
    }

    private static class CreateSecretParamsImpl extends TaskParamsImpl implements CreateSecretParams {
        private static final String PATH_KEY = "path";
        private static final String VALUE_KEY = "value";
        private static final String DESCRIPTION_KEY = "description";
        private static final String MULTILINE_KEY = "multiline";
        private static final String TAGS_KEY = "tags";
        private static final String PROTECTION_KEY_KEY = "protectionKey";

        CreateSecretParamsImpl(Variables input, SecretExporter secretExporter) {
            super(input, secretExporter);
        }

        @Override
        public String path() {
            return input.assertString(PATH_KEY);
        }

        @Override
        public String value() {
            return input.assertString(VALUE_KEY);
        }

        @Override
        public String description() {
            return input.assertString(DESCRIPTION_KEY);
        }

        @Override
        public boolean multiline() {
            return input.assertBoolean(MULTILINE_KEY);
        }

        @Override
        public List<String> tags() {
            return input.getList(TAGS_KEY, CreateSecretParams.super.tags());
        }

        @Override
        public String protectionKey() {
            return input.getString(PROTECTION_KEY_KEY);
        }
    }

    private static class UpdateSecretParamsImpl extends TaskParamsImpl implements UpdateSecretParams {
        private static final String PATH_KEY = "path";
        private static final String VALUE_KEY = "value";
        private static final String PROTECTION_KEY_KEY = "protectionKey";
        private static final String MULTILINE_KEY = "multiline";
        private static final String KEEP_PREVIOUS_VERSION_KEY = "keepPreviousVersion";

        UpdateSecretParamsImpl(Variables input, SecretExporter secretExporter) {
            super(input, secretExporter);
        }

        @Override
        public String path() {
            return input.assertString(PATH_KEY);
        }

        @Override
        public String value() {
            return input.assertString(VALUE_KEY);
        }

        @Override
        public String protectionKey() {
            return input.getString(PROTECTION_KEY_KEY);
        }

        @Override
        public Boolean multiline() {
            return input.get(MULTILINE_KEY, null, Boolean.class);
        }

        @Override
        public boolean keepPreviousVersion() {
            return input.getBoolean(KEEP_PREVIOUS_VERSION_KEY, UpdateSecretParams.super.keepPreviousVersion());
        }
    }

    private static class DeleteItemParamsImpl extends TaskParamsImpl implements DeleteItemParams {
        private static final String PATH_KEY = "path";
        private static final String VERSION_KEY = "version";
        private static final String DELETE_IMMEDIATELY_KEY = "deleteImmediately";
        private static final String DELETE_IN_DAYS_KEY = "deleteInDays";

        DeleteItemParamsImpl(Variables input, SecretExporter secretExporter) {
            super(input, secretExporter);
        }

        @Override
        public String path() {
            return input.assertString(PATH_KEY);
        }

        @Override
        public Integer version() {
            return input.get(VERSION_KEY, null, Integer.class);
        }

        @Override
        public boolean deleteImmediately() {
            return input.getBoolean(DELETE_IMMEDIATELY_KEY, true);
        }

        @Override
        public Long deleteInDays() {
            if (deleteImmediately()) {
                return 0L;
            }

            return input.assertLong(DELETE_IN_DAYS_KEY);
        }
    }

    private static Action action(Variables variables) {
        String action = variables.getString(ACTION_KEY, Action.GETSECRET.name());
        try {
            return Action.valueOf(action.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown action: '" + action + "'. Available actions: " + Arrays.toString(Action.values()));
        }
    }
}
