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

import com.walmartlabs.concord.plugins.argocd.model.HealthStatus;
import com.walmartlabs.concord.plugins.argocd.model.SyncStatus;
import com.walmartlabs.concord.plugins.argocd.openapi.ApiClient;
import com.walmartlabs.concord.plugins.argocd.openapi.ApiException;
import com.walmartlabs.concord.plugins.argocd.openapi.api.ApplicationServiceApi;
import com.walmartlabs.concord.plugins.argocd.openapi.api.ApplicationSetServiceApi;
import com.walmartlabs.concord.plugins.argocd.openapi.api.ProjectServiceApi;
import com.walmartlabs.concord.plugins.argocd.openapi.model.*;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.Callable;

public class ArgoCdClient {

    private final ApiClient client;

    private static final Logger log = LoggerFactory.getLogger(ArgoCdClient.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final int RETRY_LIMIT = 5;

    private TaskParams taskParams;

    public ArgoCdClient(TaskParams in) throws Exception {
        this.taskParams = in;
        this.client = createClient(in);
    }

    public V1alpha1Application getApp(String app, boolean refresh) throws IOException, ApiException {
        ApplicationServiceApi api = new ApplicationServiceApi(client);
        return getApp(app, refresh, api);
    }

    private static V1alpha1Application getApp(String app, boolean refresh, ApplicationServiceApi api) throws ApiException {
        log.info("Get application start {}", new Date().toString());
        V1alpha1Application app1 = api.applicationServiceGet(app, Boolean.toString(refresh), null, null, null, null, null);
        log.info("Get application end {}", new Date().toString());
        return app1;
    }

    public void deleteApp(String app, boolean cascade, String propagationPolicy) throws ApiException {
        ApplicationServiceApi api = new ApplicationServiceApi(this.client);
        api.applicationServiceDelete(app, cascade, propagationPolicy, null);
    }

    public V1alpha1Application syncApp(TaskParams.SyncParams in) throws IOException, ApiException {
        String revision = in.revision();
        if (revision != null) {
            patchApp(in.app(), Collections.singletonList(patchRevision(in.revision())));
        }
        return syncApplication(in);
    }

    public void patchApp(String app, List<Map<String, Object>> patch) throws IOException, ApiException {
        ApplicationServiceApi api = new ApplicationServiceApi(this.client);
        ApplicationApplicationPatchRequest patchRequest = new ApplicationApplicationPatchRequest().name(app).patch(MAPPER.writeValueAsString(patch)).patchType("json");
        api.applicationServicePatch(app, patchRequest);
    }

    private static Map<String, Object> patchRevision(String revision) {
        Map<String, Object> result = new HashMap<>();
        result.put("op", "replace");
        result.put("path", "/spec/source/targetRevision");
        result.put("value", revision);
        return result;
    }

    private V1alpha1Application syncApplication(TaskParams.SyncParams in) throws IOException, ApiException {
        List<V1alpha1SyncOperationResource> resources = new ArrayList<>();
        for (TaskParams.SyncParams.Resource resource : in.resources()) {
            V1alpha1SyncOperationResource resourceOb = new V1alpha1SyncOperationResource();
            resourceOb.setGroup(resource.group());
            resourceOb.setKind(resource.kind());
            resourceOb.setName(resource.name());
            resourceOb.setNamespace(resource.namespace());
            resources.add(resourceOb);
        }
        ApplicationApplicationSyncRequest syncRequest = new ApplicationApplicationSyncRequest();
        syncRequest.dryRun(in.dryRun()).prune(in.prune()).retryStrategy(MAPPER.mapToModel(in.retryStrategy(), V1alpha1RetryStrategy.class)).resources(resources).strategy(MAPPER.mapToModel(in.strategy(), V1alpha1SyncStrategy.class));
        return syncApplication(in.app(), syncRequest);
    }

    public V1alpha1Application syncApplication(String app, ApplicationApplicationSyncRequest syncRequest) throws IOException, ApiException {
        ApplicationServiceApi api = new ApplicationServiceApi(client);
        return api.applicationServiceSync(app, syncRequest);
    }

    static V1alpha1Application getAppWatchEvent(String app, ApiClient client, HttpUriRequest request, WaitWatchParams p, ApplicationServiceApi appApi) throws Exception {

        log.info("start get appwatch events {}", new Date().toString());
        try (CloseableHttpResponse response = client.getHttpClient().execute(request);) {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                log.info("Checking streaming buffer {}", new Date().toString());
                StreamResultOfV1alpha1ApplicationWatchEvent result = MAPPER.readValue(line, StreamResultOfV1alpha1ApplicationWatchEvent.class);
                if (result.getError() != null) {
                    throw new RuntimeException("Error waiting for status: " + result.getError());
                }

                if (result.getResult() == null) {
                    throw new RuntimeException("Error waiting for status: no application");
                }

                boolean operationInProgress;
                boolean refreshApp = false;
                V1alpha1Application a = Optional.ofNullable(result.getResult().getApplication()).orElseThrow(() -> new IllegalStateException("No no application info in response"));

                // consider the operation is in progress
                if (a.getOperation() != null) {
                    // if it just got requested
                    operationInProgress = true;
                    if (Boolean.FALSE.equals(Optional.ofNullable(a.getOperation()).map(V1alpha1Operation::getSync).map(V1alpha1SyncOperation::getDryRun).orElse(false))) {
                        refreshApp = true;
                    }
                } else {
                    operationInProgress = isInProgress(a);
                }

                // Wait on the application as a whole
                // TODO: support for defined resources
                boolean selectedResourcesAreReady = checkResourceStatus(p, healthStatus(a), syncStatus(a), a.getOperation());
                log.info("Selected resources are ready? {}, {}", (selectedResourcesAreReady && (!operationInProgress || !p.watchOperation())), new Date().toString());
                if (selectedResourcesAreReady && (!operationInProgress || !p.watchOperation())) {
                    if (refreshApp) {
                        log.info("loading application in main attempt {}", new Date().toString());
                        V1alpha1Application app1 = getApp(app, true, appApi);
                        log.info("application returned {}", new Date().toString());
                        return app1;
                    }
                    log.info("Retruning application, {}", new Date().toString());
                    return a;
                }
            }
        }
        throw new IllegalStateException("No sync status returned");
    }

