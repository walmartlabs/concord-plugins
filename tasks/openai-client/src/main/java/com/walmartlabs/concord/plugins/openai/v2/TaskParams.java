package com.walmartlabs.concord.plugins.openai.v2;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2026 Walmart Inc., Concord Authors
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

import java.util.List;
import java.util.Map;

public class TaskParams {

    private static final String AUTH_KEY = "auth";
    private static final String TYPE_KEY = "type";
    private static final String SECRET_REF_KEY = "secretRef";
    private static final String ORG_NAME_KEY = "orgName";
    private static final String SECRET_NAME_KEY = "secretName";

    private static final String MODEL_KEY = "model";
    private static final String MESSAGES_KEY = "messages";
    private static final String BASE_URL_KEY = "baseUrl";
    private static final String TOOLS_KEY = "tools";
    private static final String TOOL_CHOICE_KEY = "toolChoice";
    private static final String MAX_TOKENS_KEY = "maxTokens";
    private static final String TEMPERATURE_KEY = "temperature";

    private static final String DEFAULT_BASE_URL = "https://api.openai.com/v1";

    private final String orgName;
    private final String secretName;
    private final String model;
    private final List<Object> messages;
    private final String baseUrl;
    private final List<Object> tools;
    private final Object toolChoice;
    private final Integer maxTokens;
    private final Double temperature;

    public TaskParams(Variables input) {
        var auth = input.assertMap(AUTH_KEY);
        var authType = (String) auth.get(TYPE_KEY);
        if (!"bearer".equals(authType)) {
            throw new IllegalArgumentException("Unsupported auth type: '%s'. Only 'bearer' is supported".formatted(authType));
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> secretRef = (Map<String, Object>) auth.get(SECRET_REF_KEY);
        if (secretRef == null) {
            throw new IllegalArgumentException("'auth.secretRef' is required");
        }
        this.orgName = (String) secretRef.get(ORG_NAME_KEY);
        if (orgName == null) {
            throw new IllegalArgumentException("'auth.secretRef.orgName' is required");
        }
        this.secretName = (String) secretRef.get(SECRET_NAME_KEY);
        if (secretName == null) {
            throw new IllegalArgumentException("'auth.secretRef.secretName' is required");
        }

        this.model = input.assertString(MODEL_KEY);

        @SuppressWarnings("unchecked")
        List<Object> messagesRaw = (List<Object>) input.assertList(MESSAGES_KEY);
        if (messagesRaw.isEmpty()) {
            throw new IllegalArgumentException("'messages' must not be empty");
        }
        this.messages = messagesRaw;

        this.baseUrl = input.getString(BASE_URL_KEY, DEFAULT_BASE_URL);

        @SuppressWarnings("unchecked")
        List<Object> toolsRaw = (List<Object>) input.getList(TOOLS_KEY, null);
        this.tools = toolsRaw != null ? toolsRaw : List.of();

        this.toolChoice = input.get(TOOL_CHOICE_KEY);
        var maxTokensNumber = input.getNumber(MAX_TOKENS_KEY, null);
        this.maxTokens = maxTokensNumber != null ? maxTokensNumber.intValue() : null;

        var temperatureNumber = input.getNumber(TEMPERATURE_KEY, null);
        this.temperature = temperatureNumber != null ? temperatureNumber.doubleValue() : null;
    }

    public String orgName() {
        return orgName;
    }

    public String secretName() {
        return secretName;
    }

    public String model() {
        return model;
    }

    public List<Object> messages() {
        return messages;
    }

    public String baseUrl() {
        return baseUrl;
    }

    public List<Object> tools() {
        return tools;
    }

    public Object toolChoice() {
        return toolChoice;
    }

    public Integer maxTokens() {
        return maxTokens;
    }

    public Double temperature() {
        return temperature;
    }
}
