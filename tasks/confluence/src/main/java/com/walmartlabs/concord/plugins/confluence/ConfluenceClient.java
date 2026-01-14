package com.walmartlabs.concord.plugins.confluence;

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


import com.fasterxml.jackson.databind.ObjectMapper;
import com.walmartlabs.concord.plugins.confluence.model.auth.Auth;
import com.walmartlabs.concord.plugins.confluence.model.auth.AuthUtils;
import com.walmartlabs.concord.plugins.confluence.model.auth.BasicAuth;
import okhttp3.*;
import okhttp3.logging.HttpLoggingInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class ConfluenceClient {

    private static final Logger log = LoggerFactory.getLogger(ConfluenceClient.class);

    private static final ObjectMapper mapper = new ObjectMapper();

    private final OkHttpClient client;

    private final String apiUrl;
    private final Auth auth;
    private String url;

    public ConfluenceClient(TaskParams in) {
        this.apiUrl = in.apiUrl();

        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                .connectTimeout(in.connectTimeout(), TimeUnit.SECONDS)
                .readTimeout(in.readTimeout(), TimeUnit.SECONDS)
                .writeTimeout(in.writeTimeout(), TimeUnit.SECONDS);
        if (in.debug()) {
            Interceptor logging = new HttpLoggingInterceptor(log::info)
                    .setLevel(HttpLoggingInterceptor.Level.BODY);
            clientBuilder.addInterceptor(logging);
        }
        this.client = clientBuilder.build();
        this.auth = AuthUtils.parseAuth(in, mapper);
    }

    public ConfluenceClient url(String url) {
        this.url = url;
        return this;
    }

    public Map<String, Object> post(Map<String, Object> data) throws IOException {
        RequestBody body = RequestBody.create(
                MediaType.parse(Constants.CLIENT_MEDIATYPE_JSON), mapper.writeValueAsString(data));
        Request request = requestBuilder()
                .url(buildRequestUrl(apiUrl, url))
                .post(body)
                .build();

        return call(request);
    }

    public void put(Map<String, Object> data) throws IOException {
        RequestBody body = RequestBody.create(
                MediaType.parse(Constants.CLIENT_MEDIATYPE_JSON), mapper.writeValueAsString(data));
        Request request = requestBuilder()
                .url(buildRequestUrl(apiUrl, url))
                .put(body)
                .build();

        call(request);
    }

    public void delete() throws IOException {
        Request request = requestBuilder()
                .url(buildRequestUrl(apiUrl, url))
                .delete()
                .build();

        call(request);
    }

    public Map<String, Object> getWithQueryParams(String param1, String value1, String param2, String value2) throws IOException {
        HttpUrl httpUrl = HttpUrl.parse(buildRequestUrl(apiUrl, url));
        String address = "";
        if (httpUrl != null) {
            HttpUrl.Builder urlBuilder = httpUrl.newBuilder();
            urlBuilder.addQueryParameter(param1, value1);
            urlBuilder.addQueryParameter(param2, value2);

            address = urlBuilder.build().toString();
        }
        Request request = requestBuilder()
                .url(address)
                .build();

        return call(request);
    }

    public void postFormData(String comment, File file) throws IOException {
        RequestBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(Constants.CONFLUENCE_ENTITY_TYPE_COMMENT, comment)
                .addFormDataPart(Constants.CONFLUENCE_ENTITY_TYPE_FILE, file.getName(),
                        RequestBody.create(MediaType.parse(Constants.CLIENT_MEDIATYPE_JSON), file))
                .build();

        Request request = new Request.Builder()
                .url(buildRequestUrl(apiUrl, url))
                .post(body)
                .addHeader(Constants.CLIENT_HEADER_AUTH, getAuthorizationValue())
                .addHeader(Constants.CONFLUENCE_ENTITY_TOKEN_KEY, Constants.CONFLUENCE_ENTITY_TOKEN_VALUE)
                .build();

        call(request);
    }

    public Map<String, Object> get() throws IOException {
        Request request = requestBuilder()
                .url(buildRequestUrl(apiUrl, url))
                .get()
                .build();

        return call(request);
    }

    private Request.Builder requestBuilder() {
        return new Request.Builder()
                .addHeader(Constants.CLIENT_HEADER_AUTH, getAuthorizationValue())
                .addHeader(Constants.CLIENT_HEADER_ACCEPT, Constants.CLIENT_MEDIATYPE_JSON);
    }

    String getAuthorizationValue() {
        if (auth.accessToken() != null) {
            return "Bearer " + auth.accessToken();
        }

        if (auth.basic() != null) {
            BasicAuth basic = Objects.requireNonNull(auth.basic());
            String user = basic.username();
            String pass = basic.password();
            return "Basic " + Base64.getEncoder().encodeToString((user + ":" + pass).getBytes());
        }

        throw new IllegalArgumentException("Invalid 'auth' input.");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> call(Request request) throws IOException {
        Call call = client.newCall(request);
        Response response = call.execute();
        int statusCode = response.code();
        try (ResponseBody responseBody = response.body()) {
            String results = null;
            if (responseBody != null) {
                results = responseBody.string();
            }
            assertResponseCode(statusCode, results, response);
            if (results != null && !results.isEmpty()) {
                return mapper.readValue(results, Map.class);
            }
            return null;
        }
    }

    private static void assertResponseCode(int code, String result, Response response) {
        if (response.isSuccessful()) {
            return;
        }
        if (code == 400) {
            throw new RuntimeException("Input is invalid (e.g. missing required fields, invalid values). Here are the full error details: " + result);
        } else if (code == 401) {
            throw new RuntimeException("User is not authenticated. Here are the full error details: " + result);
        } else if (code == 403) {
            throw new RuntimeException("User does not have permission to perform request. Here are the full error details: " + result);
        } else if (code == 404) {
            throw new RuntimeException("Request Failed. Here are the full error details: " + result);
        } else if (code == 500) {
            throw new RuntimeException("Internal Server Error. Here are the full error details" + result);
        } else {
            throw new RuntimeException("Error: " + result);
        }
    }

    private static String buildRequestUrl(String apiUrl, String url) {
        return apiUrl + url;
    }
}
