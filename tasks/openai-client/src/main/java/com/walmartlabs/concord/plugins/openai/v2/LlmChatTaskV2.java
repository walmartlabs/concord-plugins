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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.walmartlabs.concord.runtime.v2.sdk.*;

import javax.inject.Inject;
import javax.inject.Named;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Named("llmChat")
@DryRunReady
public class LlmChatTaskV2 implements Task {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);

    private final SecretService secretService;
    private final HttpClient httpClient;

    @Inject
    public LlmChatTaskV2(SecretService secretService) {
        this.secretService = secretService;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(REQUEST_TIMEOUT)
                .build();
    }

    @Override
    public TaskResult execute(Variables input) {
        TaskParams params;
        try {
            params = new TaskParams(input);
        } catch (IllegalArgumentException e) {
            return TaskResult.error(e.getMessage());
        }

        String apiToken;
        try {
            apiToken = secretService.exportAsString(params.orgName(), params.secretName(), null);
        } catch (Exception e) {
            return TaskResult.error("Failed to resolve secret '%s/%s': %s".formatted(params.orgName(), params.secretName(), e.getMessage()));
        }

        Map<String, Object> requestBody;
        try {
            requestBody = buildRequestBody(params);
        } catch (Exception e) {
            return TaskResult.error("Failed to build request body: " + e.getMessage());
        }

        var request = HttpRequest.newBuilder()
                .uri(URI.create(params.baseUrl() + "/chat/completions"))
                .header("Authorization", "Bearer " + apiToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(toJson(requestBody)))
                .timeout(REQUEST_TIMEOUT)
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            return TaskResult.error("HTTP request failed: " + e.getMessage());
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            var errorMsg = extractErrorMessage(response);
            return TaskResult.error("API error %d: %s".formatted(response.statusCode(), errorMsg));
        }

        return parseSuccessResponse(response.body());
    }

    private static Map<String, Object> buildRequestBody(TaskParams params) {
        var body = new LinkedHashMap<String, Object>();
        body.put("model", params.model());
        body.put("messages", params.messages());

        if (!params.tools().isEmpty()) {
            body.put("tools", params.tools());
        }
        if (params.toolChoice() != null) {
            body.put("tool_choice", params.toolChoice());
        }
        if (params.maxTokens() != null) {
            body.put("max_tokens", params.maxTokens());
        }
        if (params.temperature() != null) {
            body.put("temperature", params.temperature());
        }

        return body;
    }

    @SuppressWarnings("unchecked")
    private static TaskResult parseSuccessResponse(String responseBody) {
        Map<String, Object> response;
        try {
            response = objectMapper.readValue(responseBody, MAP_TYPE);
        } catch (Exception e) {
            return TaskResult.error("Failed to parse API response: " + e.getMessage());
        }

        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        if (choices == null || choices.isEmpty()) {
            return TaskResult.error("API response missing 'choices'");
        }

        var choice = choices.get(0);
        var message = (Map<String, Object>) choice.get("message");
        if (message == null) {
            return TaskResult.error("API response missing 'choices[0].message'");
        }

        var content = message.get("content");
        String contentStr = content instanceof String s ? s : null;

        var toolCalls = (List<Object>) message.get("tool_calls");
        List<Object> mappedToolCalls = null;
        if (toolCalls != null && !toolCalls.isEmpty()) {
            mappedToolCalls = toolCalls;
        }

        var finishReason = (String) choice.get("finish_reason");

        var model = (String) response.get("model");

        var usage = (Map<String, Object>) response.get("usage");

        var result = TaskResult.success()
                .value("content", contentStr)
                .value("finishReason", finishReason);

        if (mappedToolCalls != null) {
            result.value("toolCalls", mappedToolCalls);
        }
        if (model != null) {
            result.value("model", model);
        }
        if (usage != null) {
            var mappedUsage = usage.entrySet().stream()
                    .collect(LinkedHashMap::new, (m, e) -> m.put(snakeToCamel(e.getKey()), e.getValue()), LinkedHashMap::putAll);
            result.value("usage", mappedUsage);
        }

        return result;
    }

    private static String extractErrorMessage(HttpResponse<String> response) {
        try {
            var body = objectMapper.readValue(response.body(), MAP_TYPE);
            var error = (Map<String, Object>) body.get("error");
            if (error != null) {
                var message = (String) error.get("message");
                if (message != null) {
                    return message;
                }
            }
        } catch (Exception e) {
            // fall through
        }
        return response.body();
    }

    private static String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new RuntimeException("JSON serialization failed", e);
        }
    }

    private static String snakeToCamel(String s) {
        var sb = new StringBuilder(s.length());
        var upper = false;
        for (int i = 0; i < s.length(); i++) {
            var c = s.charAt(i);
            if (c == '_') {
                if (!upper) {
                    upper = true;
                }
            } else {
                sb.append(upper ? Character.toUpperCase(c) : c);
                upper = false;
            }
        }
        return sb.toString();
    }
}
