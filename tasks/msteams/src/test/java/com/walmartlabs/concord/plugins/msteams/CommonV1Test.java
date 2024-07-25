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
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.walmartlabs.concord.runtime.v2.sdk.MapBackedVariables;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class CommonV1Test {

    @RegisterExtension
    static WireMockExtension rule = WireMockExtension.newInstance()
            .options(wireMockConfig()
                    .dynamicPort()
                    .notifier(new ConsoleNotifier(false)))
            .build();

    private static final String MOCK_TEAM_ID = UUID.randomUUID().toString();
    private static final String MOCK_TENANT_ID = UUID.randomUUID().toString();
    private static final String MOCK_WEBHOOK_ID = UUID.randomUUID().toString();
    private static final String MOCK_WEBHOOK_TYPE_ID = UUID.randomUUID().toString();

    private TeamsTaskCommon common;
    private TeamsClient client;

    @BeforeEach
    void setUp() {
        common = spy(new TeamsTaskCommon());

        doAnswer(invocation -> {
            client = spy(new TeamsClient(invocation.getArgument(0)));
            return client;
        }).when(common).getClient(any(TeamsTaskParams.class));

    }

    @Test
    void testSendMessage() throws Exception {
        stubForSendMessage();

        Result r = common.execute(TeamsTaskParams.of(new MapBackedVariables(defaultParams()), Map.of()));

        assertTrue(r.isOk());

        ServeEvent event = rule.getAllServeEvents().get(0);
        assertNotNull(event);

        verify(common, times(1)).getClient(any(TeamsTaskParams.class));
        verify(client, times(1)).exec(any(), any());
        verify(client, times(0)).sleep(anyLong());

        Map<?, ?> requestBody = Utils.mapper().readValue(event.getRequest().getBody(), Map.class);
        assertEquals("mock message title", requestBody.get("title"));
        assertEquals("mock message text", requestBody.get("text"));
        assertEquals("B0620A", requestBody.get("themeColor"));
    }

    @Test
    void testTooManyMessages() throws Exception {
        stubForTooManyRequests();

        Result r = common.execute(TeamsTaskParams.of(new MapBackedVariables(defaultParams()), Map.of()));

        assertFalse(r.isOk());
        assertTrue(r.getError().contains("too many requests"));

        verify(common, times(1)).getClient(any(TeamsTaskParams.class));
        verify(client, times(1)).exec(any(), any());
        verify(client, times(2)).sleep(anyLong());
    }

    @Test
    void testIgnoreErrors() throws Exception {
        stubForError();

        var input = new HashMap<>(defaultParams());
        input.put("ignoreErrors", true);

        Result r = common.execute(TeamsTaskParams.of(new MapBackedVariables(input), Map.of()));

        assertFalse(r.isOk());
        assertTrue(r.getError().contains("too many requests"));

        // The client doesn't actually throw an exception, even when "ignoreErrors" is false
        // Seems like a design flaw, but that's how it's been working and these
        // tests are being added very late in the game
        // TODO consider actually throwing an error, and don't retry things like 4XX errors

        verify(common, times(1)).getClient(any(TeamsTaskParams.class));
        verify(client, times(1)).exec(any(), any());
        verify(client, times(0)).sleep(anyLong());
    }

    private Map<String, Object> defaultParams() {
        Map<String, Object> input = new HashMap<>();
        input.put("rootWebhookUrl", rule.baseUrl() + "/");
        input.put("retryCount", 1);
        input.put("webhookTypeId", MOCK_WEBHOOK_TYPE_ID);
        input.put("tenantId", MOCK_TENANT_ID);
        input.put("teamId", MOCK_TEAM_ID);
        input.put("webhookId", MOCK_WEBHOOK_ID);
        input.put("action", "sendMessage");
        input.put("title", "mock message title");
        input.put("text", "mock message text");
        input.put("themeColor", "B0620A");

        return input;
    }

    void stubForSendMessage() {
        rule.stubFor(post(urlMatching("/" + MOCK_TEAM_ID + "@" + MOCK_TENANT_ID + "/IncomingWebhook/" + MOCK_WEBHOOK_ID + "/" + MOCK_WEBHOOK_TYPE_ID))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/plain; charset=utf-8")
                        .withBody("1")));
    }

    void stubForTooManyRequests() {
        rule.stubFor(post(urlMatching("/" + MOCK_TEAM_ID + "@" + MOCK_TENANT_ID + "/IncomingWebhook/" + MOCK_WEBHOOK_ID + "/" + MOCK_WEBHOOK_TYPE_ID))
                .willReturn(aResponse()
                        .withStatus(429)
                        .withHeader("Content-Type", "text/plain; charset=utf-8")
                        .withBody("too many requests")));

    }

    void stubForError() {
        rule.stubFor(post(urlMatching("/" + MOCK_TEAM_ID + "@" + MOCK_TENANT_ID + "/IncomingWebhook/" + MOCK_WEBHOOK_ID + "/" + MOCK_WEBHOOK_TYPE_ID))
                .willReturn(aResponse()
                        .withFault(Fault.CONNECTION_RESET_BY_PEER)
                        .withBody("unauthorized")));

    }
}
