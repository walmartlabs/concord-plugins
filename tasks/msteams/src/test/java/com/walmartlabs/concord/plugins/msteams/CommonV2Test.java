package com.walmartlabs.concord.plugins.msteams;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2024 Walmart Inc., Concord Authors
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

import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.walmartlabs.concord.runtime.v2.sdk.MapBackedVariables;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class CommonV2Test {

    @RegisterExtension
    static WireMockExtension rule = WireMockExtension.newInstance()
            .options(wireMockConfig()
                    .dynamicPort()
                    .notifier(new ConsoleNotifier(false)))
            .build();

    private static final String MOCK_TENANT_ID = UUID.randomUUID().toString();
    private static final String MOCK_ACCESS_TOKEN = UUID.randomUUID().toString();
    private static final String MOCK_CHANNEL_ID = UUID.randomUUID().toString();
    private static final String MOCK_CLIENT_ID = UUID.randomUUID().toString();
    private static final String MOCK_CLIENT_SECRET = UUID.randomUUID().toString();
    private static final String MOCK_CONVERSATION_ID = UUID.randomUUID().toString();
    private static final String MOCK_ACTIVITY_ID = UUID.randomUUID().toString();
    private static final String MOCK_REPLY_ID = UUID.randomUUID().toString();


    private TeamsV2TaskCommon common;
    private TeamsClientV2 client;

    @BeforeEach
    void setUp() {
        common = spy(new TeamsV2TaskCommon());

        doAnswer(invocation -> {
            client = spy(new TeamsClientV2(invocation.getArgument(0)));
            return client;
        }).when(common).getClient(any(TeamsV2TaskParams.class));
    }

    @Test
    void testSendMessage() throws Exception {
        stubForAuth();
        stubForCreateConversation();

        Result r = common.execute(TeamsV2TaskParams.of(new MapBackedVariables(defaultParams()), Map.of()));

        assertTrue(r.isOk());

        verify(common, times(1)).getClient(any(TeamsV2TaskParams.class));
        verify(common, times(1)).createConversation(any(TeamsV2TaskParams.CreateConversationParams.class));
        verify(client, times(1)).exec(any(), any());
        verify(client, times(0)).sleep(anyLong());

        var authEvent = rule.getAllServeEvents().get(1);
        assertNotNull(authEvent);
        assertEquals("/botframework.com/oauth2/v2.0/token", authEvent.getRequest().getUrl());

        var messageEvent = rule.getAllServeEvents().get(0);
        assertNotNull(messageEvent);
        assertEquals("/amer/v3/conversations", messageEvent.getRequest().getUrl());
    }

    @Test
    void testReply() throws Exception {
        stubForAuth();
        stubForReply();

        var input = new HashMap<>(defaultParams());
        input.put("action", "replyToConversation");
        input.put("conversationId", MOCK_CONVERSATION_ID);
        input.put("activity", Map.of(
                "type", "message",
                "text", "mock reply text"
        ));

        Result r = common.execute(TeamsV2TaskParams.of(new MapBackedVariables(input), Map.of()));

        assertTrue(r.isOk());

        verify(common, times(1)).getClient(any(TeamsV2TaskParams.class));
        verify(common, times(1)).replyToConversation(any(TeamsV2TaskParams.ReplayToConversationParams.class));
        verify(client, times(1)).exec(any(), any());
        verify(client, times(0)).sleep(anyLong());

        var authEvent = rule.getAllServeEvents().get(1);
        assertNotNull(authEvent);
        assertEquals("/botframework.com/oauth2/v2.0/token", authEvent.getRequest().getUrl());

        var messageEvent = rule.getAllServeEvents().get(0);
        assertNotNull(messageEvent);
        assertEquals("/amer/v3/conversations/" + MOCK_CONVERSATION_ID + "/activities", messageEvent.getRequest().getUrl());
    }

    @Test
    void testTooManyMessages() throws Exception {
        stubForAuth();
        stubForTooManyRequests();

        Result r = common.execute(TeamsV2TaskParams.of(new MapBackedVariables(defaultParams()), Map.of()));

        assertFalse(r.isOk());
        assertTrue(r.getError().contains("too many requests"));

        verify(common, times(1)).getClient(any(TeamsV2TaskParams.class));
        verify(client, times(1)).exec(any(), any());
        verify(client, times(2)).sleep(anyLong());
    }

    @Test
    void testIgnoreErrors() throws Exception {
        stubForAuth();
        stubForError();

        var input = new HashMap<>(defaultParams());
        var params = TeamsV2TaskParams.of(new MapBackedVariables(input), Map.of());

        var expected = assertThrows(RuntimeException.class, () -> common.execute(params));
        assertTrue(expected.getMessage().contains("error while creating conversation"));

        input.put("ignoreErrors", true);
        var r = assertDoesNotThrow(() -> common.execute(TeamsV2TaskParams.of(new MapBackedVariables(input), Map.of())));

        assertFalse(r.isOk());
        assertNotNull(r.getError());

        verify(common, times(2)).getClient(any(TeamsV2TaskParams.class));
        verify(client, times(1)).exec(any(), any());
        verify(client, times(0)).sleep(anyLong());
    }

    private Map<String, Object> defaultParams() {
        Map<String, Object> input = new HashMap<>();
        input.put("rootWebhookUrl", rule.baseUrl() + "/");
        input.put("retryCount", 1);
        input.put("tenantId", MOCK_TENANT_ID);
        input.put("clientId", MOCK_CLIENT_ID);
        input.put("clientSecret", MOCK_CLIENT_SECRET);
        input.put("rootApi", rule.baseUrl() + "/amer/v3/conversations");
        input.put("accessTokenApi", rule.baseUrl() + "/botframework.com/oauth2/v2.0/token");

        input.put("action", "createConversation");
        input.put("activity", Map.of(
                "type", "message",
                "text", "mock message text"));
        input.put("channelId", MOCK_CHANNEL_ID);

        return input;
    }

    void stubForAuth() {
        rule.stubFor(post(urlEqualTo("/botframework.com/oauth2/v2.0/token"))
                        .withRequestBody(equalTo("client_id=" + MOCK_CLIENT_ID +
                                "&client_secret=" + MOCK_CLIENT_SECRET +
                                "&grant_type=client_credentials&scope=https%3A%2F%2Fapi.botframework.com%2F.default"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withJsonBody(Utils.mapper().valueToTree(Map.of(
                                "token_type", "Bearer",
                                "expires_in", 86399,
                                "ext_expires", 86399,
                                "access_token", MOCK_ACCESS_TOKEN
                        )))));
    }

    void stubForCreateConversation() throws Exception {
        Map<String, Object> expectedRequestActivity = new LinkedHashMap<>();
        expectedRequestActivity.put("type", "message");
        expectedRequestActivity.put("text", "mock message text");
        Map<String, Object> expectedRequestBody = new LinkedHashMap<>();
        expectedRequestBody.put("tenantId", MOCK_TENANT_ID);
        expectedRequestBody.put("activity", expectedRequestActivity);
        expectedRequestBody.put("channelData", Map.of("channel", Map.of("id", MOCK_CHANNEL_ID)));

        rule.stubFor(post(urlEqualTo("/amer/v3/conversations"))
                        .withRequestBody(equalToJson(Utils.mapper().writeValueAsString(expectedRequestBody), true, false))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json; charset=utf-8")
                        .withJsonBody(Utils.mapper().valueToTree(Map.of(
                                "id", MOCK_CONVERSATION_ID,
                                "activityId", MOCK_ACTIVITY_ID
                        )))));
    }

    void stubForReply() throws Exception {
//        Map<String, Object> expectedRequestActivity = new LinkedHashMap<>();
//        expectedRequestActivity.put("type", "message");
//        expectedRequestActivity.put("text", "mock message text");
//        Map<String, Object> expectedRequestBody = new LinkedHashMap<>();
//        expectedRequestBody.put("tenantId", MOCK_TENANT_ID);
//        expectedRequestBody.put("activity", expectedRequestActivity);
//        expectedRequestBody.put("channelData", Map.of("channel", Map.of("id", MOCK_CHANNEL_ID)));

        rule.stubFor(post(urlEqualTo("/amer/v3/conversations/" + MOCK_CONVERSATION_ID + "/activities"))
                .withRequestBody(equalToJson(Utils.mapper().writeValueAsString(Map.of(
                        "type", "message",
                        "text", "mock reply text"
                )), true, false))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json; charset=utf-8")
                        .withJsonBody(Utils.mapper().valueToTree(Map.of(
                                "id", MOCK_REPLY_ID
                        )))));

    }

    void stubForTooManyRequests() {
        rule.stubFor(post(urlEqualTo("/amer/v3/conversations"))
                .willReturn(aResponse()
                        .withStatus(429)
                        .withHeader("Content-Type", "application/json; charset=utf-8")
                        .withBody("too many requests")));
    }

    void stubForError() {
        rule.stubFor(post(urlEqualTo("/amer/v3/conversations"))
                .willReturn(aResponse()
                        .withFault(Fault.CONNECTION_RESET_BY_PEER)
                        .withBody("unauthorized")));

    }
}
