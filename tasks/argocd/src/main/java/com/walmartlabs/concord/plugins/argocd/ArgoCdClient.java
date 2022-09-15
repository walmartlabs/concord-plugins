/**
 * Concord
 * -----
 * Copyright (C) 2017 - 2021 Walmart Inc.
 * -----
 */
package com.walmartlabs.concord.plugins.argocd;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2022 Walmart Inc., Concord Authors
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

import com.walmartlabs.concord.plugins.argocd.model.*;
import okhttp3.*;
import okhttp3.logging.HttpLoggingInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static okhttp3.internal.Util.EMPTY_REQUEST;

public class ArgoCdClient {

    private final static Logger log = LoggerFactory.getLogger(ArgoCdClient.class);

    public static final MediaType APPLICATION_JSON = MediaType.parse("application/json");
    private static final String AUTHORIZATION = "Authorization";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private OkHttpClient client;
    private final String baseUrl;

    public ArgoCdClient(TaskParams in) {
        this.client = createClient(in);
        this.baseUrl = in.baseUrl();
    }

    public String auth(TaskParams.AuthParams in) throws IOException {
        if (in instanceof TaskParams.BasicAuth) {
            return BasicAuthHandler.auth(this, (TaskParams.BasicAuth) in);
        } else if(in instanceof TaskParams.LdapAuth) {
            TokenCookieJar tokenCookieJar = LdapAuthHandler.auth(this, (TaskParams.LdapAuth) in);
            this.client = client.newBuilder().cookieJar(tokenCookieJar).build();
            return tokenCookieJar.token;
        } else {
            throw new IllegalArgumentException("Unknown auth type: " + in);
        }
    }

    public Application getApp(String token, String app, boolean refresh) throws IOException {
        HttpUrl.Builder urlBuilder = urlBuilder("api/v1/applications/" + app);
        if(refresh) {
            urlBuilder.addQueryParameter("refresh", "normal");
        }

        Request.Builder rb = new Request.Builder()
                .url(urlBuilder.build())
                .header(AUTHORIZATION, authValue(token))
                .get();

        return exec(rb.build(), response -> objectMapper.readValue(response.byteStream(), Application.class));
    }
    
    public void deleteApp(String token, String app, boolean cascade, String propagationPolicy) throws IOException {
        HttpUrl url = urlBuilder("api/v1/applications/" + app)
                .addQueryParameter("cascade", String.valueOf(cascade))
                .addQueryParameter("propagationPolicy", propagationPolicy)
                .build();

        Request.Builder rb = new Request.Builder()
                .url(url)
                .header(AUTHORIZATION, authValue(token))
                .delete();

        exec(rb.build());
    }

    public void patchApp(String token, String app, List<Map<String, Object>> patch) throws IOException {
        Map<String, Object> body = new HashMap<>();
        body.put("name", app);
        body.put("patch", objectMapper.writeValueAsString(patch));
        body.put("patchType", "json");

        Request.Builder rb = new Request.Builder()
                .url(urlBuilder("api/v1/applications/").addPathSegment(app).build())
                .patch(RequestBody.create(APPLICATION_JSON, objectMapper.writeValueAsString(body)))
                .header(AUTHORIZATION, authValue(token));

        exec(rb.build());
    }

    public Map<String, Object> updateAppSpec(String token, String app, Map<String, Object> spec) throws IOException {
        Request.Builder rb = new Request.Builder()
                .url(urlBuilder("api/v1/applications/").addPathSegment(app).addPathSegment("spec").build())
                .put(RequestBody.create(APPLICATION_JSON, objectMapper.writeValueAsString(spec)))
                .header(AUTHORIZATION, authValue(token));

        return exec(rb.build(), response -> objectMapper.readMap(response.byteStream()) );
    }

    public Application syncApp(String token, TaskParams.SyncParams in) throws IOException {
        String revision = in.revision();
        if (revision != null) {
            patchApp(token, in.app(), Collections.singletonList(patchRevision(in.revision())));
        }
        return syncApplication(token, in);
    }

    public Watch<WatchAppResult> watchApp(String token, String app, String fromVersion, Duration watchTimeout) throws IOException {
        HttpUrl url = urlBuilder("api/v1/stream/applications")
                .addQueryParameter("name", app)
                .addQueryParameter("resourceVersion", fromVersion)
                .build();

        Request.Builder rb = new Request.Builder()
                .url(url)
                .header(AUTHORIZATION, authValue(token))
                .get();

        Request request = rb.build();
        Call call = toWatchClient(client, watchTimeout).newCall(request);
        Response response = null;
        ResponseBody body = null;
        try {
            response = call.execute();
            body = response.body();

            assertResponse(request, response, body);

            return new Watch<>(call, body, str -> convert(str, WatchAppResult.class));
        } catch (Exception e) {
            if (body != null) {
                body.close();
            }
            if (response != null) {
                response.close();
            }
            call.cancel();
            throw e;
        }
    }

