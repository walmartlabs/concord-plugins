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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.okhttp.*;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class GremlinClient {

    private static final OkHttpClient client = new OkHttpClient();
    private static final Gson gson = new GsonBuilder().create();

    private final GremlinClientParams params;

    private String url;
    private int successCode;
    private String teamId;

    public GremlinClient(GremlinClientParams params) {
        this.params = params;
    }

    public GremlinClient url(String url) {
        this.url = params.apiUrl() + url;
        return this;
    }

    public GremlinClient successCode(int successCode) {
        this.successCode = successCode;
        return this;
    }

    public GremlinClient teamId(String teamId) {
        this.teamId = teamId;
        return this;
    }

    public Map<String, Object> post(Map<String, Object> data) throws IOException {
        RequestBody body = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"), gson.toJson(data));

        HttpUrl.Builder urlBuilder = HttpUrl.parse(url).newBuilder();
        if (teamId != null) {
            urlBuilder.addQueryParameter("teamId", teamId);
        }

        Request request = requestBuilder()
                .url(urlBuilder.build())
                .post(body)
                .build();

        return call(request);
    }

    public Map<String, Object> get() throws IOException {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(url).newBuilder();
        if (teamId != null) {
            urlBuilder.addQueryParameter("teamId", teamId);
        }

        Request request = getRequestBuilder()
                .url(urlBuilder.build())
                .get()
                .build();
        return call(request);
    }

    public void delete() throws IOException {
        Request request = getRequestBuilder()
                .url(url)
                .delete()
                .build();
        deleteCall(request);
    }

    private Request.Builder requestBuilder() {
        return new Request.Builder()
                .addHeader("Content-Type", "application/json")
                .addHeader("X-Gremlin-Agent", "concord/" + Version.getVersion())
                .addHeader("Authorization", "Key " + params.apiKey());
    }

    private Request.Builder getRequestBuilder() {
        return new Request.Builder()
                .addHeader("X-Gremlin-Agent", "concord/" + Version.getVersion())
                .addHeader("Authorization", "Key " + params.apiKey());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> call(Request request) throws IOException {
        setupClientParams();

        Response response = getClientResponse(request);
        int statusCode = response.code();
        try (ResponseBody responseBody = response.body()) {
            String results = null;
            if (responseBody != null) {
                results = responseBody.string();
            }
            Map<String, Object> objResults = Collections.singletonMap("results", results);
            assertResponseCode(statusCode, results, successCode);

            return gson.fromJson(objResults.toString(), Map.class);
        }
    }

    private void deleteCall(Request request) throws IOException {
        setupClientParams();

        Response response = getClientResponse(request);
        int statusCode = response.code();
        try (ResponseBody responseBody = response.body()) {
            String results = null;
            if (responseBody != null) {
                results = responseBody.string();
            }
            assertResponseCode(statusCode, results, successCode);
        }
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

    private Response getClientResponse(Request request) {
        Response response;
        try {
            response = client.newCall(request).execute();
        } catch (Exception e) {
            throw new RuntimeException("Error: " + e);
        }
        return response;
    }


    private void setupClientParams() {
        try {
            client.setConnectTimeout(params.connectTimeout(), TimeUnit.SECONDS);
            client.setReadTimeout(params.readTimeout(), TimeUnit.SECONDS);
            client.setWriteTimeout(params.writeTimeout(), TimeUnit.SECONDS);

            if (params.useProxy()) {
                // Create a trust manager that does not validate certificate chains
                final TrustManager[] trustAllCerts = new TrustManager[]{
                        new X509TrustManager() {
                            @Override
                            public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                            }

                            @Override
                            public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                            }

                            @Override
                            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                                return null;
                            }
                        }
                };

                // Install the all-trusting trust manager
                final SSLContext sslContext = SSLContext.getInstance("SSL");
                sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

                // Create an ssl socket factory with our all-trusting manager
                final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
                client.setProxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(params.proxyHost(), params.proxyPort())));
                client.setSslSocketFactory(sslSocketFactory);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error: " + e);
        }
    }
}

