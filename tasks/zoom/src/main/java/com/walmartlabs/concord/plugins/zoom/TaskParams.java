package com.walmartlabs.concord.plugins.zoom;

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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class TaskParams implements ZoomConfiguration {

    public static TaskParams of(Variables input, Map<String, Object> defaults) {
        Variables variables = merge(input, defaults);

        Action action = new TaskParams(variables).action();
        switch (action) {
            case SENDMESSAGE: {
                return new SendMessageParams(variables);
            }
            default:
                throw new IllegalArgumentException("Unsupported action type: " + action);
        }
    }

    private static final String ACTION_KEY = "action";

    public static final String VAR_CLIENT_ID = "clientId";
    public static final String VAR_CLIENT_SECRET = "clientSecret";
    public static final String VAR_ACCOUNT_ID = "accountId";
    public static final String VAR_ROBOT_ID = "robotJid";
    public static final String VAR_ROOT_API = "rootApi";

    public static final String VAR_ACCESS_TOKEN_API = "accessTokenApi";

    public static final String VAR_PROXY_ADDRESS = "proxyAddress";
    public static final String VAR_PROXY_PORT = "proxyPort";
    public static final String VAR_CONNECTION_TIMEOUT = "connectTimeout";
    public static final String VAR_SO_TIMEOUT = "soTimeout";
    public static final String VAR_RETRY_COUNT = "retryCount";

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

    @Override
    public String clientId() {
        return variables.getString(VAR_CLIENT_ID);
    }

    @Override
    public String clientSecret() {
        return variables.getString(VAR_CLIENT_SECRET);
    }

    @Override
    public String accountId() {
        return variables.getString(VAR_ACCOUNT_ID);
    }

    @Override
    public String robotJid() {
        return variables.getString(VAR_ROBOT_ID);
    }

    @Override
    public String rootApi() {
        return variables.getString(VAR_ROOT_API);
    }

    @Override
    public String accessTokenApi() {
        return variables.getString(VAR_ACCESS_TOKEN_API);
    }

    @Override
    public String proxyAddress() {
        return variables.getString(VAR_PROXY_ADDRESS);
    }

    @Override
    public int proxyPort() {
        return variables.getInt(VAR_PROXY_PORT, Constants.DEFAULT_PROXY_PORT);
    }

    @Override
    public int connectTimeout() {
        return variables.getInt(VAR_CONNECTION_TIMEOUT, Constants.DEFAULT_CONNECT_TIMEOUT);
    }

    @Override
    public int soTimeout() {
        return variables.getInt(VAR_SO_TIMEOUT, Constants.DEFAULT_SO_TIMEOUT);
    }

    @Override
    public int retryCount() {
        return variables.getInt(VAR_RETRY_COUNT, Constants.DEFAULT_RETRY_COUNT);
    }

    public static class SendMessageParams extends TaskParams {

        private static final String ZOOM_CHANNEL_JID = "channelId";
        private static final String ZOOM_HEAD_TEXT = "headText";
        private static final String ZOOM_BODY_TEXT = "bodyText";
        private static final String IGNORE_ERRORS_KEY = "ignoreErrors";

        public SendMessageParams(Variables variables) {
            super(variables);
        }

        public String channelId() {
            return variables.assertString(ZOOM_CHANNEL_JID);
        }

        public String headText() {
            return variables.assertString(ZOOM_HEAD_TEXT);
        }

        public String bodyText() {
            return variables.getString(ZOOM_BODY_TEXT);
        }

        public boolean ignoreErrors() {
            return variables.getBoolean(IGNORE_ERRORS_KEY, false);
        }

    }

    private static Variables merge(Variables variables, Map<String, Object> defaults) {
        Map<String, Object> variablesMap = new HashMap<>(defaults != null ? defaults : Collections.emptyMap());
        variablesMap.putAll(variables.toMap());
        return new MapBackedVariables(variablesMap);
    }

    public enum Action {
        SENDMESSAGE
    }
}
