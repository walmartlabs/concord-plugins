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

import com.walmartlabs.concord.plugins.akeyless.Util;
import com.walmartlabs.concord.runtime.v2.sdk.MapBackedVariables;
import com.walmartlabs.concord.runtime.v2.sdk.Variables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class TaskParamsImpl implements TaskParams {
    private static final Logger log = LoggerFactory.getLogger(TaskParamsImpl.class);

    public static final String ACTION_KEY = "action";
    public static final String API_BASE_PATH = "apiBasePath";
    public static final String DEBUG_KEY = "debug";
    private static final String ENABLE_CONCORD_SECRETS_CACHE_KEY = "enableConcordSecretCache";
    private static final String SESSION_TOKEN_KEY = "sessionToken";
    private static final String TX_ID_KEY = "txId";
    private static final String AUTH_KEY = "auth";
    private static final String ACCESS_TOKEN = "accessToken";
    private static final String IGNORE_CACHE_KEY = "ignoreCache";

    public static TaskParams of(Map<String, Object> input,
                                Map<String, Object> defaults,
                                Map<String, Object> policyDefaults) {

        Map<String, Object> mergedVars = new HashMap<>(policyDefaults != null ? policyDefaults : Collections.emptyMap());
        mergedVars.putAll(defaults);
        mergedVars.putAll(input);
        MapBackedVariables vars = new MapBackedVariables(mergedVars);

        TaskParams params;

        switch (TaskParamsImpl.action(vars)) {
            case AUTH: {
                params = new TaskParamsImpl(vars);
                break;
            }
            case GETSECRET: {
                params = new GetSecretParamsImpl(vars);
                break;
            }
            case GETSECRETS: {
                params = new GetSecretsParamsImpl(vars);
                break;
            }
            case CREATESECRET: {
                params = new CreateSecretParamsImpl(vars);
                break;
            }
            case UPDATESECRET: {
                params = new UpdateSecretParamsImpl(vars);
                break;
            }
            case DELETEITEM: {
                params = new DeleteItemParamsImpl(vars);
                break;
            }
            default:
                throw new IllegalArgumentException("Unsupported action type: " + action(vars));
        }

        return params;
    }

    final Variables input;

    protected TaskParamsImpl(Variables input) {
        this.input = input;
    }

    @Override
    public String apiBasePath() {
        return input.getString(API_BASE_PATH, TaskParams.super.apiBasePath());
    }

    @Override
    public boolean ignoreCache() {
        boolean ignore = input.getBoolean(IGNORE_CACHE_KEY, TaskParams.super.ignoreCache());

        if (ignore) {
            Util.debug(this.debug(), log, "ignoring cache");
        }

        return ignore;
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

    @Override
    public Object accessToken() {
        return input.get(ACCESS_TOKEN);
    }

    @Override
    public Map<String, Object> auth() {
        return input.assertMap(AUTH_KEY);
    }

    private static class GetSecretParamsImpl extends TaskParamsImpl implements GetSecretParams {
        private static final String PATH_KEY = "path";

        GetSecretParamsImpl(Variables input) {
            super(input);
        }

        @Override
        public String path() {
            return input.assertString(PATH_KEY);
        }
    }

    private static class GetSecretsParamsImpl extends TaskParamsImpl implements GetSecretsParams {
        private static final String PATHS_KEY = "paths";

        GetSecretsParamsImpl(Variables input) {
            super(input);
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

        CreateSecretParamsImpl(Variables input) {
            super(input);
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

        UpdateSecretParamsImpl(Variables input) {
            super(input);
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

        DeleteItemParamsImpl(Variables input) {
            super(input);
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
