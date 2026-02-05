package com.walmartlabs.concord.plugins.git.client;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2025 Walmart Inc., Concord Authors
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
import com.walmartlabs.concord.plugins.git.model.GitHubApiInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

public class GitHubClient {

    private final static Logger log = LoggerFactory.getLogger(GitHubClient.class);

    private static final TypeReference<List<Map<String, Object>>> LIST_OF_OBJECTS_TYPE = new TypeReference<>() {
    };

    private static final TypeReference<Map<String, Object>> OBJECT_TYPE = new TypeReference<>() {
    };

    private static final String HOST_API = "api.github.com";

    private static final Pattern NEXT_LINK = Pattern.compile("<([^>]+)>;\\s*rel=\"next\"");
    private static final int MAX_RETRY_ATTEMPTS = 5;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final UUID txId;
    private final GitHubApiInfo apiInfo;

    private final HttpClient httpClient;

    public GitHubClient(UUID txId, GitHubApiInfo apiInfo) {
        this.txId = txId;
        this.apiInfo = apiInfo;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    public Map<String, Object> singleObjectResult(String method, String path, Object body) throws IOException, InterruptedException, URISyntaxException {
        var response = sendRequest(method, path, Map.of(), body);
        return parseResponseAs(response, OBJECT_TYPE);
    }

    public List<Map<String, Object>> singleArrayResult(String method, String path, Object body) throws IOException, InterruptedException, URISyntaxException {
        var response = sendRequest(method, path, Map.of(), body);
        return parseResponseAs(response, LIST_OF_OBJECTS_TYPE);
    }

    public void voidResult(String method, String path, Object body) throws IOException, InterruptedException, URISyntaxException {
        sendRequest(method, path, Map.of(), body);
    }

    public void forEachPage(String path, Map<String, String> params, int pageSize, PageHandler handler)
            throws IOException, InterruptedException, URISyntaxException {

        requireNonNull(path, "'path' must not be null");
        requireNonNull(params, "'params' must not be null");

        var effectiveParams = new HashMap<>(params);
        effectiveParams.put("per_page", String.valueOf(pageSize));

        var apiUrl = buildApiUrl(apiInfo.baseUrl(), path);
        var requestUrl = appendParamsToUrl(apiUrl, effectiveParams);
        while (requestUrl != null) {
            var response = sendRequest("GET", requestUrl, null);
            if (response.body() == null || response.body().isBlank()) {
                throw new RuntimeException("Got empty response from GitHub");
            }

            boolean keepGoing = handler.onPage(objectMapper.readValue(response.body(), LIST_OF_OBJECTS_TYPE));
            if (!keepGoing) {
                break;
            }

            requestUrl = parseNextLink(response.headers().firstValue("Link").orElse(null));
        }
    }

    private static String buildApiUrl(String baseUrl, String path) throws URISyntaxException {
        var uri = new URI(baseUrl);
        var host = uri.getHost();
        String prefix = null;
        if ("github.com".equals(host) || "gist.github.com".equals(host)) {
            host = HOST_API;
        }

        // Use URI prefix on non-standard host names
        if (!HOST_API.equals(host)) {
            prefix = "/api/v3";
        }

        var scheme = requireNonNull(uri.getScheme(), "Base URL without schema");
        var port = uri.getPort();

        var apiUri = new URI(scheme, null, host, port, joinPaths(prefix, path), null, null);
        return apiUri.toString();
    }

    private HttpResponse<String> sendRequest(String method, String path, Map<String, String> params, Object body) throws IOException, InterruptedException, URISyntaxException {
        var apiUrl = buildApiUrl(apiInfo.baseUrl(), path);
        var requestUrl = appendParamsToUrl(apiUrl, params);
        return sendRequest(method, requestUrl, body);
    }

    private HttpResponse<String> sendRequest(String method, String requestUrl, Object body) throws IOException, InterruptedException, URISyntaxException {
        var requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(requestUrl))
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "application/vnd.github+json")
                .header("Authorization", "Bearer " + apiInfo.accessTokenProvider().getToken())
                .header("User-Agent", "Concord-GitHub-Plugin: " + txId);

