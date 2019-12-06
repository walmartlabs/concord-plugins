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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.walmartlabs.concord.sdk.MapUtils;
import org.apache.http.Header;
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
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TeamsClient implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(TeamsClient.class);
    private final int retryCount;
    private final PoolingHttpClientConnectionManager connManager;
    private final CloseableHttpClient client;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public TeamsClient(TeamsConfiguration cfg) {
        this.retryCount = cfg.getRetryCount();
        this.connManager = createConnManager();
        this.client = createClient(cfg, connManager);
    }

    @Override
    public void close() throws IOException {
        client.close();
        connManager.close();
    }

    public Result message(Map<String, Object> cfg, String title, String text, String themeColor,
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

    private Result exec(Map<String, Object> cfg, Map<String, Object> params) throws IOException {

        String teamId = MapUtils.getString(cfg, Constants.TEAM_ID_KEY, null);
        String webhookId = MapUtils.getString(cfg, Constants.WEBHOOK_ID_KEY, null);
        String webhookUrl = MapUtils.getString(cfg, Constants.WEBHOOK_URL_KEY, null);

        HttpPost request;

        if ((teamId != null && !teamId.isEmpty()) && (webhookId != null && !webhookId.isEmpty())) {
            webhookUrl = cfg.get(Constants.ROOT_WEBHOOK_URL_KEY) + teamId + "@" + cfg.get(Constants.TENANT_ID_KEY) + "/IncomingWebhook/" + webhookId + "/" + cfg.get(Constants.WEBHOOKTYPE_ID_KEY);
            request = new HttpPost(webhookUrl);
        } else if (webhookUrl != null && !webhookUrl.isEmpty()) {
            request = new HttpPost(webhookUrl);
        } else {
            throw new IllegalArgumentException("Mandatory parameters 'teamId & webhookId' or 'webhookUrl' is required for the execution of 'msteams' task");
        }
        request.setEntity(new StringEntity(objectMapper.writeValueAsString(params), ContentType.APPLICATION_JSON));

        for (int i = 0; i < retryCount + 1; i++) {
            try (CloseableHttpResponse response = client.execute(request)) {
                if (response.getStatusLine().getStatusCode() == Constants.TOO_MANY_REQUESTS_ERROR) {
                    int retryAfter = getRetryAfter(response);
                    log.warn("exec [webhookUrl: '{}', params: '{}'] -> too many requests, retry after {} sec", webhookUrl, params, retryAfter);
                    sleep(retryAfter * 1000L);
                } else {
                    if (response.getEntity() == null) {
                        log.error("exec [webhookUrl: '{}', params: '{}'] -> empty response", webhookUrl, params);
                        return new Result(false, "empty response", null);
                    }

                    String s = EntityUtils.toString(response.getEntity());
                    if (response.getStatusLine().getStatusCode() != Constants.TEAMS_SUCCESS_STATUS_CODE) {
                        log.error("exec [webhookUrl: '{}', params: '{}'] -> failed response", webhookUrl, params);
                        return new Result(false, s, null);
                    }

                    Result r = new Result(true, null, s);
                    log.info("exec [webhookUrl: '{}', params: '{}'] -> {}", webhookUrl, params, r);
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

    @SuppressWarnings("Duplicates")
    private static PoolingHttpClientConnectionManager createConnManager() {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
            sslContext.init(new KeyManager[0], new TrustManager[]{new DefaultTrustManager()}, new SecureRandom());

            Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("http", PlainConnectionSocketFactory.INSTANCE)
                    .register("https", new SSLConnectionSocketFactory(sslContext, SSLConnectionSocketFactory.getDefaultHostnameVerifier()))
                    .build();

            return new PoolingHttpClientConnectionManager(registry);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static CloseableHttpClient createClient(TeamsConfiguration cfg, HttpClientConnectionManager connManager) {
        return HttpClientBuilder.create()
                .setDefaultRequestConfig(createConfig(cfg))
                .setConnectionManager(connManager)
                .build();
    }

    private static RequestConfig createConfig(TeamsConfiguration cfg) {
        HttpHost proxy = null;
        if (cfg.getProxyAddress() != null) {
            proxy = new HttpHost(cfg.getProxyAddress(), cfg.getProxyPort(), "http");
        }

        return RequestConfig.custom()
                .setConnectTimeout(cfg.getConnectTimeout())
                .setSocketTimeout(cfg.getSoTimeout())
                .setProxy(proxy)
                .build();
    }

    private static class DefaultTrustManager implements X509TrustManager {

        @Override
        public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException { // NOSONAR
        }

        @Override
        public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException { // NOSONAR
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }
    }
}
