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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.walmartlabs.concord.plugins.argocd.openapi.model.V1alpha1Application;
import com.walmartlabs.concord.plugins.argocd.openapi.model.V1alpha1ApplicationSpec;
import com.walmartlabs.concord.plugins.argocd.openapi.model.V1alpha1HelmParameter;
import org.immutables.value.Value;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Disabled("Required url, credentials, app")
public class ArgoCdClientTest {

    ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testSync() throws Exception {
        TaskParams.SyncParams in = ImmutableTestSyncParams.builder()
                .baseUrl(System.getProperty("ARGO_CD_BASE_URL"))
                .auth(ldap())
                .debug(true)
                .app(System.getProperty("ARGO_CD_APP"))
                .syncTimeout(Duration.ofMinutes(1))
                .build();

        ArgoCdClient client = new ArgoCdClient(in);
        V1alpha1Application app = client.syncApp(in);
        System.out.println("app: " + app);

        app = client.waitForSync(in.app(), app.getMetadata().getResourceVersion(), in.syncTimeout(), WaitWatchParams.builder().build());
        System.out.println("app2: " + app);
    }

    @Test
    public void testCreate() throws Exception {
        String values = "{replicaCount: 1, image: {repository: nginx, pullPolicy: IfNotPresent, tag: latest }, imagePullSecrets: [], nameOverride: , fullnameOverride: , serviceAccount: {create: true, annotations: {}, name: }, podAnnotations: {}, podSecurityContext: {}, securityContext: {}, service: {type: ClusterIP, port: 80}, ingress: {enabled: false, className: , annotations: {}, hosts: [{host: chart-example.local, paths: [{path: /, pathType: ImplementationSpecific}]}], tls: []}, resources: {}, autoscaling: {enabled: false, minReplicas: 1, maxReplicas: 100, targetCPUUtilizationPercentage: 80}, nodeSelector: {}, tolerations: [], affinity: {}}";
        TaskParams.CreateUpdateParams.GitRepo gitRepo = ImmutableTestGitRepo.builder()
                .repoUrl(System.getProperty("REPO_URL"))
                .path(System.getProperty("REPO_PATH"))
                .targetRevision(System.getProperty("REPO_BRANCH")).build();
        TaskParams.CreateUpdateParams in = ImmutableTestCreateParams.builder()
                .baseUrl(System.getProperty("ARGO_CD_BASE_URL"))
                .auth(ldap())
                .debug(true)
                .app(System.getProperty("ARGO_CD_APP"))
                .project(System.getProperty("ARGO_CD_PROJECT"))
                .recordEvents(false)
                .cluster(System.getProperty("ARGO_CD_CLUSTER"))
                .namespace(System.getProperty("ARGO_CD_NAMESPACE"))
                .gitRepo(gitRepo)
                .helm(TestCreateParams.TestHelm.of(null, values))
                .syncTimeout(Duration.ofMinutes(1))
                .build();

        ArgoCdClient client = new ArgoCdClient(in);
        V1alpha1Application app = client.createApp(objectMapper.buildApplicationObject(in));
        System.out.println("app: " + app);

        app = client.waitForSync(in.app(), app.getMetadata().getResourceVersion(), in.syncTimeout(), WaitWatchParams.builder().build());
        System.out.println("app2: " + app);
    }

    @Test
    public void testDelete() throws Exception {
        TaskParams.DeleteAppParams in = ImmutableTestDeleteAppParams.builder()
                .baseUrl(System.getProperty("ARGO_CD_BASE_URL"))
                .auth(ldap())
                .debug(true)
                .app(System.getProperty("ARGO_CD_APP"))
                .build();

        ArgoCdClient client = new ArgoCdClient(in);
        client.deleteApp(in.app(), in.cascade(), in.propagationPolicy());
    }

    @Test
    public void testGet() throws Exception {
        TaskParams.GetParams in = ImmutableTestGetParams.builder()
                .baseUrl(System.getProperty("ARGO_CD_BASE_URL"))
                .auth(ldap())
                .debug(true)
                .app(System.getProperty("ARGO_CD_APP"))
                .build();

        ArgoCdClient client = new ArgoCdClient(in);
        V1alpha1Application app = client.getApp(in.app(), in.refresh());
        System.out.println("app: " + app);
    }



    @Test
    public void testGetWithToken() throws Exception {
        TaskParams.GetParams in = ImmutableTestGetParams.builder()
                .baseUrl(System.getProperty("ARGO_CD_BASE_URL"))
                .auth(token())
                .debug(true)
                .app(System.getProperty("ARGO_CD_APP"))
                .build();

        ArgoCdClient client = new ArgoCdClient(in);
        V1alpha1Application app = client.getApp(in.app(), in.refresh());
        System.out.println("app: " + app);
    }

