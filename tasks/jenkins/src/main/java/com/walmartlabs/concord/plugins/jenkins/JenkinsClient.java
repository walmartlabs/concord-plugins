package com.walmartlabs.concord.plugins.jenkins;

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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.escape.Escaper;
import com.google.common.net.UrlEscapers;
import com.walmartlabs.concord.plugins.jenkins.model.BuildInfo;
import com.walmartlabs.concord.plugins.jenkins.model.Executable;
import com.walmartlabs.concord.plugins.jenkins.model.QueueItem;
import okhttp3.*;

import javax.ws.rs.core.HttpHeaders;
import java.io.File;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class JenkinsClient {

    private static final MediaType APPLICATION_OCTET_STREAM = MediaType.parse("application/octet-stream");
    private static final int SC_CREATED = 201;

    private final OkHttpClient client;
    private final String baseUrl;
    private final String userName;
    private final String apiToken;

    private final ObjectMapper objectMapper;

    public JenkinsClient(JenkinsConfiguration configuration) {
        this.client = createClient(configuration);
        this.baseUrl = configuration.getBaseUrl();
        this.userName = configuration.getUsername();
        this.apiToken = configuration.getApiToken();
        this.objectMapper = new ObjectMapper()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    public String build(String jobName, Map<String, String> params, Map<String, File> fileParams) throws Exception {
        Request request = createBuildRequest(jobName, params, fileParams);

        try (Response response = client.newCall(request).execute();
             ResponseBody body = response.body()) {

            if (!response.isSuccessful() || response.code() != SC_CREATED) {
                String details = body != null ? body.string() : "n/a";
                throw new RuntimeException("Jenkins job '" + jobName + "' build error: " + response.code() + ", details: " + details);
            }

            String queueLink = response.header(HttpHeaders.LOCATION);
            if (queueLink == null || queueLink.trim().isEmpty()) {
                throw new RuntimeException("Can't get queue link for Jenkins job '" + jobName + "'");
            }

            return queueLink;
        }
    }

    public QueueItem getQueueItem(String queueLink) throws Exception {
        return get(queueLink, QueueItem.class);
    }

    public BuildInfo getBuildInfo(Executable executable) throws Exception {
        return get(executable.getUrl(), BuildInfo.class);
    }

    private <E> E get(String url, Class<E> valueType) throws Exception {
        Request request = new Request.Builder()
                .url(toJsonApiUrl(url))
                .addHeader(HttpHeaders.AUTHORIZATION, getAuthHeader())
                .addHeader(HttpHeaders.ACCEPT, "application/json")
                .build();

        try (Response response = client.newCall(request).execute();
             ResponseBody body = response.body()) {

            if (!response.isSuccessful()) {
                throw new RuntimeException("Unexpected code " + response);
            }

            if (body == null) {
                throw new RuntimeException("Empty response");
            }

            return objectMapper.readValue(body.byteStream(), valueType);
        }
    }

    private Request createBuildRequest(String jobName, Map<String, String> params, Map<String, File> fileParams) {
        boolean withParams = !params.isEmpty() || !fileParams.isEmpty();
        String u = withParams ? "/buildWithParameters?" + toQueryString(params) : "/build";

        RequestBody requestBody = RequestBody.create(null, new byte[0]);

        if (fileParams != null && !fileParams.isEmpty()) {
            MultipartBody.Builder requestBodyBuilder = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM);

            for (Map.Entry<String, File> entry : fileParams.entrySet()) {
                requestBodyBuilder.addFormDataPart(entry.getKey(), entry.getValue().getName(),
                        RequestBody.create(APPLICATION_OCTET_STREAM, entry.getValue()));
            }

            requestBody = requestBodyBuilder.build();
        }

        return new Request.Builder()
                .addHeader(HttpHeaders.AUTHORIZATION, getAuthHeader())
                .url(this.baseUrl + "/job/" + UrlEscapers.urlFragmentEscaper().escape(jobName) + u)
                .post(requestBody)
                .build();
    }

    private String getAuthHeader() {
        return "Basic " + Base64.getEncoder().encodeToString((this.userName + ":" + this.apiToken).getBytes());
    }

    private static String toQueryString(Map<String, String> params) {
        List<String> p = params.entrySet().stream()
                .map(MapEntryToQueryStringPair::apply)
                .collect(Collectors.toList());

        return String.join("&", p);
    }

    private static String toJsonApiUrl(String path) {
        String p = path;

        if (!p.contains("?")) {
            p = Utils.normalizeUrl(p) + "/api/json";
        } else {
            final String[] components = p.split("\\?", 2);
            p = Utils.normalizeUrl(components[0]) + "/api/json" + "?" + components[1];
        }
        return p.replace(" ", "%20");
    }

    private OkHttpClient createClient(JenkinsConfiguration cfg) {
        return new OkHttpClient.Builder()
                .connectTimeout(cfg.getConnectTimeout(), TimeUnit.SECONDS)
                .readTimeout(cfg.getReadTimeout(), TimeUnit.SECONDS)
                .writeTimeout(cfg.getWriteTimeout(), TimeUnit.SECONDS)
                .build();
    }

    private static class MapEntryToQueryStringPair {

        static String apply(Map.Entry<String, String> entry) {
            Escaper escaper = UrlEscapers.urlFormParameterEscaper();
            return escaper.escape(entry.getKey()) + "=" + escaper.escape(entry.getValue());
        }
    }
}
