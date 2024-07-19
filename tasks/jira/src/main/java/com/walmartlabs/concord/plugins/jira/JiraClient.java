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

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.MultipartBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class JiraClient implements JiraHttpClient {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new Jdk8Module());
    static final JavaType MAP_TYPE = MAPPER.getTypeFactory()
            .constructMapType(HashMap.class, String.class, Object.class);
    private static final JavaType LIST_OF_MAPS_TYPE = MAPPER.getTypeFactory()
            .constructCollectionType(List.class, MAP_TYPE);

    private static final OkHttpClient client = new OkHttpClient();
    private final JiraClientCfg cfg;
    private URI uri;
    private int successCode;
    private String auth;

    public JiraClient(JiraClientCfg cfg) {
        this.cfg = cfg;
    }

    @Override
    public JiraHttpClient url(String url) {
        this.uri = URI.create(url);
        return this;
    }

    @Override
    public JiraHttpClient successCode(int successCode) {
        this.successCode = successCode;
        return this;
    }

    @Override
    public JiraHttpClient jiraAuth(String auth) {
        this.auth = auth;
        return this;
    }

    @Override
    public Map<String, Object> get() throws IOException {
        Request request = requestBuilder(auth)
                .url(uri.toURL())
                .get()
                .build();

        return call(request, MAP_TYPE);
    }

    @Override
    public Map<String, Object> post(Map<String, Object> data) throws IOException {
        RequestBody body = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"), MAPPER.writeValueAsString(data));
        Request request = requestBuilder(auth)
                .url(uri.toURL())
                .post(body)
                .build();

        return call(request, MAP_TYPE);
    }

    @Override
    public void post(File file) throws IOException {
        MultipartBuilder b = new MultipartBuilder(Constants.BOUNDARY).type(MultipartBuilder.FORM);
        b.addFormDataPart("file", file.getName(),
                RequestBody.create(MediaType.parse("application/octet-stream"), Files.readAllBytes(file.toPath())));

        RequestBody body = b.build();
        Request request = requestBuilder(auth)
                .header("X-Atlassian-Token", "nocheck")
                .url(uri.toURL())
                .post(body)
                .build();

        call(request, LIST_OF_MAPS_TYPE);
    }

    @Override
    public void put(Map<String, Object> data) throws IOException {
        RequestBody body = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"), MAPPER.writeValueAsString(data));
        Request request = requestBuilder(auth)
                .url(uri.toURL())
                .put(body)
                .build();

        call(request, MAP_TYPE);
    }

    @Override
    public void delete() throws IOException {
        Request request = requestBuilder(auth)
                .url(uri.toURL())
                .delete()
                .build();

        call(request, MAP_TYPE);
    }

    private static Request.Builder requestBuilder(String auth) {
        return new Request.Builder()
                .addHeader("Authorization", auth)
                .addHeader("Accept", "application/json");
    }


    <T> T call(Request request, JavaType returnType) throws IOException {
        setClientTimeoutParams(cfg);

        Call call = client.newCall(request);
        Response response = call.execute();

        try (ResponseBody responseBody = response.body()) {
            String results = null;
            if (responseBody != null) {
                results = responseBody.string();
            }

            int statusCode = response.code();
            JiraHttpClient.assertResponseCode(statusCode, results, successCode);

            if (results == null || statusCode == 204) {
                return null;
            } else {
                return MAPPER.readValue(results, returnType);
            }
        }
    }

    private static void setClientTimeoutParams(JiraClientCfg cfg) {
        client.setConnectTimeout(cfg.connectTimeout(), TimeUnit.SECONDS);
        client.setReadTimeout(cfg.readTimeout(), TimeUnit.SECONDS);
        client.setWriteTimeout(cfg.writeTimeout(), TimeUnit.SECONDS);
    }
}