    @Test
    public void testPatch() throws Exception {
        Map<String, Object> patch = new HashMap<>();
        patch.put("op", "replace");
        patch.put("path", "/spec/source/helm/image/tag");
        patch.put("value", "XXX-YYY-ZZZ");

        TaskParams.PatchParams in = ImmutableTestPatchParams.builder()
                .baseUrl(System.getProperty("ARGO_CD_BASE_URL"))
                .auth(basic())
                .debug(true)
                .app(System.getProperty("ARGO_CD_APP"))
                .addPatches(patch)
                .build();

        ArgoCdClient client = new ArgoCdClient(in);
        client.patchApp(in.app(), in.patches());
    }

    @Test
    public void testSetParams() throws Exception {
        TaskParams.SetAppParams in = ImmutableTestSetAppParams.builder()
                .baseUrl(System.getProperty("ARGO_CD_BASE_URL"))
                .auth(basic())
                .debug(true)
                .app(System.getProperty("ARGO_CD_APP"))
                .addHelm(TestSetAppParams.TestHelmParam.of("image.tag", "XX-YY"))
                .build();

        ArgoCdClient client = new ArgoCdClient(in);
        V1alpha1Application app = client.getApp(in.app(), false);
        V1alpha1ApplicationSpec appSpec = app.getSpec();

        List<V1alpha1HelmParameter> params = new ArrayList<>();
        in.helm().stream()
                .forEach(p -> {
                    V1alpha1HelmParameter param = new V1alpha1HelmParameter();
                    param.setValue(p.value().toString());
                    param.setName(p.name());
                    params.add(param);
                });
        appSpec.getSource().getHelm().setParameters(params);

        V1alpha1ApplicationSpec result = client.updateAppSpec(in.app(), appSpec);
        System.out.println(result);
    }

    private static TaskParams.AzureAuth azure() {
        return ImmutableTestAzureAuth.builder()
                .clientId(System.getProperty("ARGO_CD_CLIENT_ID"))
                .authority(System.getProperty("ARGO_CD_AUTHORITY"))
                .username(System.getProperty("ARGO_CD_USER"))
                .password(System.getProperty("ARGO_CD_PASSWORD"))
                .scope(Collections.singletonList("user.read"))
                .build();
    }

    @Test
    public void testGetWithAzureToken() throws Exception {
        TaskParams.GetParams in = ImmutableTestGetParams.builder()
                .baseUrl(System.getProperty("ARGO_CD_BASE_URL"))
                .auth(azure())
                .debug(true)
                .app(System.getProperty("ARGO_CD_APP"))
                .build();

        ArgoCdClient client = new ArgoCdClient(in);
        V1alpha1Application app = client.getApp(in.app(), in.refresh());
        assertEquals(System.getProperty("ARGO_CD_APP"), app.getMetadata().getName());
    }

    @Test
    public void testSyncWithAzureToken() throws Exception {
        TaskParams.SyncParams in = ImmutableTestSyncParams.builder()
                .baseUrl(System.getProperty("ARGO_CD_BASE_URL"))
                .auth(azure())
                .debug(true)
                .app(System.getProperty("ARGO_CD_APP"))
                .syncTimeout(Duration.ofMinutes(1))
                .build();

        ArgoCdClient client = new ArgoCdClient(in);
        V1alpha1Application app = client.syncApp(in);
        System.out.println("app: " + app);

        app = client.waitForSync(in.app(), app.getMetadata().getResourceVersion(), in.syncTimeout(), WaitWatchParams.builder().build());
        System.out.println("app2: " + app);
    }

    private static TaskParams.BasicAuth basic() {
        return ImmutableTestBasicAuth.builder()
                .username(System.getProperty("ARGO_CD_USER"))
                .password(System.getProperty("ARGO_CD_PASSWORD"))
                .build();
    }

    private static TaskParams.LdapAuth ldap() {
        return ImmutableTestLdapAuth.builder()
                .username(System.getProperty("ARGO_CD_USER"))
                .password(System.getProperty("ARGO_CD_PASSWORD"))
                .build();
    }


    private static TaskParams.TokenAuth token() {
        return ImmutableTestTokenAuth.builder()
                .token("tes-jwt-token")
                .build();
    }

