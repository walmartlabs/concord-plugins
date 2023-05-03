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
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;

public class ArgoCdClient {

    private final ApiClient client;

    private final static Logger log = LoggerFactory.getLogger(ArgoCdClient.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ArgoCdClient(TaskParams in) throws Exception {
        this.client = createClient(in);
    }

    public V1alpha1Application getApp(String app, boolean refresh) throws IOException, ApiException {
        ApplicationServiceApi api = new ApplicationServiceApi(this.client);
        V1alpha1Application application = api.applicationServiceGet(app, Boolean.toString(refresh), null, null, null, null);
        return application;
    }

    public void deleteApp(String app, boolean cascade, String propagationPolicy) throws IOException, ApiException {
        ApplicationServiceApi api = new ApplicationServiceApi(this.client);
        api.applicationServiceDelete(app, cascade, propagationPolicy);
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
        ApplicationApplicationPatchRequest patchRequest = new ApplicationApplicationPatchRequest()
                .name(app)
                .patch(objectMapper.writeValueAsString(patch))
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
        ApplicationServiceApi api = new ApplicationServiceApi(client);
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
        syncRequest.dryRun(in.dryRun())
                .prune(in.prune())
                .retryStrategy(objectMapper.mapToModel(in.retryStrategy(), V1alpha1RetryStrategy.class))
                .resources(resources)
                .strategy(objectMapper.mapToModel(in.strategy(), V1alpha1SyncStrategy.class));
        return api.applicationServiceSync(in.app(), syncRequest);
    }

    public V1alpha1Application waitForSync(String app, String resourceVersion, Duration waitTimeout, WaitWatchParams p) throws IOException, ApiException, URISyntaxException {
        boolean refresh = false;
        log.info("Waiting for application to sync.");
        URI uri = new URIBuilder(URI.create(client.getBasePath()))
                .setPath("api/v1/stream/applications").addParameter("name", app)
                .addParameter("resourceVersion", resourceVersion).build();
        waitTimeout = (waitTimeout == null) ? Duration.ZERO : waitTimeout;
        HttpUriRequest request = RequestBuilder.get(uri)
                .setConfig(RequestConfig.custom().
                        setSocketTimeout((int) waitTimeout.toMillis()).build())
                .build();
        CloseableHttpResponse response = client.getHttpClient().execute(request);

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
        String line;
        try {
            while ((line = bufferedReader.readLine()) != null) {
                StreamResultOfV1alpha1ApplicationWatchEvent result = objectMapper.readValue(line, StreamResultOfV1alpha1ApplicationWatchEvent.class);
                if (result.getError() != null) {
                    throw new RuntimeException("Error waiting for status: " + result.getError());
                }

                if (result.getResult() == null) {
                    throw new RuntimeException("Error waiting for status: no application");
                }

                boolean operationInProgress = false;

                V1alpha1Application a = result.getResult().getApplication();
                // consider the operation is in progress
                if (a.getOperation() != null) {
                    // if it just got requested
                    operationInProgress = true;
                    if (Boolean.FALSE.equals(a.getOperation().getSync().getDryRun())) {
                        refresh = true;
                    }
                } else if (a.getStatus().getOperationState() != null) {
                    V1alpha1OperationState opState = Objects.requireNonNull(a.getStatus().getOperationState());
                    OffsetDateTime finishedAt = OffsetDateTime.parse(opState.getFinishedAt());
                    OffsetDateTime reconciledAt = OffsetDateTime.parse(a.getStatus().getReconciledAt());
                    V1alpha1Operation operation = opState.getOperation();

                    if (finishedAt == null) {
                        // if it is not finished yet
                        operationInProgress = true;
                    } else if (operation != null && !Boolean.TRUE.equals(operation.getSync().getDryRun()) && (reconciledAt == null || reconciledAt.isBefore(finishedAt))) {
                        // if it is just finished and we need to wait for controller to reconcile app once after syncing
                        operationInProgress = true;
                    }
                }

                // Wait on the application as a whole
                // TODO: support for defined resources
                boolean selectedResourcesAreReady = checkResourceStatus(p, a.getStatus().getHealth().getStatus(), a.getStatus().getSync().getStatus(), a.getOperation());
                log.info("Selected resources are ready ? {}", selectedResourcesAreReady);
                if (selectedResourcesAreReady && (!operationInProgress || !p.watchOperation())) {
                    if (refresh) {
                        return getApp(app, true);
                    }
                    return a;
                }
            }
        } finally {
            response.close();
        }

        return null;
    }

    public V1alpha1Application createApp(TaskParams.CreateUpdateParams in) throws RuntimeException, IOException, ApiException {
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
            source.put("repoURL", Objects.requireNonNull(in.gitRepo()).repoUrl());
            source.put("path", Objects.requireNonNull(in.gitRepo()).path());
            source.put("targetRevision", Objects.requireNonNull(in.gitRepo()).targetRevision());
        } else if (in.helmRepo() != null) {
            source.put("repoUrl", Objects.requireNonNull(in.helmRepo()).repoUrl());
            source.put("chart", Objects.requireNonNull(in.helmRepo()).chart());
            source.put("targetRevision", Objects.requireNonNull(in.helmRepo()).targetRevision());
        } else {
            throw new RuntimeException("Source information not provided for " + in.app() + "." +
                    "Provide either `gitRepo` or `helmRepo` details for the application to be created." +
                    "Cannot proceed further. Refer docs (https://concord.walmartlabs.com/docs/plugins-v2/argocd.html#usage) for usage");
        }

        if (in.helm() != null) {
            if (Objects.requireNonNull(in.helm()).parameters() != null)
                helm.put("parameters", Objects.requireNonNull(in.helm()).parameters());

            helm.put("values", Objects.requireNonNull(in.helm()).values());
            source.put("helm", helm);
        }

        spec.put("project", in.project());
        spec.put("destination", destination);
        spec.put("source", source);

        if (in.createNamespace()) {
            Map<String, Object> syncPolicy = new HashMap<>(ArgoCdConstants.SYNC_POLICY);
            syncPolicy.put("syncOptions", ArgoCdConstants.CREATE_NAMESPACE_OPTION);
            spec.put("syncPolicy", syncPolicy);
        } else {
            spec.put("syncPolicy", ArgoCdConstants.SYNC_POLICY);
        }

        body.put("metadata", metadata);
        body.put("spec", spec);

        V1alpha1Application application = objectMapper.mapToModel(body, V1alpha1Application.class);

        ApplicationServiceApi api = new ApplicationServiceApi(client);
        return api.applicationServiceCreate(application, null, null);
    }

