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
import com.walmartlabs.concord.sdk.Context;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.security.cert.CertificateException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.walmartlabs.concord.sdk.ContextUtils.assertString;
import static com.walmartlabs.concord.sdk.ContextUtils.getBoolean;


public class GremlinClient {

    private static final OkHttpClient client = new OkHttpClient();
    private static final Gson gson = new GsonBuilder().create();
    private static final String GREMLIN_API_KEY = "apiKey";
    private static final String PROXY = "useProxy";

    private final Context ctx;

    private String url;
    private int successCode;

    public GremlinClient(Context ctx) {
        this.ctx = ctx;
    }

    public GremlinClient url(String url) {
        this.url = url;
        return this;
    }

    public GremlinClient successCode(int successCode) {
        this.successCode = successCode;
        return this;
    }

    public Map<String, Object> post(Map<String, Object> data) throws IOException {
        RequestBody body = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"), gson.toJson(data));
        Request request = requestBuilder(ctx)
                .url(url)
                .post(body)
                .build();

        return call(request);
    }

    public Map<String, Object> get() throws IOException {
        Request request = getRequestBuilder(ctx)
                .url(url)
                .get()
                .build();
        return call(request);
    }

    public void delete() throws IOException {
        Request request = getRequestBuilder(ctx)
                .url(url)
                .delete()
                .build();
        deleteCall(request);
    }

    private static Request.Builder requestBuilder(Context ctx) {
        String apiKey = assertString(ctx, GREMLIN_API_KEY);
        return new Request.Builder()
                .addHeader("Content-Type", "application/json")
                .addHeader("X-Gremlin-Agent", "concord/" + Version.getVersion())
                .addHeader("Authorization", "Key " + apiKey);
    }

    private static Request.Builder getRequestBuilder(Context ctx) {
        String apiKey = assertString(ctx, GREMLIN_API_KEY);
        return new Request.Builder()
                .addHeader("X-Gremlin-Agent", "concord/" + Version.getVersion())
                .addHeader("Authorization", "Key " + apiKey);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> call(Request request) throws IOException {
        //setup client params
        setupClientParams(ctx);

        Response response = getClientResponse(request);
        int statusCode = response.code();
        try (ResponseBody responseBody = response.body()) {
            String results = null;
            if (responseBody != null) {
                results = responseBody.string();
            }
            Map<String, Object> objResults = Collections.singletonMap("results", results);
            gson.toJson(objResults);
            assertResponseCode(statusCode, objResults.toString(), successCode);

            return gson.fromJson(objResults.toString(), Map.class);
        }
    }

    private void deleteCall(Request request) throws IOException {
        //setup client params
        setupClientParams(ctx);

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


    private static void setupClientParams(Context ctx) {
        Map<String, Object> cfg = GremlinTask.createCfg(ctx);

        long connectTimeout = Long.parseLong(cfg.get("connectTimeout").toString());
        long readTimeout = Long.parseLong(cfg.get("readTimeout").toString());
        long writeTimeout = Long.parseLong(cfg.get("writeTimeout").toString());
        boolean useProxy = getBoolean(ctx, PROXY, false);

        try {
            client.setConnectTimeout(connectTimeout, TimeUnit.SECONDS);
            client.setReadTimeout(readTimeout, TimeUnit.SECONDS);
            client.setWriteTimeout(writeTimeout, TimeUnit.SECONDS);

            if (useProxy) {
                String proxyHost = cfg.get("proxyHost").toString();
                int proxyPort = Integer.parseInt(cfg.get("proxyPort").toString());

                // Create a trust manager that does not validate certificate chains
                final TrustManager[] trustAllCerts = new TrustManager[]{
                        new X509TrustManager() {
                            @Override
                            public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                            }

                            @Override
                            public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
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
                client.setProxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort)));
                client.setSslSocketFactory(sslSocketFactory);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error: " + e);
        }
    }
}

