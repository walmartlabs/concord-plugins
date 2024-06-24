package com.walmartlabs.concord.plugins.gremlin;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2019 Walmart Inc.
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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Map;

public class GremlinClient {

    private final HttpClient client;

    private final GremlinClientParams params;

    private String url;
    private int successCode;

    public GremlinClient(GremlinClientParams params) {
        this.params = params;
        this.client = getClient(params);
    }

    public GremlinClient url(String url) {
        if (url.endsWith("/")) {
            this.url = url;
        } else {
            this.url = url + "/";
        }

        return this;
    }

    public GremlinClient successCode(int successCode) {
        this.successCode = successCode;
        return this;
    }

    public Map<String, Object> post(Map<String, Object> data) throws IOException {
        HttpRequest request = requestBuilder()
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(Utils.objectMapper().writeValueAsString(data)))
                .build();

        return call(request);
    }

    public Map<String, Object> get() throws IOException {
        HttpRequest request = requestBuilder()
                .GET()
                .build();

        return call(request);
    }

    public void delete() {
        HttpRequest request = requestBuilder()
                .DELETE()
                .build();

        deleteCall(request);
    }

    private HttpRequest.Builder requestBuilder() {

        var rawUrl = params.teamId() == null
                ? url
                : url + toParameterString(Map.of("teamId", params.teamId()));

        return HttpRequest.newBuilder(URI.create(rawUrl))
                .timeout(Duration.ofSeconds(params.connectTimeout()))
                .header("X-Gremlin-Agent", "concord/" + Version.getVersion())
                .header("Authorization", "Key " + params.apiKey());
    }

    private static String toParameterString(Map<String, String> params) {
        if (params.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder("?");
        var paramQueue = new ArrayDeque<>(params.entrySet().stream().toList());

        while (!paramQueue.isEmpty()) {
            var param = paramQueue.poll();

            sb.append(URLEncoder.encode(param.getKey(), StandardCharsets.UTF_8))
                    .append("=")
                    .append(URLEncoder.encode(param.getValue(), StandardCharsets.UTF_8));

            if (!paramQueue.isEmpty()) {
                sb.append("&");
            }
        }

        return sb.toString();
    }

    private Map<String, Object> call(HttpRequest request) throws IOException {
        String results = null;
        var response = getClientResponse(request, HttpResponse.BodyHandlers.ofString());
        var responseBody = response.body();
        int statusCode = response.statusCode();
        if (responseBody != null) {
            results = responseBody;
        }
        Map<String, Object> objResults = Collections.singletonMap("results", results);
        assertResponseCode(statusCode, objResults.toString(), successCode);

        return Map.of("results", Utils.objectMapper().readValue(responseBody, Map.class));
    }

    private void deleteCall(HttpRequest request) {
        var response = getClientResponse(request, HttpResponse.BodyHandlers.ofString());
        int statusCode = response.statusCode();
        var responseBody = response.body();
        String results = null;
        if (responseBody != null) {
            results = responseBody;
        }
        assertResponseCode(statusCode, results, successCode);
    }

    private static void assertResponseCode(int code, String result, int successCode) {
        if (code == successCode) {
            return;
        }

        if (code == 400) {
            throw new RuntimeException("Input is invalid (e.g. missing required fields, invalid values). Here are the full error details: " + result);
        } else if (code == 401) {
            throw new RuntimeException("User is not authenticated. Here are the full error details: " + result);
        } else if (code == 403) {
            throw new RuntimeException("User does not have permission to perform request. Here are the full error details: " + result);
        } else if (code == 404) {
            throw new RuntimeException("Attack does not exist. Here are the full error details: " + result);
        } else if (code == 500) {
            throw new RuntimeException("Internal Server Error. Here are the full error details" + result);
        } else {
            throw new RuntimeException("Error: " + result);
        }
    }

    private <T> HttpResponse<T> getClientResponse(HttpRequest request, HttpResponse.BodyHandler<T> bodyHandler) {
        try {
            return client.send(request, bodyHandler);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Thread interrupted", e);
        } catch (IOException e) {
            throw new RuntimeException("Error: " + e);
        }
    }

    private HttpClient getClient(GremlinClientParams params) {
        var clientBuilder = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(params.connectTimeout()));

        if (params.useProxy()) {
            clientBuilder.proxy(ProxySelector.of(new InetSocketAddress(params.proxyHost(), params.proxyPort())));
        }

        return clientBuilder.build();
    }
}

