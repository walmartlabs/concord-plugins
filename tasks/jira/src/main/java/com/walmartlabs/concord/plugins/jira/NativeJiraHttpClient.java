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

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.walmartlabs.concord.client2.impl.MultipartBuilder;
import com.walmartlabs.concord.client2.impl.MultipartRequestBodyHandler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NativeJiraHttpClient implements JiraHttpClient {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new Jdk8Module());
    static final JavaType MAP_TYPE = MAPPER.getTypeFactory()
            .constructMapType(HashMap.class, String.class, Object.class);
    private static final JavaType LIST_OF_MAPS_TYPE = MAPPER.getTypeFactory()
            .constructCollectionType(List.class, MAP_TYPE);
    private static final String CONTENT_TYPE = "Content-Type";

    private final HttpClient client;
    private final JiraClientCfg cfg;
    private URI url;
    private int successCode;
    private String auth;

    public NativeJiraHttpClient(JiraClientCfg cfg) {
        this.cfg = cfg;

        var builder = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(cfg.connectTimeout()));

        if (cfg.httpProtocolVersion() != JiraClientCfg.HttpVersion.DEFAULT) {
            if (cfg.httpProtocolVersion() == JiraClientCfg.HttpVersion.HTTP_2) {
                builder.version(HttpClient.Version.HTTP_2);
            } else if (cfg.httpProtocolVersion() == JiraClientCfg.HttpVersion.HTTP_1_1) {
                builder.version(HttpClient.Version.HTTP_1_1);
            }
        }

        client = builder.build();
    }

    @Override
    public JiraHttpClient url(String url) {
        this.url = URI.create(url);
        return this;
    }

    @Override
    public JiraHttpClient successCode(int successCode) {
        this.successCode = successCode;
        return this;
    }

    @Override
    public JiraHttpClient jiraAuth(String auth) {
        this.auth = auth;
        return this;
    }

    @Override
    public Map<String, Object> get() throws IOException {
        HttpRequest request = requestBuilder(auth)
                .uri(url)
                .GET()
                .build();

        return call(request, MAP_TYPE);
    }

    @Override
    public Map<String, Object> post(Map<String, Object> data) throws IOException {
        HttpRequest.BodyPublisher body = HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(data));
        HttpRequest request = requestBuilder(auth)
                .uri(url)
                .POST(body)
                .header(CONTENT_TYPE, "application/json; charset=utf-8")
                .build();

        return call(request, MAP_TYPE);
    }

    @Override
    public void post(File file) throws IOException {
        var requestBody = new MultipartBuilder(Constants.BOUNDARY)
                .addFormDataPart("file", file.getName(), new MultipartRequestBodyHandler.PathRequestBody(file.toPath()))
                .build();

        try (InputStream body = requestBody.getContent()) {
            var req = requestBuilder(auth)
                    .uri(url)
                    .POST(HttpRequest.BodyPublishers.ofInputStream(() -> body))
                    .header(CONTENT_TYPE, requestBody.contentType().toString())
                    .header("X-Atlassian-Token", "nocheck")
                    .build();

            call(req, LIST_OF_MAPS_TYPE);
        }
    }

    @Override
    public void put(Map<String, Object> data) throws IOException {
        HttpRequest request = requestBuilder(auth)
                .uri(url)
                .PUT(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(data)))
                .header(CONTENT_TYPE, "application/json; charset=utf-8")
                .build();

        call(request, MAP_TYPE);
    }

    @Override
    public void delete() throws IOException {
        HttpRequest request = requestBuilder(auth)
                .uri(url)
                .DELETE()
                .build();

        call(request, MAP_TYPE);
    }

    private HttpRequest.Builder requestBuilder(String auth) {
        return HttpRequest.newBuilder()
                .timeout(Duration.ofSeconds(cfg.readTimeout()))
                .header("Authorization", auth)
                .header("User-Agent", cfg.userAgent())
                .header("Accept", "application/json");
    }

    <T> T call(HttpRequest request, JavaType returnType) throws IOException {
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            int statusCode = response.statusCode();
            String results = response.body();
            JiraHttpClient.assertResponseCode(statusCode, results, successCode);

            if (results == null || statusCode == 204) {
                return null;
            } else {
                return MAPPER.readValue(results, returnType);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
