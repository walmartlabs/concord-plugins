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
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.*;

public class TeamsClientV2 implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(TeamsClient.class);

    private final int retryCount;
    private final PoolingHttpClientConnectionManager connManager;
    private final CloseableHttpClient client;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public TeamsClientV2(TeamsConfiguration cfg, boolean useProxy) {
        this.retryCount = cfg.getRetryCount();
        this.connManager = TeamsClient.createConnManager();
        this.client = createClient(cfg, connManager, useProxy);
    }

    @Override
    public void close() throws IOException {
        client.close();
        connManager.close();
    }

    public Result createConversation(Map<String, Object> cfg, Map<String, Object> activity,
                                     String channelId, String rootApi) throws IOException {

        Map<String, Object> params = new HashMap<>();
        params.put("activity", activity);
        params.put("tenantId", MapUtils.getString(cfg, Constants.TENANT_ID_KEY));

        Map<String, Object> channel = Collections.singletonMap("id", URLDecoder.decode(channelId, "UTF-8"));
        Map<String, Object> channelData = Collections.singletonMap("channel", channel);
        params.put("channelData", channelData);
        return exec(params, rootApi);
    }

    public Result replyToConversation(Map<String, Object> activity, String rootApi, String conversationId) throws IOException {
        rootApi = rootApi + "/" + conversationId + "/activities";
        return exec(activity, rootApi);
    }

    private Result exec(Map<String, Object> params, String rootApi) throws IOException {
        HttpPost request = new HttpPost(rootApi);
        request.setEntity(new StringEntity(objectMapper.writeValueAsString(params), ContentType.APPLICATION_JSON));

        for (int i = 0; i < retryCount + 1; i++) {
            try (CloseableHttpResponse response = client.execute(request)) {
                if (response.getStatusLine().getStatusCode() == Constants.TOO_MANY_REQUESTS_ERROR) {
                    int retryAfter = TeamsClient.getRetryAfter(response);
                    log.warn("exec [params: '{}'] -> too many requests, retry after {} sec", params, retryAfter);
                    TeamsClient.sleep(retryAfter * 1000L);
                } else {
                    if (response.getEntity() == null) {
                        log.error("exec [params: '{}'] -> empty response", params);
                        return new Result(false, "empty response", null, null, null);
                    }

                    String s = EntityUtils.toString(response.getEntity());
                    if (response.getStatusLine().getStatusCode() != Constants.TEAMS_SUCCESS_STATUS_CODE_V2) {
                        log.error("exec [params: '{}'] -> failed response", params);
                        return new Result(false, s, null, null, null);
                    }


                    Result r;
                    if (objectMapper.readTree(s).has("activityId")) {
                        String conversationId = objectMapper.readTree(s).get("id").toString().
                                replace("\"", "");
                        String activityId = objectMapper.readTree(s).get("activityId").toString().
                                replace("\"", "");

                        r = new Result(true, null, s, conversationId, activityId);
                        log.info("exec [params: '{}'] -> {}", params, r);
                        return r;
                    }
                    r = new Result(true, null, s, null, null);
                    log.info("exec [params: '{}'] -> {}", params, r);
                    return r;
                }
            }
        }

        return new Result(false, "too many requests", null, null, null);
    }

    private static CloseableHttpClient createClient(TeamsConfiguration cfg, HttpClientConnectionManager connManager,
                                                    boolean useProxy) {
        String accessToken = generateAccessToken(cfg);

        Collection<Header> headers = Collections.singleton(new BasicHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken));
        return HttpClientBuilder.create()
                .setDefaultRequestConfig(createConfig(cfg, useProxy))
                .setConnectionManager(connManager)
                .setDefaultHeaders(headers)
                .build();
    }

    public static RequestConfig createConfig(TeamsConfiguration cfg, boolean useProxy) {
        HttpHost proxy = null;
        if (useProxy) {
            proxy = new HttpHost(cfg.getProxyAddress(), cfg.getProxyPort(), "http");
        }

        return RequestConfig.custom()
                .setConnectTimeout(cfg.getConnectTimeout())
                .setSocketTimeout(cfg.getSoTimeout())
                .setProxy(proxy)
                .build();
    }

    @SuppressWarnings("unchecked")
    private static String generateAccessToken(TeamsConfiguration cfg) {
        List<NameValuePair> data = new ArrayList<>();
        data.add(new BasicNameValuePair("client_id", cfg.getClientId()));
        data.add(new BasicNameValuePair("client_secret", cfg.getClientSecret()));
        data.add(new BasicNameValuePair("scope", Constants.API_BOT_FRAMEWORK_SCOPE));
        data.add(new BasicNameValuePair("grant_type", Constants.API_BOT_FRAMEWORK_GRANT_TYPE));

        try {
            HttpPost request = new HttpPost(cfg.getAccessTokenApi());
            request.addHeader("content-type", "application/x-www-form-urlencoded");
            request.setEntity(new UrlEncodedFormEntity(data));
            CloseableHttpClient client = HttpClientBuilder.create().build();

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
                        throw new RuntimeException("Error while generating access token" + responseBody);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error while generating access token", e);
        }
        return null;
    }
}