        if (body != null) {
            var jsonBody = objectMapper.writeValueAsString(body);
            requestBuilder.header("Content-Type", "application/json");
            requestBuilder.method(method, BodyPublishers.ofString(jsonBody));
        } else if ("DELETE".equals(method)) {
            requestBuilder.DELETE();
        } else {
            requestBuilder.method(method, BodyPublishers.noBody());
        }

        var attempts = 0;
        while (true) {
            attempts++;
            HttpResponse<String> resp;
            try {
                resp = httpClient.send(requestBuilder.build(), BodyHandlers.ofString());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw e;
            }
            var code = resp.statusCode();

            if (code >= 200 && code < 300) {
                return resp;
            }

            if (isRetryable(method, code)) {
                var sleepMs = retryDelayMs(resp, attempts);
                var reqId = resp.headers().firstValue("X-GitHub-Request-Id").orElse("n/a");
                log.warn("GitHub returned {} (reqId={}). Retry in {} ms (attempt {} of {})",
                        code, reqId, sleepMs, attempts, MAX_RETRY_ATTEMPTS);
                if (sleepMs >= 0 && attempts < MAX_RETRY_ATTEMPTS) {
                    sleep(sleepMs);
                    continue;
                }
            }

            throw toException(resp);
        }
    }

    private <T> T parseResponseAs(HttpResponse<String> response, TypeReference<T> type) throws IOException {
        if (response.body() == null || response.body().isBlank()) {
            throw new RuntimeException("Got empty response from GitHub");
        }

        return objectMapper.readValue(response.body(), type);
    }

    private static boolean isRetryable(String method, int code) {
        var m = method.toUpperCase();
        var idempotent =
                m.equals("GET") || m.equals("HEAD") || m.equals("PUT")
                        || m.equals("DELETE") || m.equals("OPTIONS");
        return idempotent && (code == 429 || code == 500 || code == 502 || code == 503);
    }

    private static long retryDelayMs(HttpResponse<?> resp, int attempts) {
        var retryAfter = resp.headers().firstValue("Retry-After");
        if (retryAfter.isPresent()) {
            try {
                return Long.parseLong(retryAfter.get()) * 1000L;
            } catch (NumberFormatException ignore) {
            }
        }

        var reset = resp.headers().firstValue("X-RateLimit-Reset")
                .map(Long::parseLong).orElse(-1L);
        if (reset > 0) {
            var now = System.currentTimeMillis() / 1000L;
            var delta = (reset - now + 1) * 1000L;
            if (delta > 0) {
                return delta;
            }
        }

        var base = (long) Math.min(1000 * Math.pow(2, attempts - 1), 8000);
        var jitter = (long) (Math.random() * 250);
        return base + jitter;
    }

    private static GitHubApiException toException(HttpResponse<String> resp) {
        var body = resp.body();
        var message = "GitHub API error: " + resp.statusCode();
        if (body != null && !body.isBlank()) {
            try {
                var node = objectMapper.readTree(body);
                message += " - " + node.path("message").asText("");
                if (node.has("errors")) {
                    message += " - " +  node.get("errors").toString();
                }
            } catch (Exception ignore) {
                message += " - " + body;
            }
        }
        return new GitHubApiException(message, resp.statusCode());
    }

    static void sleep(long millis) throws InterruptedException {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw ie;
        }
    }

    private static String joinPaths(String a, String b) {
        var p2 = b.startsWith("/") ? b : "/" + b;
        if (a == null) {
            return p2;
        }
        var p1 = a.endsWith("/") ? a.substring(0, a.length() - 1) : a;
        return p1 + p2;
    }

    private static String appendParamsToUrl(String absoluteUrl, Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return absoluteUrl;
        }

        var sb = new StringBuilder(absoluteUrl);
        var sep = absoluteUrl.contains("?") ? '&' : '?';

        for (var e : params.entrySet()) {
            var k = e.getKey();
            var v = e.getValue();
            if (k == null || v == null) {
                continue;
            }
            sb.append(sep).append(urlEncode(k)).append('=').append(urlEncode(v));
            sep = '&';
        }
        return sb.toString();
    }

    private static String urlEncode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8)
                .replace("+", "%20");
    }

    static String parseNextLink(String linkHeader) {
        if (linkHeader == null || linkHeader.isBlank()) {
            return null;
        }
        var m = NEXT_LINK.matcher(linkHeader);
        return m.find() ? m.group(1) : null;
    }
}
