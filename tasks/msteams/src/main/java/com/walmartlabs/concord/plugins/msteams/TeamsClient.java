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
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TeamsClient implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(TeamsClient.class);

    private final int retryCount;
    private final HttpClient client;

    public TeamsClient(TeamsConfiguration cfg) {
        this.retryCount = cfg.retryCount();
        this.client = createClient(cfg);
    }

    @Override
    public void close() {
        // leaving this for now, in case something out there is using it.
        // it is public :(
    }

    public Result message(TeamsConfiguration cfg, String title, String text, String themeColor,
                          List<Object> sections, List<Object> potentialAction) throws IOException {

        Map<String, Object> params = new HashMap<>();
        params.put("title", title);
        params.put("text", text);
        params.put("themeColor", themeColor);

        if (sections != null && !sections.isEmpty()) {
            params.put("sections", sections);
        }
        if (potentialAction != null && !potentialAction.isEmpty()) {
            params.put("potentialAction", potentialAction);
        }

        return exec(cfg, params);
    }

    Result exec(TeamsConfiguration cfg, Map<String, Object> params) throws IOException {
        var webhookUrl = getWebhookUrl(cfg);
        var request = HttpRequest.newBuilder(URI.create(webhookUrl))
                .POST(HttpRequest.BodyPublishers.ofString(Utils.mapper().writeValueAsString(params)))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofMillis(cfg.soTimeout()))
                .build();

        for (int i = 0; i < retryCount + 1; i++) {
            try {
                var response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == Constants.TOO_MANY_REQUESTS_ERROR) {
                    int retryAfter = getRetryAfter(response);
                    log.warn("exec [webhookUrl: '{}', params: '{}'] -> too many requests, retry after {} sec", webhookUrl, params, retryAfter);
                    sleep(retryAfter * 1000L);
                } else {
                    var body = response.body();
                    if (body == null) {
                        log.error("exec [webhookUrl: '{}', params: '{}'] -> empty response", webhookUrl, params);
                        return new Result(false, "empty response", null, null, null);
                    }

                    if (response.statusCode() != Constants.TEAMS_SUCCESS_STATUS_CODE) {
                        log.error("exec [webhookUrl: '{}', params: '{}'] -> failed response", webhookUrl, params);
                        return new Result(false, body, null, null, null);
                    }

                    Result r = new Result(true, null, body, null, null);
                    log.info("exec [webhookUrl: '{}', params: '{}'] -> {}", webhookUrl, params, r);
                    return r;
                }
            } catch (IOException e) {
                log.error("IO Error sending request to webhook url '{}': {}", webhookUrl, e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("thread interrupted");
            }
        }

        return new Result(false, "too many requests", null, null, null);
    }

    private String getWebhookUrl(TeamsConfiguration cfg) {
        String teamId = cfg.teamId();
        String webhookId = cfg.webhookId();
        String webhookUrl = cfg.webhookUrl();

        if ((teamId != null && !teamId.isEmpty()) && (webhookId != null && !webhookId.isEmpty())) {
            return cfg.rootWebhookUrl() + teamId + "@" + cfg.tenantId() + "/IncomingWebhook/" + webhookId + "/" + cfg.webhookTypeId();
        } else if (webhookUrl != null && !webhookUrl.isEmpty()) {
            return webhookUrl;
        } else {
            throw new IllegalArgumentException("Mandatory parameters 'teamId & webhookId' or 'webhookUrl' is required for the execution of 'msteams' task");
        }
    }

    public static int getRetryAfter(HttpResponse<String> response) {
        var retryAfterHeader = response.headers().firstValue("Retry-After");

        if (retryAfterHeader.isEmpty()) {
            return Constants.DEFAULT_RETRY_AFTER;
        }

        try {
            return Integer.parseInt(retryAfterHeader.get());
        } catch (Exception e) {
            log.warn("getRetryAfter -> can't parse retry value '{}'", retryAfterHeader.get());
            return Constants.DEFAULT_RETRY_AFTER;
        }
    }

    void sleep(long t) {
        try {
            Thread.sleep(t);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static HttpClient createClient(TeamsConfiguration cfg) {
        var clientBuilder =  HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(cfg.connectTimeout()));

        if (cfg.proxyAddress() != null) {
            clientBuilder.proxy(ProxySelector.of(new InetSocketAddress(cfg.proxyAddress(), cfg.proxyPort())));
        }

        return clientBuilder.build();
    }
}
