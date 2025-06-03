package com.walmartlabs.concord.plugins.msteams;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2018 Walmart Inc.
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

import com.walmartlabs.concord.runtime.v2.sdk.Variables;

import java.util.Arrays;
import java.util.Map;

public class TeamsV2TaskParams implements TeamsV2Configuration {

    public static TeamsV2TaskParams of(Variables input, Map<String, Object> defaults) {
        Variables variables = Utils.merge(input, defaults);

        Action action = new TeamsV2TaskParams(variables).action();
        switch (action) {
            case CREATECONVERSATION: {
                return new CreateConversationParams(variables);
            }
            case REPLYTOCONVERSATION: {
                return new ReplayToConversationParams(variables);
            }
            default:
                throw new IllegalArgumentException("Unsupported action type: " + action);
        }
    }

    private static final String ACTION_KEY = "action";
    private static final String VAR_ACTIVITY = "activity";
    private static final String TENANT_ID_KEY = "tenantId";
    private static final String VAR_ACCESS_TOKEN_API = "accessTokenApi";
    private static final String VAR_CLIENT_ID = "clientId";
    private static final String VAR_CLIENT_SECRET = "clientSecret";
    private static final String VAR_ROOT_API = "rootApi";
    private static final String PROXY_ADDRESS_KEY = "proxyAddress";
    private static final String PROXY_PORT_KEY = "proxyPort";
    private static final String USE_PROXY_KEY = "useProxy";
    private static final String CONNECTION_TIMEOUT_KEY = "connectTimeout";
    private static final String SO_TIMEOUT_KEY = "soTimeout";
    private static final String RETRY_COUNT_KEY = "retryCount";

    protected final Variables variables;

    public TeamsV2TaskParams(Variables variables) {
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

    public Map<String, Object> activity() {
        return variables.assertMap(VAR_ACTIVITY);
    }

    @Override
    public String tenantId() {
        return variables.getString(TENANT_ID_KEY);
    }

    @Override
    public String accessTokenApi() {
        return variables.getString(VAR_ACCESS_TOKEN_API);
    }

    @Override
    public String clientId() {
        return variables.getString(VAR_CLIENT_ID);
    }

    @Override
    public String clientSecret() {
        return variables.getString(VAR_CLIENT_SECRET);
    }

    @Override
    public String rootApi() {
        return variables.getString(VAR_ROOT_API);
    }

    @Override
    public String proxyAddress() {
        return variables.getString(PROXY_ADDRESS_KEY);
    }

    @Override
    public int proxyPort() {
        return variables.getInt(PROXY_PORT_KEY, Constants.DEFAULT_PROXY_PORT);
    }

    @Override
    public boolean useProxy() {
        return variables.getBoolean(USE_PROXY_KEY, false);
    }

    @Override
    public int connectTimeout() {
        return variables.getInt(CONNECTION_TIMEOUT_KEY, Constants.DEFAULT_CONNECT_TIMEOUT);
    }

    @Override
    public int soTimeout() {
        return variables.getInt(SO_TIMEOUT_KEY, Constants.DEFAULT_SO_TIMEOUT);
    }

    @Override
    public int retryCount() {
        return variables.getInt(RETRY_COUNT_KEY, Constants.DEFAULT_RETRY_COUNT);
    }

    public static class CreateConversationParams extends TeamsV2TaskParams {

        private static final String VAR_CHANNEL_ID = "channelId";
        private static final String IGNORE_ERRORS_KEY = "ignoreErrors";

        public CreateConversationParams(Variables variables) {
            super(variables);
        }

        public String channelId() {
            return variables.assertString(VAR_CHANNEL_ID);
        }

        public boolean ignoreErrors() {
            return variables.getBoolean(IGNORE_ERRORS_KEY, false);
        }
    }

    public static class ReplayToConversationParams extends TeamsV2TaskParams {

        private static final String VAR_CONVERSATION_ID = "conversationId";
        private static final String IGNORE_ERRORS_KEY = "ignoreErrors";

        public ReplayToConversationParams(Variables variables) {
            super(variables);
        }

        public boolean ignoreErrors() {
            return variables.getBoolean(IGNORE_ERRORS_KEY, false);
        }

        public String conversationId() {
            return variables.assertString(VAR_CONVERSATION_ID);
        }
    }

    public enum Action {
        CREATECONVERSATION,
        REPLYTOCONVERSATION
    }
}
