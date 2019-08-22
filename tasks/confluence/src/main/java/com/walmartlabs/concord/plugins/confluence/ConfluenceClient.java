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
import com.walmartlabs.concord.sdk.Context;
import okhttp3.*;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.walmartlabs.concord.sdk.ContextUtils.assertString;
import static com.walmartlabs.concord.sdk.ContextUtils.getNumber;

@SuppressWarnings("Duplicates")
public class ConfluenceClient {

    private static final OkHttpClient client = new OkHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();

    private static final String CONFLUENCE_PWD = "password";
    private static final String CONFLUENCE_UID = "userId";

    private static final String CLIENT_CONNECTTIMEOUT = "connectTimeout";
    private static final String CLIENT_READTIMEOUT = "readTimeout";
    private static final String CLIENT_WRITETIMEOUT = "writeTimeout";

    private final Context ctx;
    private String url;

    public ConfluenceClient(Context ctx) {
        this.ctx = ctx;
    }

    public ConfluenceClient url(String url) {
        this.url = url;
        return this;
    }

    public Map<String, Object> post(Map<String, Object> data) throws IOException {
        RequestBody body = RequestBody.create(
                MediaType.parse(Constants.CLIENT_MEDIATYPE_JSON), mapper.writeValueAsString(data));
        Request request = requestBuilder(ctx)
                .url(url)
                .post(body)
                .build();

        return call(request);
    }

    public void put(Map<String, Object> data) throws IOException {
        RequestBody body = RequestBody.create(
                MediaType.parse(Constants.CLIENT_MEDIATYPE_JSON), mapper.writeValueAsString(data));
        Request request = requestBuilder(ctx)
                .url(url)
                .put(body)
                .build();

        call(request);
    }

    public void delete() throws IOException {
        Request request = requestBuilder(ctx)
                .url(url)
                .delete()
                .build();

        call(request);
    }

    public Map<String, Object> getWithQueryParams(String param1, String value1, String param2, String value2) throws IOException {
        HttpUrl httpUrl = HttpUrl.parse(url);
        String address = "";
        if (httpUrl != null) {
            HttpUrl.Builder urlBuilder = httpUrl.newBuilder();
            urlBuilder.addQueryParameter(param1, value1);
            urlBuilder.addQueryParameter(param2, value2);

            address = urlBuilder.build().toString();
        }
        Request request = requestBuilder(ctx)
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
                .url(url)
                .post(body)
                .addHeader(Constants.CLIENT_HEADER_AUTH, buildAuth(ctx))
                .addHeader(Constants.CONFLUENCE_ENTITY_TOKEN_KEY, Constants.CONFLUENCE_ENTITY_TOKEN_VALUE)
                .build();

        call(request);
    }

    public Map<String, Object> get() throws IOException {
        Request request = requestBuilder(ctx)
                .url(url)
                .get()
                .build();

        return call(request);
    }

    private static Request.Builder requestBuilder(Context ctx) {
        return new Request.Builder()
                .addHeader(Constants.CLIENT_HEADER_AUTH, buildAuth(ctx))
                .addHeader(Constants.CLIENT_HEADER_ACCEPT, Constants.CLIENT_MEDIATYPE_JSON);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> call(Request request) throws IOException {
        setClientTimeoutParams(ctx);

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

    private static String buildAuth(Context ctx) {
        String uid = assertString(ctx, CONFLUENCE_UID);
        String pwd = assertString(ctx, CONFLUENCE_PWD);
        return Credentials.basic(uid, pwd);
    }

    private static void setClientTimeoutParams(Context ctx) {
        long connectTimeout = getNumber(ctx, CLIENT_CONNECTTIMEOUT, Constants.CONNECTION_TIMEOUT).longValue();
        long readTimeout = getNumber(ctx, CLIENT_READTIMEOUT, Constants.READ_TIMEOUT).longValue();
        long writeTimeout = getNumber(ctx, CLIENT_WRITETIMEOUT, Constants.WRITE_TIMEOUT).longValue();

        client.newBuilder().
                connectTimeout(connectTimeout, TimeUnit.SECONDS)
                .readTimeout(readTimeout, TimeUnit.SECONDS)
                .writeTimeout(writeTimeout, TimeUnit.SECONDS);
    }
}