    public HttpUrl.Builder urlBuilder(String path) {
        HttpUrl url = HttpUrl.parse(baseUrl);
        if (url == null) {
            throw new RuntimeException("Invalid URL: " + baseUrl);
        }
        return url.newBuilder()
                .addPathSegments(path);
    }

    public Application waitForSync(String token, String app, String resourceVersion, Duration waitTimeout, WaitWatchParams p) throws IOException {
        boolean refresh = false;
        try (Watch<WatchAppResult> w = watchApp(token, app, resourceVersion, waitTimeout)) {
            while (w.hasNext()) {
                WatchAppResult result = w.next();
                if (!result.error().isEmpty()) {
                    throw new RuntimeException("Error waiting for status: " + result.error());
                }

                if (result.application() == null) {
                    throw new RuntimeException("Error waiting for status: no application");
                }

                boolean operationInProgress = false;

                Application a = result.application();
                // consider the operation is in progress
                if (a.operation() != null) {
                    // if it just got requested
                    operationInProgress = true;
                    if (!Objects.requireNonNull(a.operation()).dryRun()) {
                        refresh = true;
                    }
                } else if (a.status().operationState() != null) {
                    Application.OperationState opState = Objects.requireNonNull(a.status().operationState());
                    OffsetDateTime finishedAt = opState.finishedAt();
                    OffsetDateTime reconciledAt = a.status().reconciledAt();
                    Application.Operation operation = opState.operation();

                    if (finishedAt == null) {
                        // if it is not finished yet
                        operationInProgress = true;
                    } else if (operation != null && !operation.dryRun() && (reconciledAt == null || reconciledAt.isBefore(finishedAt))) {
                        // if it is just finished and we need to wait for controller to reconcile app once after syncing
                        operationInProgress = true;
                    }
                }

                // Wait on the application as a whole
                // TODO: support for defined resources
                boolean selectedResourcesAreReady = checkResourceStatus(p, a.status().health().status(), a.status().sync().status(), a.operation());

                if (selectedResourcesAreReady && (!operationInProgress || !p.watchOperation())) {
                    if (refresh) {
                        return getApp(token, app, true);
                    }
                    return a;
                }
            }
        }
        return null;
    }

    public OkHttpClient.Builder newBuilder() {
        return this.client.newBuilder();
    }

    private static boolean checkResourceStatus(WaitWatchParams p, String healthStatus, String syncStatus, Application.Operation operation) {
        boolean healthCheckPassed = true;
        if (p.watchSuspended() && p.watchHealth()) {
            healthCheckPassed = HealthStatus.HEALTHY.value().equals(healthStatus) ||
                    HealthStatus.SUSPENDED.value().equals(healthStatus);
        } else if (p.watchSuspended()) {
            healthCheckPassed = HealthStatus.SUSPENDED.value().equals(healthStatus);
        } else if (p.watchHealth()) {
            healthCheckPassed = HealthStatus.HEALTHY.value().equals(healthStatus);
        }

        boolean synced = !p.watchSync() || SyncStatus.SYNCED.value().equals(syncStatus);
        boolean operational = !p.watchOperation() || operation == null;
        return synced && healthCheckPassed && operational;
    }

    private static Map<String, Object> patchRevision(String revision) {
        Map<String, Object> result = new HashMap<>();
        result.put("op", "replace");
        result.put("path", "/spec/source/targetRevision");
        result.put("value", revision);
        return result;
    }

    public Application createApp(String token, TaskParams.CreateUpdateParams in) throws RuntimeException, IOException {
        Map<String, Object> metadata = new HashMap<>();
        Map<String, Object> spec = new HashMap<>();
        Map<String, Object> source = new HashMap<>();
        Map<String, Object> helm = new HashMap<>();
        Map<String, Object> destination = new HashMap<>();
        Map<String, Object> body = new HashMap<>();

        metadata.put("name", in.app());
        metadata.put("namespace", ArgoCdConstants.ARGOCD_NAMESPACE);
        metadata.put("finalizers", ArgoCdConstants.FINALIZERS);

        if (in.annotations() != null) {
            metadata.put("annotations", in.annotations());
        }

        destination.put("namespace", in.namespace());
        destination.put("name", in.cluster());

        if (in.gitRepo() != null) {
            source.put("repoUrl", Objects.requireNonNull(in.gitRepo()).repoUrl());
            source.put("path", Objects.requireNonNull(in.gitRepo()).path());
            source.put("targetRevision", Objects.requireNonNull(in.gitRepo()).targetRevision());
        } else if (in.helmRepo() != null) {
            source.put("repoUrl", Objects.requireNonNull(in.helmRepo()).repoUrl());
            source.put("chart", Objects.requireNonNull(in.helmRepo()).chart());
            source.put("targetRevision", Objects.requireNonNull(in.helmRepo()).targetRevision());
        } else {
            throw new RuntimeException("Source information not provided for " + in.app() + ". Cannot proceed further");
        }

        if (in.helm() != null) {
            if (in.helm().parameters() != null)
                helm.put("parameters", in.helm().parameters());

            helm.put("values", in.helm().values());
            source.put("helm", helm);
        }

        spec.put("project", in.project());
        spec.put("destination", destination);
        spec.put("source", source);
        spec.put("syncPolicy", ArgoCdConstants.SYNC_POLICY);

        body.put("metadata", metadata);
        body.put("spec", spec);

        RequestBody requestBody = RequestBody.create(APPLICATION_JSON, objectMapper.writeValueAsString(body));
        Request.Builder rb = new Request.Builder()
                .url(urlBuilder("api/v1/applications").build())
                .post(requestBody)
                .header(AUTHORIZATION, authValue(token));

        return exec(rb.build(), response -> objectMapper.readValue(response.byteStream(), Application.class));

    }

