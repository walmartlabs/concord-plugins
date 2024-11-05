package com.walmartlabs.concord.plugins.zoom;

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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.walmartlabs.concord.common.ConfigurationUtils;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.*;

public class ZoomClient implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(ZoomClient.class);
    private final int retryCount;
    private final PoolingHttpClientConnectionManager connManager;
    private final CloseableHttpClient client;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final boolean dryRunMode;

    public ZoomClient(ZoomConfiguration cfg, boolean dryRunMode) {
        this.retryCount = cfg.retryCount();
        this.dryRunMode = dryRunMode;
        this.connManager = createConnManager();
        this.client = createClient(cfg, connManager);
    }

    @Override
    public void close() throws IOException {
        client.close();
        connManager.close();
    }

    public Result message(String robotJid, String headText, String bodyText, String channelId,
                          String accountId, String rootApi) throws IOException {

        Map<String, Object> params = new HashMap<>();
        params.put("robot_jid", robotJid);
        params.put("to_jid", channelId);
        params.put("account_id", accountId);

        Map<String, Object> objHeadTextKey = Collections.singletonMap("text", headText);
        Map<String, Object> objHead = Collections.singletonMap("head", objHeadTextKey);

        if (bodyText != null && !bodyText.isEmpty()) {
            Map<String, Object> objBodyKeys = new HashMap<>();
            objBodyKeys.put("type", "message");
            objBodyKeys.put("text", bodyText);

            ArrayList<Map<String, Object>> bodyArray = new ArrayList<>();
            bodyArray.add(objBodyKeys);

            Map<String, Object> objBody = Collections.singletonMap("body", bodyArray);
            params.put("content", ConfigurationUtils.deepMerge(objHead, objBody));

        } else {
            params.put("content", objHead);
        }

        if (dryRunMode) {
            log.info("Dry-run mode enabled: Skipping sending of the message");
            return new Result(true, null, null);
        }

        return exec(params, rootApi);
    }

    private Result exec(Map<String, Object> params, String rootApi) throws IOException {
        HttpPost request = new HttpPost(rootApi + Constants.CHAT_POST_MESSAGE_API);
        request.setEntity(new StringEntity(objectMapper.writeValueAsString(params), ContentType.APPLICATION_JSON));

        for (int i = 0; i < retryCount + 1; i++) {
            try (CloseableHttpResponse response = client.execute(request)) {
                if (response.getStatusLine().getStatusCode() == Constants.TOO_MANY_REQUESTS_ERROR) {
                    int retryAfter = getRetryAfter(response);
                    log.warn("exec ['{}', '{}'] -> too many requests, retry after {} sec", Constants.CHAT_POST_MESSAGE_API, params, retryAfter);
                    sleep(retryAfter * 1000L);
                } else {
                    if (response.getEntity() == null) {
                        log.error("exec ['{}', '{}'] -> empty response", Constants.CHAT_POST_MESSAGE_API, params);
                        return new Result(false, "empty response", null);
                    }

                    String s = EntityUtils.toString(response.getEntity());
                    if (response.getStatusLine().getStatusCode() != Constants.ZOOM_SUCCESS_STATUS_CODE) {
                        log.error("exec ['{}', '{}'] -> failed response", Constants.CHAT_POST_MESSAGE_API, params);
                        return new Result(false, s, null);
                    }

                    Result r = new Result(true, null, s);
                    log.info("exec ['{}', '{}'] -> {}", Constants.CHAT_POST_MESSAGE_API, params, r);
                    return r;
                }
            }
        }

        return new Result(false, "too many requests", null);
    }


    private static int getRetryAfter(HttpResponse response) {
        Header h = response.getFirstHeader("Retry-After");
        if (h == null) {
            return Constants.DEFAULT_RETRY_AFTER;
        }

        try {
            return Integer.parseInt(h.getValue());
        } catch (Exception e) {
            log.warn("getRetryAfter -> can't parse retry value '{}'", h.getValue());
            return Constants.DEFAULT_RETRY_AFTER;
        }
    }

    private static void sleep(long t) {
        try {
            Thread.sleep(t);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static PoolingHttpClientConnectionManager createConnManager() {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(new KeyManager[0], new TrustManager[]{new DefaultTrustManager()}, new SecureRandom());

            Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("http", PlainConnectionSocketFactory.INSTANCE)
                    .register("https", new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE))
                    .build();

            return new PoolingHttpClientConnectionManager(registry);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private static CloseableHttpClient createClient(ZoomConfiguration cfg, HttpClientConnectionManager connManager) {
        String accessToken = getAccessToken(cfg, connManager);

        Collection<Header> headers = Collections.singleton(new BasicHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken));
        return HttpClientBuilder.create()
                .setDefaultRequestConfig(createConfig(cfg))
                .setConnectionManager(connManager)
                .setDefaultHeaders(headers)
                .build();
    }

    @SuppressWarnings("unchecked")
    private static String getAccessToken(ZoomConfiguration cfg, HttpClientConnectionManager connManager) {
        String authorization = Base64.getEncoder().encodeToString((cfg.clientId() + ":" + cfg.clientSecret()).getBytes());


        try {
            Collection<Header> headers = Collections.singleton(new BasicHeader(HttpHeaders.AUTHORIZATION, "Basic " + authorization));

            HttpPost request = new HttpPost(cfg.accessTokenApi());
            request.addHeader("content-type", "application/json");
            CloseableHttpClient client = HttpClientBuilder.create()
                    .setDefaultRequestConfig(createConfig(cfg))
                    .setConnectionManager(connManager)
                    .setDefaultHeaders(headers)
                    .build();

            try (CloseableHttpResponse r = client.execute(request)) {

                String responseBody = EntityUtils.toString(r.getEntity());
                if (responseBody != null) {

                    // Getting the status code.
                    int statusCode = r.getStatusLine().getStatusCode();

                    if (statusCode == 200) {
                        ObjectMapper mapper = new ObjectMapper();
                        Map<Object, String> map = mapper.readValue(responseBody, Map.class);
                        return map.get(Constants.VAR_ACCESS_TOKEN);
                    } else {
                        throw new RuntimeException("Error occurred while generating access token" + responseBody);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while generating access token", e);
        }
        return null;
    }

    private static RequestConfig createConfig(ZoomConfiguration cfg) {
        HttpHost proxy = null;
        if (cfg.proxyAddress() != null) {
            proxy = new HttpHost(cfg.proxyAddress(), cfg.proxyPort(), "http");
        }

        return RequestConfig.custom()
                .setConnectTimeout(cfg.connectTimeout())
                .setSocketTimeout(cfg.soTimeout())
                .setProxy(proxy)
                .build();
    }


    private static class DefaultTrustManager implements X509TrustManager {

        @Override
        public void checkClientTrusted(X509Certificate[] x509Certificates, String s) { // NOSONAR
        }

        @Override
        public void checkServerTrusted(X509Certificate[] x509Certificates, String s) { // NOSONAR
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }
    }
}