    static boolean isInProgress(V1alpha1Application a) {
        return Optional.ofNullable(a.getStatus()).map(V1alpha1ApplicationStatus::getOperationState)
                // ignore dry runs
                .filter(opState -> !Optional.ofNullable(opState.getOperation()).map(V1alpha1Operation::getSync).map(V1alpha1SyncOperation::getDryRun)
                        // assume non-dryRun if not found
                        .orElse(false))
                // see if last reconcile time is before finishedAt time
                .map(opState -> {
                    Optional<OffsetDateTime> rawFinishedAt = Optional.ofNullable(opState.getFinishedAt()).map(OffsetDateTime::parse);
                    Optional<OffsetDateTime> rawReconciledAt = Optional.ofNullable(a.getStatus()).map(V1alpha1ApplicationStatus::getReconciledAt).map(OffsetDateTime::parse);

                    boolean comparable = rawFinishedAt.isPresent() && rawReconciledAt.isPresent();
                    return comparable &&
                            // if it is just finished, and we need to wait for controller to reconcile app once after syncing
                            rawReconciledAt.get().isBefore(rawFinishedAt.get());
                })
                // worst case, assume not in progress
                .orElse(false);
    }

    public V1alpha1Application waitForSync(String appName, String resourceVersion, Duration waitTimeout, WaitWatchParams p) throws Exception {
        log.info("Waiting for application to sync.");
        ApiClient apiClient = createClient(taskParams);
        URI uri = new URIBuilder(URI.create(taskParams.baseUrl())).setPath("api/v1/stream/applications").addParameter("name", appName).addParameter("resourceVersion", resourceVersion).build();
        waitTimeout = (waitTimeout == null) ? Duration.ZERO : waitTimeout;
        HttpUriRequest request = RequestBuilder.get(uri).setConfig(RequestConfig.custom().setSocketTimeout((int) waitTimeout.toMillis()).build()).build();

        Callable<V1alpha1Application> mainAttempt = () -> getAppWatchEvent(appName, apiClient, request, p, new ApplicationServiceApi(client));
        Callable<Optional<V1alpha1Application>> fallback = () -> {
            V1alpha1Application app = getApp(appName, true);
            if (checkResourceStatus(p, healthStatus(app), syncStatus(app), app.getOperation())) {
                return Optional.of(app);
            }
            return Optional.empty();
        };

        return new CallRetry<>(mainAttempt, fallback, List.of(SocketTimeoutException.class)).attemptWithRetry(RETRY_LIMIT);
    }