    public V1alpha1ApplicationSpec updateAppSpec(String app, V1alpha1ApplicationSpec spec) throws IOException, ApiException {
        ApplicationServiceApi api = new ApplicationServiceApi(client);
        return api.applicationServiceUpdateSpec(app, spec, true);
    }

    public V1alpha1AppProject getProject(String project) throws ApiException {
        ProjectServiceApi api = new ProjectServiceApi(client);
        return api.projectServiceGet(project);
    }

    public void deleteProject(String project) throws ApiException {
        ProjectServiceApi api = new ProjectServiceApi(client);
        api.projectServiceDelete(project);
    }

    public V1alpha1AppProject createProject(TaskParams.CreateProjectParams in) throws IOException, ApiException {
        ProjectServiceApi api = new ProjectServiceApi(client);

        Map<String, Object> metadata = new HashMap<>();
        Map<String, Object> spec = new HashMap<>();
        Map<String, Object> project = new HashMap<>();

        metadata.put("name", in.project());
        if(in.namespace() != null && !in.namespace().isEmpty()){
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
        V1alpha1AppProject projectObject = objectMapper.mapToModel(project, V1alpha1AppProject.class);

        ProjectProjectCreateRequest createRequest = new ProjectProjectCreateRequest()
                .project(projectObject).upsert(in.upsert());

        return api.projectServiceCreate(createRequest);
    }


    private static boolean checkResourceStatus(WaitWatchParams p, String healthStatus, String syncStatus, V1alpha1Operation operation) {
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


    private static ApiClient createClient(TaskParams in) throws Exception {
        HttpClientBuilder builder = HttpClientBuilder.create();

        if (!in.validateCerts()) {
            TrustStrategy acceptingTrustStrategy = (cert, authType) -> true;
            SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(null, acceptingTrustStrategy).build();
            SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext,
                    NoopHostnameVerifier.INSTANCE);

            Registry<ConnectionSocketFactory> socketFactoryRegistry =
                    RegistryBuilder.<ConnectionSocketFactory>create()
                            .register("https", sslsf)
                            .register("http", new PlainConnectionSocketFactory())
                            .build();

            BasicHttpClientConnectionManager connectionManager =
                    new BasicHttpClientConnectionManager(socketFactoryRegistry);
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