    private Application syncApplication(String token, TaskParams.SyncParams in) throws IOException {
        Map<String, Object> body = new HashMap<>();
        if (in.resources() != null) {
            body.put("resources", in.resources());
        }

        if (in.dryRun()) {
            body.put("dryRun", in.dryRun());
        }

        if (in.prune()) {
            body.put("prune", in.prune());
        }

        if (!in.retryStrategy().isEmpty()) {
            body.put("retryStrategy", in.retryStrategy());
        }

        if (!in.strategy().isEmpty()) {
            body.put("strategy", in.strategy());
        }

        RequestBody request = EMPTY_REQUEST;
        if (!body.isEmpty()) {
            request = RequestBody.create(APPLICATION_JSON, objectMapper.writeValueAsString(body));
        }

        Request.Builder rb = new Request.Builder()
                .url(urlBuilder("api/v1/applications/").addPathSegment(in.app()).addPathSegment("sync").build())
                .post(request)
                .header(AUTHORIZATION, authValue(token));

        return exec(rb.build(), response -> objectMapper.readValue(response.byteStream(), Application.class));
    }

    private <T> T convert(String content, Class<T> valueType) {
        try {
            return objectMapper.readValue(content, valueType);
        } catch (Exception e) {
            log.error("Can't read '" + valueType.getName() + "' from: '" + content + "'");
            throw new RuntimeException(e);
        }
    }

    private void exec(Request request) throws IOException {
        exec(request, response -> null);
    }

    public <T> T exec(Request request, ResponseConverter<T> converter) throws IOException {
        try (Response response = client.newCall(request).execute();
             ResponseBody body = response.body()) {
            assertResponse(request, response, body);

            return converter.convert(body);
        }
    }

    private static void assertResponse(Request request, Response response, ResponseBody body) throws IOException {
        if (!response.isSuccessful()) {
            if (body != null) {
                throw exception(request, response.code(), body.string());
            }
            throw exception(request, response.code(), response.message());
        }

        if (body == null) {
            throw exception(request, response.code(), "no body returned");
        }
    }

    private static OkHttpClient toWatchClient(OkHttpClient client, Duration watchTimeout) {
        OkHttpClient.Builder builder = client.newBuilder()
                .callTimeout(watchTimeout != null ? watchTimeout : Duration.ZERO)
                .readTimeout(0, TimeUnit.SECONDS);

        List<Interceptor> interceptors = builder.interceptors();
        interceptors.removeIf(i -> i.getClass().equals(HttpLoggingInterceptor.class));

        return builder.build();
    }

    static RuntimeException exception(Request request, int code, String msg) {
        return new RuntimeException("Error occurred with API call (" + request.url() + "): status code:" + code + ", error: " + msg);
    }

    private static String authValue(String token) {
        return "Bearer " + token;
    }

    interface ResponseConverter<T> {
        T convert(ResponseBody response) throws IOException;
    }

    private static OkHttpClient createClient(TaskParams in) {
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                .connectTimeout(in.connectTimeout(), TimeUnit.SECONDS)
                .readTimeout(in.readTimeout(), TimeUnit.SECONDS)
                .writeTimeout(in.writeTimeout(), TimeUnit.SECONDS);

        if (in.debug()) {
            clientBuilder.addInterceptor(new HttpLoggingInterceptor(log::info).setLevel(HttpLoggingInterceptor.Level.BODY));
        }

        if (!in.validateCerts()) {
            try {
                final TrustManager[] tms = new TrustManager[]{
                        new X509TrustManager() {
                            @Override
                            public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                            }

                            @Override
                            public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                            }

                            @Override
                            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                                return new java.security.cert.X509Certificate[0];
                            }
                        }
                };
                final SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
                sslContext.init(null, tms, new java.security.SecureRandom());
                SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
                clientBuilder
                        .sslSocketFactory(sslSocketFactory, (X509TrustManager) tms[0])
                        .hostnameVerifier((hostname, session) -> true);
            } catch (Exception e) {
                throw new RuntimeException("Error disabling certificate validation: " + e.getMessage());
            }
        }

        return clientBuilder.build();
    }
}
