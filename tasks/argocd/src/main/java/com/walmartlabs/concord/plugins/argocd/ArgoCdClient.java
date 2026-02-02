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
import com.walmartlabs.concord.plugins.argocd.model.HttpExecutor;
import com.walmartlabs.concord.plugins.argocd.model.SyncStatus;
import com.walmartlabs.concord.plugins.argocd.openapi.ApiClient;
import com.walmartlabs.concord.plugins.argocd.openapi.ApiException;
import com.walmartlabs.concord.plugins.argocd.openapi.api.ApplicationServiceApi;
import com.walmartlabs.concord.plugins.argocd.openapi.api.ApplicationSetServiceApi;
import com.walmartlabs.concord.plugins.argocd.openapi.api.ProjectServiceApi;
import com.walmartlabs.concord.plugins.argocd.openapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.Callable;

public class ArgoCdClient {

    private final ApiClient client;

    private static final Logger log = LoggerFactory.getLogger(ArgoCdClient.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final int RETRY_LIMIT = 5;

    private static final Duration POLL_WAIT_TIME = Duration.ofSeconds(3);

    private static final Duration MAX_RETRY_TIMEOUT = Duration.ofMinutes(3);

    public ArgoCdClient(TaskParams in) throws Exception {
        this.client = createClient(in);
    }

    public V1alpha1Application getApp(String app, boolean refresh) throws ApiException {
        var api = new ApplicationServiceApi(client);
        return getApp(app, refresh, api);
    }

    private static V1alpha1Application getApp(String app, boolean refresh, ApplicationServiceApi api) throws ApiException {
        return api.applicationServiceGet(app, Boolean.toString(refresh), null, null, null, null, null);
    }

    public void deleteApp(String app, boolean cascade, String propagationPolicy) throws ApiException {
        var api = new ApplicationServiceApi(this.client);
        api.applicationServiceDelete(app, cascade, propagationPolicy, null);
    }

    public V1alpha1Application syncApp(TaskParams.SyncParams in) throws IOException, ApiException {
        var revision = in.revision();
        if (revision != null) {
            patchApp(in.app(), Collections.singletonList(patchRevision(in.revision())));
        }
        return syncApplication(in);
    }

    public void patchApp(String app, List<Map<String, Object>> patch) throws IOException, ApiException {
        var api = new ApplicationServiceApi(this.client);
        var patchRequest = new ApplicationApplicationPatchRequest()
                .name(app)
                .patch(MAPPER.writeValueAsString(patch))
                .patchType("json");
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
        var api = new ApplicationServiceApi(client);
        var resources = new ArrayList<V1alpha1SyncOperationResource>();
        for (TaskParams.SyncParams.Resource resource : in.resources()) {
            V1alpha1SyncOperationResource resourceOb = new V1alpha1SyncOperationResource();
            resourceOb.setGroup(resource.group());
            resourceOb.setKind(resource.kind());
            resourceOb.setName(resource.name());
            resourceOb.setNamespace(resource.namespace());
            resources.add(resourceOb);
        }
        var syncRequest = new ApplicationApplicationSyncRequest();
        syncRequest.dryRun(in.dryRun())
                .prune(in.prune())
                .retryStrategy(MAPPER.mapToModel(in.retryStrategy(), V1alpha1RetryStrategy.class))
                .resources(resources)
                .strategy(MAPPER.mapToModel(in.strategy(), V1alpha1SyncStrategy.class));
        return api.applicationServiceSync(in.app(), syncRequest);
    }

    static V1alpha1Application getAppWatchEvent(String app,
                                                HttpExecutor httpExecutor,
                                                HttpRequest request,
                                                WaitWatchParams p,
                                                ApplicationServiceApi appApi) throws ApiException, IOException {

            try (var is = httpExecutor.execute(request)) {
                var bufferedReader = new BufferedReader((new InputStreamReader(is)));

                String line;

                while ((line = bufferedReader.readLine()) != null) {
                    var result = MAPPER.readValue(line, StreamResultOfV1alpha1ApplicationWatchEvent.class);
                    if (result.getError() != null) {
                        throw new RuntimeException("Error waiting for status: " + result.getError());
                    }

                    if (result.getResult() == null) {
                        throw new RuntimeException("Error waiting for status: no application");
                    }

                    boolean operationInProgress;
                    boolean refreshApp = false;
                    V1alpha1Application a = Optional.ofNullable(result.getResult().getApplication())
                            .orElseThrow(() -> new IllegalStateException("No no application info in response"));

                    // consider the operation is in progress
                    if (a.getOperation() != null) {
                        // if it just got requested
                        operationInProgress = true;
                        if (Boolean.FALSE.equals(Optional.ofNullable(a.getOperation())
                                .map(V1alpha1Operation::getSync)
                                .map(V1alpha1SyncOperation::getDryRun)
                                .orElse(false))) {
                            refreshApp = true;
                        }
                    } else {
                        operationInProgress = isInProgress(a);
                    }

                    // Wait on the application as a whole
                    // TODO: support for defined resources
                    boolean selectedResourcesAreReady = checkResourceStatus(p, healthStatus(a), syncStatus(a), a.getOperation());
                    log.info("Selected resources are ready? {}", selectedResourcesAreReady);
                    if (selectedResourcesAreReady && (!operationInProgress || !p.watchOperation())) {
                        if (refreshApp) {
                            return getApp(app, true, appApi);
                        }
                        return a;
                    }
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            throw new IllegalStateException("No sync status returned");
    }

    private static boolean isInProgress(V1alpha1Application a) {
        return Optional.ofNullable(a.getStatus())
                .map(V1alpha1ApplicationStatus::getOperationState)
                // ignore dry runs
                .filter(opState -> !Optional.ofNullable(opState.getOperation())
                        .map(V1alpha1Operation::getSync)
                        .map(V1alpha1SyncOperation::getDryRun)
                        // assume non-dryRun if not found
                        .orElse(false))
                // see if last reconcile time is before finishedAt time
                .map(opState -> {
                    Optional<OffsetDateTime> rawFinishedAt = Optional.ofNullable(opState.getFinishedAt())
                            .map(OffsetDateTime::parse);
                    Optional<OffsetDateTime> rawReconciledAt = Optional.ofNullable(a.getStatus())
                            .map(V1alpha1ApplicationStatus::getReconciledAt)
                            .map(OffsetDateTime::parse);

                    boolean comparable = rawFinishedAt.isPresent() && rawReconciledAt.isPresent();
                    return comparable &&
                            // if it is just finished, and we need to wait for controller to reconcile app once after syncing
                            rawReconciledAt.get().isBefore(rawFinishedAt.get());
                })
                // worst case, assume not in progress
                .orElse(false);
    }

    public V1alpha1Application waitForSyncWithPolling(String appName, String resourceVersion, Duration waitTimeout, WaitWatchParams waitParams) {
        log.info("Waiting for application to sync using polling");
        OffsetDateTime startTime = OffsetDateTime.now();
        int pollCount = 0;
        while (OffsetDateTime.now().minus(waitTimeout).isBefore(startTime)) {
            V1alpha1Application application = new CallRetry<>(() -> getApp(appName, true),
                    null,
                    Set.of()).attemptWithRetry(RETRY_LIMIT, MAX_RETRY_TIMEOUT);
            boolean isReady = checkResourceStatus(waitParams,
                    healthStatus(application),
                    syncStatus(application),
                    application.getOperation());
            if (isReady) {
                log.info("Application is ready within {} seconds", Duration.between(startTime, OffsetDateTime.now()).toSeconds());
                return application;
            }
            pollCount++;
            log.info("Application is not ready. Waiting {} seconds before next try",
                    POLL_WAIT_TIME.getSeconds() * pollCount);
            try {
                //noinspection BusyWait
                Thread.sleep(pollCount * POLL_WAIT_TIME.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        throw new RuntimeException("Application is not ready within " + waitTimeout.getSeconds() + " seconds.");
    }

    public V1alpha1Application waitForSync(String appName, String resourceVersion, Duration waitTimeout, WaitWatchParams waitParams) {
        waitTimeout = (waitTimeout == null) ? Duration.ofMinutes(15) : waitTimeout;
        if (waitParams.useStreamApi())
            return waitForSyncWithStreamApi(appName, resourceVersion, waitTimeout, waitParams);
        else return waitForSyncWithPolling(appName, resourceVersion, waitTimeout, waitParams);
    }

    private V1alpha1Application waitForSyncWithStreamApi(String appName, String resourceVersion,
                                           Duration waitTimeout, WaitWatchParams waitParams) {
        log.info("Waiting for application to sync.");

        var paramString = toParameterString(Map.of("name", appName, "resourceVersion", resourceVersion));
        var uri = URI.create(client.getBaseUri() + "/api/v1/stream/applications?" + paramString);

        log.info("Using wait timeout {}", waitTimeout);
        var req = HttpRequest.newBuilder(uri)
                .GET()
                .timeout(waitTimeout)
                .build();

        Callable<V1alpha1Application> mainAttempt = () ->
                getAppWatchEvent(appName, httpExecutor(), req, waitParams, new ApplicationServiceApi(client));
        Callable<Optional<V1alpha1Application>> fallback = () -> {
            V1alpha1Application app = getApp(appName, true);
            if (checkResourceStatus(waitParams, healthStatus(app), syncStatus(app), app.getOperation())) {
                return Optional.of(app);
            }
            return Optional.empty();
        };

        return new CallRetry<>(mainAttempt, fallback, Set.of(SocketTimeoutException.class, HttpTimeoutException.class))
                .attemptWithRetry(RETRY_LIMIT, waitTimeout);
    }

    /**
     * @return default {@link HttpExecutor} implementation
     */
    private HttpExecutor httpExecutor() {
        return request -> client.getHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofInputStream()).body();
    }

    private static String healthStatus(V1alpha1Application app) {
        return Optional.ofNullable(app.getStatus())
                .map(V1alpha1ApplicationStatus::getHealth)
                .map(V1alpha1HealthStatus::getStatus)
                .orElse("unknown");
    }

    private static String syncStatus(V1alpha1Application app) {
        return Optional.ofNullable(app.getStatus())
                .map(V1alpha1ApplicationStatus::getSync)
                .map(V1alpha1SyncStatus::getStatus)
                .orElse("unknown");
    }

    public V1alpha1Application createApp(V1alpha1Application application, boolean upsert) throws RuntimeException, ApiException {
        var api = new ApplicationServiceApi(client);
        return api.applicationServiceCreate(application, upsert, true);
    }

    public V1alpha1ApplicationSpec updateAppSpec(String app, V1alpha1ApplicationSpec spec) throws ApiException {
        var api = new ApplicationServiceApi(client);
        return api.applicationServiceUpdateSpec(app, spec, true, null);
    }

    public V1alpha1AppProject getProject(String project) throws ApiException {
        var api = new ProjectServiceApi(client);
        return api.projectServiceGet(project);
    }

    public void deleteProject(String project) throws ApiException {
        var api = new ProjectServiceApi(client);
        api.projectServiceDelete(project);
    }

    public V1alpha1ApplicationSet createApplicationSet(V1alpha1ApplicationSet applicationSet, boolean upsert) throws ApiException {
        var api = new ApplicationSetServiceApi(client);
        return api.applicationSetServiceCreate(applicationSet, upsert);
    }

    public void deleteApplicationSet(String name) throws ApiException {
        var api = new ApplicationSetServiceApi(client);
        api.applicationSetServiceDelete(name);
    }

    public V1alpha1ApplicationSet getApplicationSet(String name) throws ApiException {
        var api = new ApplicationSetServiceApi(client);
        return api.applicationSetServiceGet(name);
    }

    public V1alpha1ApplicationSetList listApplicationSets(List<String> projects, String selector) throws ApiException {
        var api = new ApplicationSetServiceApi(client);
        return api.applicationSetServiceList(projects, selector);
    }

    public V1alpha1AppProject createProject(TaskParams.CreateProjectParams in) throws IOException, ApiException {
        var api = new ProjectServiceApi(client);
        var metadata = new HashMap<String, Object>();
        var spec = new HashMap<String, Object>();
        var project = new HashMap<String, Object>();

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
        var projectObject = MAPPER.mapToModel(project, V1alpha1AppProject.class);

        var createRequest = new ProjectProjectCreateRequest()
                .project(projectObject)
                .upsert(in.upsert());

        return api.projectServiceCreate(createRequest);
    }


    private static boolean checkResourceStatus(WaitWatchParams p, String healthStatus, String syncStatus, V1alpha1Operation operation) {
        var isHealthCheckPassed = true;
        if (p.watchSuspended() && p.watchHealth()) {
            isHealthCheckPassed = HealthStatus.HEALTHY.value().equals(healthStatus) ||
                    HealthStatus.SUSPENDED.value().equals(healthStatus);
        } else if (p.watchSuspended()) {
            isHealthCheckPassed = HealthStatus.SUSPENDED.value().equals(healthStatus);
        } else if (p.watchHealth()) {
            isHealthCheckPassed = HealthStatus.HEALTHY.value().equals(healthStatus);
        }

        var isSynced = !p.watchSync() || SyncStatus.SYNCED.value().equals(syncStatus);
        var isOperational = !p.watchOperation() || operation == null;
        return isSynced && isHealthCheckPassed && isOperational;
    }

    private static ApiClient createClient(TaskParams in) throws Exception {
        var httpBuilder = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(in.connectTimeout()));

        if (!in.validateCerts()) {
            try {
                final TrustManager[] tms = NoopTrustManager.getManagers();
                final SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
                sslContext.init(null, tms, new SecureRandom());
                httpBuilder.sslContext(sslContext);
            } catch (Exception e) {
                log.info("Error disabling certificate validation.");
                throw new IllegalStateException("Error disabling certificate validation: " + e.getMessage());
            }
        }

        var apiKey = createApiKey(in, httpBuilder);
        return new ApiClient(httpBuilder, MAPPER.getDelegate(), in.baseUrl())
                .setReadTimeout(Duration.ofSeconds(in.readTimeout()))
                .setRequestInterceptor(builder -> injectDefaultHeaders(builder, apiKey));
    }

    private static void injectDefaultHeaders(HttpRequest.Builder builder, String apiKey) {
        builder.header("Authorization", "Bearer " + apiKey);

        // This kind of stinks. GET and DELETE requests typically *shouldn't* have
        // a body, so swagger doesn't set the header in its generated code. However,
        // the ArgoCD API requires a Content-Type on these body-less requests.
        // Since the swagger spec, has a single blanket content type for all requests,
        // we could probably get away with just setting it to 'application/json'
        var current = builder.build(); // no getters on HttpRequest.Builder
        var contentType = current.headers().firstValue("Content-Type");

        if (contentType.isEmpty() || contentType.get().isBlank()) {
            builder.header("Content-Type", "application/json");
        }
    }

    private static String createApiKey(TaskParams in, HttpClient.Builder builder) throws Exception {
        var auth = in.auth();

        if (auth instanceof TaskParams.TokenAuth tokenAuth) {
            return TokenAuthHandler.auth(tokenAuth);
        } else if (auth instanceof TaskParams.BasicAuth basicAuth) {
            return BasicAuthHandler.auth(builder, in.baseUrl(), basicAuth);
        } else if (auth instanceof TaskParams.AzureAuth azureAuth) {
            return AzureAuthHandler.auth(azureAuth);
        } else if (auth instanceof TaskParams.LdapAuth ldapAuth) {
            return LdapAuthHandler.auth(builder, in.baseUrl(), ldapAuth);
        } else {
            throw new IllegalArgumentException("Unknown auth type");
        }
    }

    /**
     * Converts a map of parameters to a url query string. Does <b>not</b> include
     * the <code>?</code> at the start. Keys and values are url-encoded.
     * @return a query string of the form <code>key1=value1&key2=value2</code>
     */
    static String toParameterString(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return null;
        }

        var sb = new StringBuilder();
        params.forEach((k, v) -> {
            if (!sb.isEmpty()) {
                sb.append("&");
            }

            sb.append(URLEncoder.encode(k, StandardCharsets.UTF_8))
                    .append("=")
                    .append(URLEncoder.encode(v, StandardCharsets.UTF_8));
        });

        return sb.toString();
    }
}
