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
import com.walmartlabs.concord.plugins.argocd.model.Application;
import org.immutables.value.Value;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Disabled("Required url, credentials, app")
public class ArgoCdClientTest {

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
        String token = client.auth(in.auth());
        Application app = client.syncApp(token, in);
        System.out.println("app: " + app);

        app = client.waitForSync(token, in.app(), app.resourceVersion(), in.syncTimeout(), WaitWatchParams.builder().build());
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
        String token = client.auth(in.auth());
        Application app = client.createApp(token, in);
        System.out.println("app: " + app);

        app = client.waitForSync(token, in.app(), app.resourceVersion(), in.syncTimeout(), WaitWatchParams.builder().build());
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
        String token = client.auth(in.auth());
        client.deleteApp(token, in.app(), in.cascade(), in.propagationPolicy());
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
        String token = client.auth(in.auth());
        Application app = client.getApp(token, in.app(), in.refresh());
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
        String token = client.auth(in.auth());
        client.patchApp(token, in.app(), in.patches());
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
        String token = client.auth(in.auth());
        Application app = client.getApp(token, in.app(), false);
        Map<String, Object> appSpec = app.spec();

        List<Map<String, Object>> appHelmParams = in.helm().stream()
                .map(p -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("name", p.name());
                    result.put("value", p.value());
                    return result;
                })
                .collect(Collectors.toList());
        appSpec = MapUtils.set(appSpec, "source.helm.parameters", appHelmParams);

        Map<String, Object> result = client.updateAppSpec(token, in.app(), appSpec);
        System.out.println(result);
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
        interface TestHelmParams extends CreateUpdateParams.HelmParams {

            static TestHelmParams of(String name, String value) {
                return ImmutableTestHelmParams.builder()
                        .name(name)
                        .value(value)
                        .build();
            }
        }

        @Value.Immutable
        @Value.Style(jdkOnly = true)
        interface TestHelm extends TaskParams.CreateUpdateParams.Helm {

            static TestHelm of(List <TestHelmParams> parameters, String values) {
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
    public interface TestBasicAuth extends TaskParams.BasicAuth {

    }
}
