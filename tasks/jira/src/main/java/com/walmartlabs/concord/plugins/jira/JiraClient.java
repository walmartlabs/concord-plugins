package com.walmartlabs.concord.plugins.jira;

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

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.walmartlabs.concord.sdk.ContextUtils.getNumber;

public class JiraClient {

    private static final OkHttpClient client = new OkHttpClient();
    private static final Gson gson = new GsonBuilder().create();

    private static final String CLIENT_CONNECTTIMEOUT = "connectTimeout";
    private static final String CLIENT_READTIMEOUT = "readTimeout";
    private static final String CLIENT_WRITETIMEOUT = "writeTimeout";

    private final Context ctx;
    private String url;
    private int successCode;
    private String auth;

    public JiraClient(Context ctx) {
        this.ctx = ctx;
    }

    public JiraClient url(String url) {
        this.url = url;
        return this;
    }

    public JiraClient successCode(int successCode) {
        this.successCode = successCode;
        return this;
    }

    public JiraClient jiraAuth(String auth) {
        this.auth = auth;
        return this;
    }

    public Map<String, Object> post(Map<String, Object> data) throws IOException {
        RequestBody body = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"), gson.toJson(data));
        Request request = requestBuilder(auth)
                .url(url)
                .post(body)
                .build();

        return call(request);
    }

    public void put(Map<String, Object> data) throws IOException {
        RequestBody body = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"), gson.toJson(data));
        Request request = requestBuilder(auth)
                .url(url)
                .put(body)
                .build();

        call(request);
    }

    public void delete() throws IOException {
        Request request = requestBuilder(auth)
                .url(url)
                .delete()
                .build();

        call(request);
    }

    private static Request.Builder requestBuilder(String auth) {
        return new Request.Builder()
                .addHeader("Authorization", auth)
                .addHeader("Accept", "application/json");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> call(Request request) throws IOException {
        //set client timeouts
        setClientTimeoutParams(ctx);

        Call call = client.newCall(request);
        Response response = call.execute();
        int statusCode = response.code();
        try (ResponseBody responseBody = response.body()) {
            String results = null;
            if (responseBody != null) {
                results = responseBody.string();
            }

            assertResponseCode(statusCode, results, successCode);

            return gson.fromJson(results, Map.class);
        }
    }

    private static void assertResponseCode(int code, String result, int successCode) {
        if (code == successCode) {
            return;
        }

        if (code == 400) {
            throw new RuntimeException("input is invalid (e.g. missing required fields, invalid values). Here are the full error details: " + result);
        } else if (code == 401) {
            throw new RuntimeException("User is not authenticated. Here are the full error details: " + result);
        } else if (code == 403) {
            throw new RuntimeException("User does not have permission to perform request. Here are the full error details: " + result);
        } else if (code == 404) {
            throw new RuntimeException("Issue does not exist. Here are the full error details: " + result);
        } else if (code == 500) {
            throw new RuntimeException("Internal Server Error. Here are the full error details" + result);
        } else {
            throw new RuntimeException("Error: " + result);
        }
    }

    private static void setClientTimeoutParams(Context ctx) {
        long connectTimeout = getNumber(ctx, CLIENT_CONNECTTIMEOUT, 30L).longValue();
        long readTimeout = getNumber(ctx, CLIENT_READTIMEOUT, 30L).longValue();
        long writeTimeout = getNumber(ctx, CLIENT_WRITETIMEOUT, 30L).longValue();

        client.setConnectTimeout(connectTimeout, TimeUnit.SECONDS);
        client.setReadTimeout(readTimeout, TimeUnit.SECONDS);
        client.setWriteTimeout(writeTimeout, TimeUnit.SECONDS);
    }
}
