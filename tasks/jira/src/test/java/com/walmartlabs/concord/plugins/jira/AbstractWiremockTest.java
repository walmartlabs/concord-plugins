package com.walmartlabs.concord.plugins.jira;

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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class AbstractWiremockTest {

    @RegisterExtension
    static WireMockExtension rule = WireMockExtension.newInstance()
            .options(wireMockConfig()
                    .dynamicPort()
                    .notifier(new ConsoleNotifier(true)))
            .build();

    protected static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new Jdk8Module());

    protected JiraClientCfg jiraClientCfg;

    protected void stubForCurrentStatus() {
        var body = Map.of(
                "fields", Map.of(
                        "status", Map.of(
                                "name", "Open"
                        )
                )
        );

        var mapper = new ObjectMapper();

        try {
            rule.stubFor(get(urlEqualTo("/issue/issueId?fields=status"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(mapper.writeValueAsString(body)))
            );
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid json: " + e.getMessage());
        }
    }

    protected void stubForBasicAuth() {
        rule.stubFor(post(urlEqualTo("/issue/"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\n" +
                                "  \"id\": \"123\",\n" +
                                "  \"key\": \"key1\",\n" +
                                "  \"self\": \"2\"\n" +
                                "}\n"))
        );
    }

    protected void stubForAddAttachment() {
        rule.stubFor(post(urlEqualTo("/issue/issueId/attachments"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[{\n" +
                                "  \"id\": \"123\",\n" +
                                "  \"key\": \"key1\",\n" +
                                "  \"self\": \"2\"\n" +
                                "}]"))
        );
    }

    protected void stubForPut() {
        rule.stubFor(put(urlEqualTo("/issue/issueId"))
                .willReturn(aResponse()
                        .withStatus(204))
        );
    }

    protected void stubForDelete() {
        rule.stubFor(delete(urlEqualTo("/issue/issueId"))
                .willReturn(aResponse()
                        .withStatus(204))
        );
    }

    private static void assertAuth(String value) {
        assertEquals("Basic " + Base64.getEncoder().encodeToString("mock-user:mock-pass".getBytes(StandardCharsets.UTF_8)), value);
    }

    @BeforeEach
    void setUp() {
        jiraClientCfg = new JiraClientCfg() {
            @Override
            public HttpVersion httpProtocolVersion() {
                return HttpVersion.HTTP_1_1;
            }
        };
    }

    abstract JiraHttpClient getClient(JiraClientCfg cfg);

    @Test
    void testGet() throws IOException {
        stubForCurrentStatus();

        Map<String, Object> resp = getClient(jiraClientCfg)
                .url(rule.baseUrl() + "/issue/issueId?fields=status")
                .jiraAuth(new JiraCredentials("mock-user", "mock-pass").authHeaderValue())
                .successCode(200)
                .get();

        assertNotNull(resp);
        Map<String, Object> expected = Map.of(
                "fields", Map.of(
                        "status", Map.of(
                                "name", "Open"
                        )
                )
        );
        assertEquals(expected, resp);

        ServeEvent event = rule.getAllServeEvents().get(0);
        assertNotNull(event);

        assertAuth(event.getRequest().header("Authorization").firstValue());
        assertEquals("Concord-Jira-Plugin", event.getRequest().header("User-Agent").firstValue());
    }

    @Test
    void testPost() throws IOException {
        stubForBasicAuth();

        Map<String, Object> resp = getClient(jiraClientCfg)
                .url(rule.baseUrl() + "/issue/")
                .jiraAuth(new JiraCredentials("mock-user", "mock-pass").authHeaderValue())
                .successCode(201)
                .post(Map.of("field1", "value1"));

        assertNotNull(resp);
        assertEquals("123", resp.get("id"));

        ServeEvent event = rule.getAllServeEvents().get(0);
        assertNotNull(event);

        Map<String, Object> requestBody = objectMapper.readValue(event.getRequest().getBody(), NativeJiraHttpClient.MAP_TYPE);
        assertEquals("value1", requestBody.get("field1"));

        assertAuth(event.getRequest().header("Authorization").firstValue());
        assertEquals("Concord-Jira-Plugin", event.getRequest().header("User-Agent").firstValue());
    }

    @Test
    void testPostFile() throws IOException {
        stubForAddAttachment();

        getClient(jiraClientCfg)
                .url(rule.baseUrl() + "/issue/issueId/attachments")
                .successCode(200)
                .jiraAuth(new JiraCredentials("mock-user", "mock-pass").authHeaderValue())
                .post(Paths.get("src/test/resources/sample.txt").toFile());

        ServeEvent event = rule.getAllServeEvents().get(0);
        assertNotNull(event);

        String requestBody = event.getRequest().getBodyAsString();
        assertTrue(requestBody.contains("Content-Length: 11"));
        assertTrue(requestBody.contains("Content-Type: application/octet-stream"));
        assertTrue(requestBody.contains("Content-Disposition: form-data; name=\"file\"; filename=\"sample.txt\""));

        String contentType = event.getRequest().header("Content-Type").firstValue();
        assertEquals("multipart/form-data; boundary=" + Constants.BOUNDARY, contentType);

        assertAuth(event.getRequest().header("Authorization").firstValue());
        assertEquals("Concord-Jira-Plugin", event.getRequest().header("User-Agent").firstValue());
    }

    @Test
    void testPut() throws IOException {
        stubForPut();

        getClient(jiraClientCfg)
                .url(rule.baseUrl() + "/issue/issueId")
                .successCode(204)
                .jiraAuth(new JiraCredentials("mock-user", "mock-pass").authHeaderValue())
                .put(Map.of("aKey", "aValue"));

        ServeEvent event = rule.getAllServeEvents().get(0);
        assertNotNull(event);

        assertAuth(event.getRequest().header("Authorization").firstValue());
        assertEquals("Concord-Jira-Plugin", event.getRequest().header("User-Agent").firstValue());
    }

    @Test
    void testDelete() throws IOException {
        stubForDelete();

        getClient(jiraClientCfg)
                .url(rule.baseUrl() + "/issue/issueId")
                .successCode(204)
                .jiraAuth(new JiraCredentials("mock-user", "mock-pass").authHeaderValue())
                .delete();

        ServeEvent event = rule.getAllServeEvents().get(0);
        assertNotNull(event);

        assertAuth(event.getRequest().header("Authorization").firstValue());
        assertEquals("Concord-Jira-Plugin", event.getRequest().header("User-Agent").firstValue());
    }

    @Test
    void testResponseCodes() {
        var ex400 = assertThrows(IllegalStateException.class, () -> JiraHttpClient.assertResponseCode(400, "example message", 200));
        assertTrue(ex400.getMessage().contains("input is invalid"));
        var ex401 = assertThrows(IllegalStateException.class, () -> JiraHttpClient.assertResponseCode(401, "example message", 200));
        assertTrue(ex401.getMessage().contains("User is not authenticated"));
        var ex403 = assertThrows(IllegalStateException.class, () -> JiraHttpClient.assertResponseCode(403, "example message", 200));
        assertTrue(ex403.getMessage().contains("User does not have permission to perform request"));
        var ex404 = assertThrows(IllegalStateException.class, () -> JiraHttpClient.assertResponseCode(404, "example message", 200));
        assertTrue(ex404.getMessage().contains("Issue does not exist"));
        var ex500 = assertThrows(IllegalStateException.class, () -> JiraHttpClient.assertResponseCode(500, "example message", 200));
        assertTrue(ex500.getMessage().contains("Internal Server Error"));
        var exUnknown = assertThrows(IllegalStateException.class, () -> JiraHttpClient.assertResponseCode(501, "example message", 200));
        assertTrue(exUnknown.getMessage().contains("Error: example message"));
    }

}