    private static String healthStatus(V1alpha1Application app) {
        return Optional.ofNullable(app.getStatus()).map(V1alpha1ApplicationStatus::getHealth).map(V1alpha1HealthStatus::getStatus).orElse("unknown");
    }

    private static String syncStatus(V1alpha1Application app) {
        return Optional.ofNullable(app.getStatus()).map(V1alpha1ApplicationStatus::getSync).map(V1alpha1SyncStatus::getStatus).orElse("unknown");
    }

    public V1alpha1Application createApp(V1alpha1Application application, boolean upsert) throws RuntimeException, ApiException {
        ApplicationServiceApi api = new ApplicationServiceApi(client);
        return api.applicationServiceCreate(application, upsert, true);
    }

    public V1alpha1ApplicationSpec updateAppSpec(String app, V1alpha1ApplicationSpec spec) throws ApiException {
        ApplicationServiceApi api = new ApplicationServiceApi(client);
        return api.applicationServiceUpdateSpec(app, spec, true, null);
    }

    public V1alpha1AppProject getProject(String project) throws ApiException {
        ProjectServiceApi api = new ProjectServiceApi(client);
        return api.projectServiceGet(project);
    }

    public void deleteProject(String project) throws ApiException {
        ProjectServiceApi api = new ProjectServiceApi(client);
        api.projectServiceDelete(project);
    }

    public V1alpha1ApplicationSet createApplicationSet(V1alpha1ApplicationSet applicationSet, boolean upsert) throws ApiException {
        ApplicationSetServiceApi api = new ApplicationSetServiceApi(client);
        return api.applicationSetServiceCreate(applicationSet, upsert);
    }

    public void deleteApplicationSet(String name) throws ApiException {
        ApplicationSetServiceApi api = new ApplicationSetServiceApi(client);
        api.applicationSetServiceDelete(name);
    }

    public V1alpha1ApplicationSet getApplicationSet(String name) throws ApiException {
        ApplicationSetServiceApi api = new ApplicationSetServiceApi(client);
        return api.applicationSetServiceGet(name);
    }

    public V1alpha1ApplicationSetList listApplicationSets(List<String> projects, String selector) throws ApiException {
        ApplicationSetServiceApi api = new ApplicationSetServiceApi(client);
        return api.applicationSetServiceList(projects, selector);
    }

