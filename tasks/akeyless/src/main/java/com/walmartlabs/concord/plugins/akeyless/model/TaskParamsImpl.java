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
import com.walmartlabs.concord.runtime.v2.sdk.MapBackedVariables;
import com.walmartlabs.concord.runtime.v2.sdk.Variables;

import java.util.*;
import java.util.function.Function;

public class TaskParamsImpl implements TaskParams {

    public static final String ACTION_KEY = "action";
    private static final String AUTH_KEY = "auth";

    private static final Map<String, Function<Variables, Auth>> authBuilders = createAuthBuilders();
    private final SecretExporter secretExporter;

    public static TaskParams of(Map<String, Object> input,
                                Map<String, Object> defaults,
                                Map<String, Object> policyDefaults,
                                SecretExporter secretExporter) {
        Map<String, Object> mergedVars = new HashMap<>(policyDefaults != null ? policyDefaults : Collections.emptyMap());
        mergedVars.putAll(defaults);
        mergedVars.putAll(input);
        MapBackedVariables vars = new MapBackedVariables(mergedVars);

        switch (TaskParamsImpl.action(vars)) {
            case GETSECRET:
                return new GetSecretParamsImpl(vars, secretExporter);

            case GETSECRETS:
                return new GetSecretsParamsImpl(vars, secretExporter);

            case CREATESECRET:
                return new CreateSecretParamsImpl(vars, secretExporter);

            case UPDATESECRET:
                return new UpdateSecretParamsImpl(vars, secretExporter);

            default:
                throw new IllegalArgumentException("Unsupported action type: " + action(vars));
        }
    }

    private static Map<String, Function<Variables, Auth>> createAuthBuilders() {
        Map<String, Function<Variables, Auth>> result = new HashMap<>();
        result.put("apiKey", ApiKeyAuthImpl::of);
        return result;
    }


    final Variables input;

    protected TaskParamsImpl(Variables input, SecretExporter secretExporter) {
        this.input = input;
        this.secretExporter = secretExporter;
    }

    @Override
    public Action action() {
        return action(input);
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

        Function<Variables, Auth> builder = authBuilders.get(authType);
        if (builder == null) {
            throw new IllegalArgumentException("Unknown auth type '" + authType + "'. Available: " + authBuilders.keySet());
        }

        Map<String, Object> authTypeParams = new MapBackedVariables(auth).assertMap(authType);
        return builder.apply(new MapBackedVariables(authTypeParams));
    }

    private static class ApiKeyAuthImpl extends Auth {
        private static final String ACCESS_ID_KEY = "accessId";
        private static final String ACCESS_KEY_KEY = "accessKey";


        private static Auth of(Variables vars) {
            Auth auth = new Auth();
            auth.setAccessId(vars.assertString(ACCESS_ID_KEY));
            auth.setAccessKey(vars.assertString(ACCESS_KEY_KEY));
            return auth;

        }
    }

    private static class GetSecretParamsImpl extends TaskParamsImpl implements GetSecretParams {
        private static final String PATH_KEY = "secretPath";

        GetSecretParamsImpl(Variables input, SecretExporter secretExporter) {
            super(input, secretExporter);
        }

        @Override
        public String path() {
            return input.assertString(PATH_KEY);
        }
    }

    private static class GetSecretsParamsImpl extends TaskParamsImpl implements GetSecretsParams {
        private static final String PATHS_KEY = "secretPaths";

        GetSecretsParamsImpl(Variables input, SecretExporter secretExporter) {
            super(input, secretExporter);
        }

        @Override
        public List<String> paths() {
            return input.assertList(PATHS_KEY);
        }
    }

    private static class CreateSecretParamsImpl extends TaskParamsImpl implements CreateSecretParams {
        private static final String PATH_KEY = "secretPath";
        private static final String VALUE_KEY = "value";
        private static final String DESCRIPTION_KEY = "description";
        public static final String MULTILINE_KEY = "multiline";
        private static final String TAGS_KEY = "tags";
        public static final String PROTECTION_KEY_KEY = "protectionKey";

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
        private static final String PATH_KEY = "secretPath";
        private static final String VALUE_KEY = "value";
        public static final String PROTECTION_KEY_KEY = "protectionKey";
        public static final String MULTILINE_KEY = "multiline";
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

    private static Action action(Variables variables) {
        String action = variables.getString(ACTION_KEY, Action.GETSECRET.name());
        try {
            return Action.valueOf(action.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown action: '" + action + "'. Available actions: " + Arrays.toString(Action.values()));
        }
    }
}
