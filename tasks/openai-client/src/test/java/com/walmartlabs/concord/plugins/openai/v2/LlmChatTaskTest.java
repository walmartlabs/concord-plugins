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

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.walmartlabs.concord.runtime.v2.sdk.MapBackedVariables;
import com.walmartlabs.concord.runtime.v2.sdk.SecretService;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LlmChatTaskTest {

    private static final String ORG_NAME = "Default";
    private static final String SECRET_NAME = "test-api-key";
    private static final String API_KEY = "sk-test-12345";

    @RegisterExtension
    static WireMockExtension httpMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @Mock
    SecretService secretService;

    private Map<String, Object> basicInput() {
        return Map.of(
                "auth", Map.of(
                        "type", "bearer",
                        "secretRef", Map.of(
                                "orgName", ORG_NAME,
                                "secretName", SECRET_NAME)),
                "model", "gpt-4o",
                "baseUrl", httpMock.baseUrl(),
                "messages", List.of(
                        Map.of("role", "user", "content", "Hello"))
        );
    }

    @Test
    void testSimpleChat() throws Exception {
        configureFor(httpMock.getPort());
        when(secretService.exportAsString(ORG_NAME, SECRET_NAME, null)).thenReturn(API_KEY);

        stubFor(post(urlEqualTo("/chat/completions"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "id": "chatcmpl-123",
                                    "object": "chat.completion",
                                    "created": 1677652288,
                                    "model": "gpt-4o",
                                    "choices": [{
                                        "index": 0,
                                        "message": {
                                            "role": "assistant",
                                            "content": "Hello! How can I help you today?"
                                        },
                                        "finish_reason": "stop"
                                    }],
                                    "usage": {
                                        "prompt_tokens": 10,
                                        "completion_tokens": 8,
                                        "total_tokens": 18
                                    }
                                }
                                """)));

        var task = new LlmChatTaskV2(secretService);
        var result = task.execute(new MapBackedVariables(basicInput()));
        var simple = assertInstanceOf(TaskResult.SimpleResult.class, result);
        assertTrue(simple.ok());
        assertEquals("Hello! How can I help you today?", simple.values().get("content"));
        assertEquals("stop", simple.values().get("finishReason"));
        assertEquals("gpt-4o", simple.values().get("model"));
        assertNotNull(simple.values().get("usage"));
    }

    @Test
    void testToolCalls() throws Exception {
        configureFor(httpMock.getPort());
        when(secretService.exportAsString(ORG_NAME, SECRET_NAME, null)).thenReturn(API_KEY);

        stubFor(post(urlEqualTo("/chat/completions"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "id": "chatcmpl-456",
                                    "object": "chat.completion",
                                    "created": 1677652288,
                                    "model": "gpt-4o",
                                    "choices": [{
                                        "index": 0,
                                        "message": {
                                            "role": "assistant",
                                            "content": null,
                                            "tool_calls": [{
                                                "id": "call_abc123",
                                                "type": "function",
                                                "function": {
                                                    "name": "get_weather",
                                                    "arguments": "{\\"city\\":\\"San Francisco\\"}"
                                                }
                                            }]
                                        },
                                        "finish_reason": "tool_calls"
                                    }],
                                    "usage": {
                                        "prompt_tokens": 20,
                                        "completion_tokens": 15,
                                        "total_tokens": 35
                                    }
                                }
                                """)));

        var inputWithTools = new MapBackedVariables(Map.of(
                "auth", Map.of(
                        "type", "bearer",
                        "secretRef", Map.of(
                                "orgName", ORG_NAME,
                                "secretName", SECRET_NAME)),
                "model", "gpt-4o",
                "baseUrl", httpMock.baseUrl(),
                "messages", List.of(
                        Map.of("role", "user", "content", "What's the weather in SF?")),
                "tools", List.of(
                        Map.of("type", "function",
                                "function", Map.of(
                                        "name", "get_weather",
                                        "parameters", Map.of(
                                                "type", "object",
                                                "properties", Map.of("city", Map.of("type", "string"))))))
        ));

        var task = new LlmChatTaskV2(secretService);
        var result = task.execute(inputWithTools);
        var simple = assertInstanceOf(TaskResult.SimpleResult.class, result);
        assertTrue(simple.ok());
        assertNull(simple.values().get("content"));
        assertEquals("tool_calls", simple.values().get("finishReason"));

        @SuppressWarnings("unchecked")
        var toolCalls = (List<Map<String, Object>>) simple.values().get("toolCalls");
        assertNotNull(toolCalls);
        assertEquals(1, toolCalls.size());
        assertEquals("call_abc123", toolCalls.get(0).get("id"));
    }

    @Test
    void testApiError401() throws Exception {
        configureFor(httpMock.getPort());
        when(secretService.exportAsString(ORG_NAME, SECRET_NAME, null)).thenReturn(API_KEY);

        stubFor(post(urlEqualTo("/chat/completions"))
                .willReturn(aResponse()
                        .withStatus(401)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "error": {
                                        "message": "Incorrect API key provided",
                                        "type": "invalid_request_error",
                                        "code": "invalid_api_key"
                                    }
                                }
                                """)));

        var task = new LlmChatTaskV2(secretService);
        var result = task.execute(new MapBackedVariables(basicInput()));
        var simple = assertInstanceOf(TaskResult.SimpleResult.class, result);
        assertFalse(simple.ok());
        assertTrue(simple.error().contains("Incorrect API key"));
    }

    @Test
    void testMissingAuth() {
        var task = new LlmChatTaskV2(secretService);
        var input = new MapBackedVariables(Map.of(
                "model", "gpt-4o",
                "baseUrl", httpMock.baseUrl(),
                "messages", List.of(
                        Map.of("role", "user", "content", "Hello"))
        ));

        var result = task.execute(input);
        var simple = assertInstanceOf(TaskResult.SimpleResult.class, result);
        assertFalse(simple.ok());
        assertTrue(simple.error().contains("auth"));
    }

    @Test
    void testSecretResolutionFailure() throws Exception {
        when(secretService.exportAsString(ORG_NAME, "bad-secret", null))
                .thenThrow(new RuntimeException("Secret not found"));

        var task = new LlmChatTaskV2(secretService);
        var input = new MapBackedVariables(Map.of(
                "auth", Map.of(
                        "type", "bearer",
                        "secretRef", Map.of(
                                "orgName", ORG_NAME,
                                "secretName", "bad-secret")),
                "model", "gpt-4o",
                "baseUrl", httpMock.baseUrl(),
                "messages", List.of(
                        Map.of("role", "user", "content", "Hello"))
        ));

        var result = task.execute(input);
        var simple = assertInstanceOf(TaskResult.SimpleResult.class, result);
        assertFalse(simple.ok());
        assertTrue(simple.error().contains("Failed to resolve secret"));
    }
}