    @Test
    public void testCreateWithAzureToken() throws Exception {
        String values = "{replicaCount: 1, image: {repository: nginx, pullPolicy: IfNotPresent, tag: latest }, imagePullSecrets: [], nameOverride: , fullnameOverride: , serviceAccount: {create: true, annotations: {}, name: }, podAnnotations: {}, podSecurityContext: {}, securityContext: {}, service: {type: ClusterIP, port: 80}, ingress: {enabled: false, className: , annotations: {}, hosts: [{host: chart-example.local, paths: [{path: /, pathType: ImplementationSpecific}]}], tls: []}, resources: {}, autoscaling: {enabled: false, minReplicas: 1, maxReplicas: 100, targetCPUUtilizationPercentage: 80}, nodeSelector: {}, tolerations: [], affinity: {}}";
        TaskParams.CreateUpdateParams.GitRepo gitRepo = ImmutableTestGitRepo.builder()
                .repoUrl(System.getProperty("REPO_URL"))
                .path(System.getProperty("REPO_PATH"))
                .targetRevision(System.getProperty("REPO_BRANCH")).build();
        TaskParams.CreateUpdateParams in = ImmutableTestCreateParams.builder()
                .baseUrl(System.getProperty("ARGO_CD_BASE_URL"))
                .auth(azure())
                .debug(true)
                .app(System.getProperty("ARGO_CD_APP"))
                .project(System.getProperty("ARGO_CD_PROJECT"))
                .recordEvents(false)
                .cluster(System.getProperty("ARGO_CD_CLUSTER"))
                .namespace(System.getProperty("ARGO_CD_NAMESPACE"))
                .gitRepo(gitRepo)
                .helm(TestCreateParams.TestHelm.of(null, values))
                .syncTimeout(Duration.ofMinutes(1))
                .build();

        ArgoCdClient client = new ArgoCdClient(in);
        V1alpha1Application app = client.createApp(objectMapper.buildApplicationObject(in));
        System.out.println("app: " + app);

        app = client.waitForSync(in.app(), app.getMetadata().getResourceVersion(), in.syncTimeout(), WaitWatchParams.builder().build());
        System.out.println("app2: " + app);
    }

    @Value.Immutable
    @Value.Style(jdkOnly = true)
    public interface TestSyncParams extends TaskParams.SyncParams {

        @Value.Immutable
        @Value.Style(jdkOnly = true)
        @JsonSerialize(as = ImmutableTestResource.class)
        @JsonDeserialize(as = ImmutableTestResource.class)
        interface TestResource extends TaskParams.SyncParams.Resource {
        }

        @Value.Default()
        @Override
        default boolean validateCerts() {
            return false;
        }
    }

    @Value.Immutable
    @Value.Style(jdkOnly = true)
    public interface TestDeleteAppParams extends TaskParams.DeleteAppParams {

        @Value.Default()
        @Override
        default boolean validateCerts() {
            return false;
        }
    }

    @Value.Immutable
    @Value.Style(jdkOnly = true)
    public interface TestPatchParams extends TaskParams.PatchParams {

        @Value.Default()
        @Override
        default boolean validateCerts() {
            return false;
        }
    }

    @Value.Immutable
    @Value.Style(jdkOnly = true)
    public interface TestGetParams extends TaskParams.GetParams {

        @Value.Default()
        @Override
        default boolean validateCerts() {
            return false;
        }
    }

    @Value.Immutable
    @Value.Style(jdkOnly = true)
    public interface TestSetAppParams extends TaskParams.SetAppParams {

        @Value.Default()
        @Override
        default boolean validateCerts() {
            return false;
        }

        @Value.Immutable
        @Value.Style(jdkOnly = true)
        interface TestHelmParam extends TaskParams.SetAppParams.HelmParam {

            static HelmParam of(String name, Object value) {
                return ImmutableTestHelmParam.builder()
                        .name(name)
                        .value(value)
                        .build();
            }
        }
    }

    @Value.Immutable
    @Value.Style(jdkOnly = true)
    public interface TestCreateParams extends TaskParams.CreateUpdateParams {

        @Value.Default()
        @Override
        default boolean validateCerts() {
            return false;
        }

        @Value.Immutable
        @Value.Style(jdkOnly = true)
        interface TestHelm extends TaskParams.CreateUpdateParams.Helm {

            static TestHelm of(List <Map<String, Object>> parameters, String values) {
                return ImmutableTestHelm.builder()
                        .parameters(parameters)
                        .values(values)
                        .build();
            }
        }

        @Value.Immutable
        @Value.Style(jdkOnly = true)
        interface TestGitRepo extends CreateUpdateParams.GitRepo {

            static TestGitRepo of(String repoUrl, String targetRevision, String path) {
                return ImmutableTestGitRepo.builder()
                        .repoUrl(repoUrl)
                        .targetRevision(targetRevision)
                        .path(path)
                        .build();
            }
        }

        @Value.Immutable
        @Value.Style(jdkOnly = true)
        interface TestHelmRepo extends CreateUpdateParams.HelmRepo {

            static TestHelmRepo of(String repoUrl, String targetRevision, String chart) {
                return ImmutableTestHelmRepo.builder()
                        .repoUrl(repoUrl)
                        .targetRevision(targetRevision)
                        .chart(chart)
                        .build();
            }
        }
    }

    @Value.Immutable
    @Value.Style(jdkOnly = true)
    public interface TestLdapAuth extends TaskParams.LdapAuth {

    }


    @Value.Immutable
    @Value.Style(jdkOnly = true)
    public interface TestTokenAuth extends TaskParams.TokenAuth {

    }

    @Value.Immutable
    @Value.Style(jdkOnly = true)
    public interface TestBasicAuth extends TaskParams.BasicAuth {

    }

    @Value.Immutable
    @Value.Style(jdkOnly = true)
    public interface TestAzureAuth extends TaskParams.AzureAuth {

    }
}
