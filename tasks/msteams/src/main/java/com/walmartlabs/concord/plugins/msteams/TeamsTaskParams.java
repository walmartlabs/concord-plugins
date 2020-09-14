package com.walmartlabs.concord.plugins.msteams;

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

import com.walmartlabs.concord.runtime.v2.sdk.Variables;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class TeamsTaskParams implements TeamsConfiguration {

    public static TeamsTaskParams of(Variables input, Map<String, Object> defaults) {
        Variables variables = Utils.merge(input, defaults);

        Action action = new TeamsTaskParams(variables).action();
        switch (action) {
            case SENDMESSAGE: {
                return new SendMessageParams(variables);
            }
            default:
                throw new IllegalArgumentException("Unsupported action type: " + action);
        }
    }

    private static final String ACTION_KEY = "action";
    private static final String TEAM_ID_KEY = "teamId";
    private static final String TENANT_ID_KEY = "tenantId";
    private static final String WEBHOOKTYPE_ID_KEY = "webhookTypeId";
    private static final String WEBHOOK_ID_KEY = "webhookId";
    private static final String WEBHOOK_URL_KEY = "webhookUrl";
    private static final String ROOT_WEBHOOK_URL_KEY = "rootWebhookUrl";
    private static final String PROXY_ADDRESS_KEY = "proxyAddress";
    private static final String PROXY_PORT_KEY = "proxyPort";
    private static final String CONNECTION_TIMEOUT_KEY = "connectTimeout";
    private static final String SO_TIMEOUT_KEY = "soTimeout";
    private static final String RETRY_COUNT_KEY = "retryCount";

    protected final Variables variables;

    public TeamsTaskParams(Variables variables) {
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
    public String teamId() {
        return variables.getString(TEAM_ID_KEY);
    }

    @Override
    public String tenantId() {
        return variables.getString(TENANT_ID_KEY);
    }

    @Override
    public String webhookTypeId() {
        return variables.getString(WEBHOOKTYPE_ID_KEY);
    }

    @Override
    public String webhookId() {
        return variables.getString(WEBHOOK_ID_KEY);
    }

    @Override
    public String webhookUrl() {
        return variables.getString(WEBHOOK_URL_KEY);
    }

    @Override
    public String rootWebhookUrl() {
        return variables.getString(ROOT_WEBHOOK_URL_KEY);
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

    public static class SendMessageParams extends TeamsTaskParams {

        private static final String MESSAGE_TITLE_KEY = "title";
        private static final String MESSAGE_TEXT_KEY = "text";
        private static final String MESSAGE_THEME_COLOR_KEY = "themeColor";
        private static final String MESSAGE_SECTIONS_KEY = "sections";
        private static final String MESSAGE_POTENTIAL_ACTION_KEY = "potentialAction";
        private static final String IGNORE_ERRORS_KEY = "ignoreErrors";

        public SendMessageParams(Variables variables) {
            super(variables);
        }

        public String title() {
            return variables.getString(MESSAGE_TITLE_KEY);
        }

        public String text() {
            return variables.assertString(MESSAGE_TEXT_KEY);
        }

        public String themeColor() {
            return variables.getString(MESSAGE_THEME_COLOR_KEY, Constants.DEFAULT_THEME_COLOR);
        }

        public List<Object> sections() {
            return variables.getList(MESSAGE_SECTIONS_KEY, null);
        }
        public List<Object> potentialAction() {
            return variables.getList(MESSAGE_POTENTIAL_ACTION_KEY, null);
        }
        public boolean ignoreErrors() {
            return variables.getBoolean(IGNORE_ERRORS_KEY, false);
        }
    }

    public enum Action {
        SENDMESSAGE
    }
}