    public V1alpha1AppProject createProject(TaskParams.CreateProjectParams in) throws IOException, ApiException {
        ProjectServiceApi api = new ProjectServiceApi(client);

        Map<String, Object> metadata = new HashMap<>();
        Map<String, Object> spec = new HashMap<>();
        Map<String, Object> project = new HashMap<>();

        metadata.put("name", in.project());
        if (in.namespace() != null && !in.namespace().isEmpty()) {
            metadata.put("namespace", in.namespace());
        }
        metadata.put("finalizers", ArgoCdConstants.FINALIZERS);

        if (in.cluster() != null) {
            metadata.put("clusterName", in.cluster());
        }

        if (in.description() != null) {
            spec.put("description", in.description());
        }

        if (in.sourceRepos() == null || Objects.requireNonNull(in.sourceRepos()).isEmpty()) {
            spec.put("sourceRepos", ArgoCdConstants.DEFAULT_SOURCE_REPOS);
        } else {
            spec.put("sourceRepos", in.sourceRepos());
        }

        if (in.destinations() == null || Objects.requireNonNull(in.destinations()).isEmpty()) {
            spec.put("destinations", ArgoCdConstants.DEFAULT_DESTINATIONS);
        } else {
            spec.put("destinations", in.destinations());
        }

        project.put("metadata", metadata);
        project.put("spec", spec);
        V1alpha1AppProject projectObject = MAPPER.mapToModel(project, V1alpha1AppProject.class);

        ProjectProjectCreateRequest createRequest = new ProjectProjectCreateRequest().project(projectObject).upsert(in.upsert());

        return api.projectServiceCreate(createRequest);
    }


    private static boolean checkResourceStatus(WaitWatchParams p, String healthStatus, String syncStatus, V1alpha1Operation operation) {
        boolean healthCheckPassed = true;
        if (p.watchSuspended() && p.watchHealth()) {
            healthCheckPassed = HealthStatus.HEALTHY.value().equals(healthStatus) || HealthStatus.SUSPENDED.value().equals(healthStatus);
        } else if (p.watchSuspended()) {
            healthCheckPassed = HealthStatus.SUSPENDED.value().equals(healthStatus);
        } else if (p.watchHealth()) {
            healthCheckPassed = HealthStatus.HEALTHY.value().equals(healthStatus);
        }

        boolean synced = !p.watchSync() || SyncStatus.SYNCED.value().equals(syncStatus);
        boolean operational = !p.watchOperation() || operation == null;
        return synced && healthCheckPassed && operational;
    }

    private static ApiClient createClient(TaskParams in) throws Exception {
        HttpClientBuilder builder = HttpClientBuilder.create();

        if (!in.validateCerts()) {
            TrustStrategy acceptingTrustStrategy = (cert, authType) -> true;
            SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(null, acceptingTrustStrategy).build();
            SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);

            Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create().register("https", sslsf).register("http", new PlainConnectionSocketFactory()).build();

            BasicHttpClientConnectionManager connectionManager = new BasicHttpClientConnectionManager(socketFactoryRegistry);
            builder.setSSLSocketFactory(sslsf).setConnectionManager(connectionManager);
        }
        String apiKey;
        if (in.auth() instanceof TaskParams.TokenAuth) {
            apiKey = TokenAuthHandler.auth((TaskParams.TokenAuth) in.auth());
        } else if (in.auth() instanceof TaskParams.BasicAuth) {
            apiKey = BasicAuthHandler.auth(in.baseUrl(), (TaskParams.BasicAuth) in.auth());
        } else if (in.auth() instanceof TaskParams.AzureAuth) {
            apiKey = AzureAuthHandler.auth((TaskParams.AzureAuth) in.auth());
        } else if (in.auth() instanceof TaskParams.LdapAuth) {
            apiKey = LdapAuthHandler.auth(builder, in.baseUrl(), (TaskParams.LdapAuth) in.auth());
        } else {
            throw new IllegalArgumentException("");
        }
        builder.setDefaultHeaders(Collections.singletonList(new BasicHeader("Authorization", "Bearer " + apiKey)));
        builder.setDefaultRequestConfig(RequestConfig.custom().setSocketTimeout(0).setConnectTimeout(0).build());
        ApiClient apiClient = new ApiClient(builder.build());
        apiClient.setBasePath(in.baseUrl());
        apiClient.setConnectTimeout((int) in.connectTimeout() * 1000);
        return apiClient;
    }

}
