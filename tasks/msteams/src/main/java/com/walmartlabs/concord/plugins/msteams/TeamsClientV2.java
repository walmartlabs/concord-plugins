package com.walmartlabs.concord.plugins.msteams;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2018 Walmart Inc.
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class TeamsClientV2 implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(TeamsClientV2.class);

    private final int retryCount;
    private final HttpClient client;
    private final Map<String, String> defaultHeaders;

    public TeamsClientV2(TeamsV2Configuration cfg) {
        this.defaultHeaders = new HashMap<>();
        this.retryCount = cfg.retryCount();
        this.client = createClient(cfg);
        defaultHeaders.put("Authorization", "Bearer " + generateAccessToken(cfg, client));
    }

    @Override
    public void close() {
        // leaving this for now, in case something out there is using it.
        // it is public :(
    }

    public Result createConversation(String tenantId, Map<String, Object> activity,
                                     String channelId, String rootApi) throws IOException {

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("tenantId", tenantId);
        params.put("activity", activity);

        Map<String, Object> channel = Collections.singletonMap("id", URLDecoder.decode(channelId, StandardCharsets.UTF_8));
        Map<String, Object> channelData = Collections.singletonMap("channel", channel);
        params.put("channelData", channelData);
        return exec(params, rootApi);
    }

    public Result replyToConversation(Map<String, Object> activity, String rootApi, String conversationId) throws IOException {
        rootApi = rootApi + "/" + conversationId + "/activities";
        return exec(activity, rootApi);
    }

    private HttpRequest.Builder buildRequest() {
        var builder = HttpRequest.newBuilder();

        defaultHeaders.forEach(builder::header);

        return builder;
    }

    Result exec(Map<String, Object> params, String rootApi) throws IOException {
        var request = buildRequest()
                .uri(URI.create(rootApi))
                .POST(HttpRequest.BodyPublishers.ofString(Utils.mapper().writeValueAsString(params)))
                .header("Content-Type", "application/json")
                .build();

        for (int i = 0; i < retryCount + 1; i++) {
            try {
                var response = client.send(request, HttpResponse.BodyHandlers.ofString());
                var body = response.body();

                if (response.statusCode() == Constants.TOO_MANY_REQUESTS_ERROR) {
                    int retryAfter = TeamsClient.getRetryAfter(response);
                    log.warn("exec [params: '{}'] -> too many requests, retry after {} sec", params, retryAfter);
                    sleep(retryAfter * 1000L);
                } else {
                    if (body == null) {
                        log.error("exec [params: '{}'] -> empty response", params);
                        return new Result(false, "empty response", null, null, null);
                    }

                    if (response.statusCode() != Constants.TEAMS_SUCCESS_STATUS_CODE_V2) {
                        log.error("exec [params: '{}'] -> failed response", params);
                        return new Result(false, body, null, null, null);
                    }

                    Result r;
                    var tree = Utils.mapper().readTree(body);
                    if (tree.has("activityId")) {
                        String conversationId = tree.get("id").toString().
                                replace("\"", "");
                        String activityId = tree.get("activityId").toString().
                                replace("\"", "");

                        r = new Result(true, null, body, conversationId, activityId);
                        log.info("exec [params: '{}'] -> {}", params, r);
                        return r;
                    }
                    r = new Result(true, null, body, null, null);
                    log.info("exec [params: '{}'] -> {}", params, r);
                    return r;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        return new Result(false, "too many requests", null, null, null);
    }

    private static HttpClient createClient(TeamsV2Configuration cfg) {
        var clientBuilder =  HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(cfg.connectTimeout()));

        if (cfg.proxyAddress() != null) {
            clientBuilder.proxy(ProxySelector.of(new InetSocketAddress(cfg.proxyAddress(), cfg.proxyPort())));
        }

        return clientBuilder.build();
    }

    @SuppressWarnings("unchecked")
    String generateAccessToken(TeamsV2Configuration cfg, HttpClient client) {
        Map<String, String> params = new LinkedHashMap<>(); // linked so order is predictable for unit tests
        params.put("client_id", cfg.clientId());
        params.put("client_secret", cfg.clientSecret());
        params.put("grant_type", Constants.API_BOT_FRAMEWORK_GRANT_TYPE);
        params.put("scope", Constants.API_BOT_FRAMEWORK_SCOPE);

        var paramString = params.entrySet()
                .stream()
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));

        var request = HttpRequest.newBuilder(URI.create(cfg.accessTokenApi()))
                .POST(HttpRequest.BodyPublishers.ofString(paramString))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .timeout(Duration.ofMillis(cfg.soTimeout()))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();

            if (response.statusCode() != 200) {
                throw new RuntimeException("Error while generating access token" + responseBody);
            }

            if (responseBody == null || responseBody.isBlank()) {
                throw new RuntimeException("No body returned for access token request.");
            }

            Map<Object, String> map = Utils.mapper().readValue(responseBody, Map.class);
            return map.get(Constants.VAR_ACCESS_TOKEN);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            throw new RuntimeException("IO error while retieving access token: " + e.getMessage(), e);
        }

        return null;
    }

    void sleep(long t) {
        try {
            Thread.sleep(t);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
