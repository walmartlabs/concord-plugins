package com.walmartlabs.concord.plugins.argocd.it;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2025 Walmart Inc., Concord Authors
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

import ca.ibodrov.concord.testcontainers.Concord;
import ca.ibodrov.concord.testcontainers.ConcordProcess;
import ca.ibodrov.concord.testcontainers.Payload;
import ca.ibodrov.concord.testcontainers.junit5.ConcordRule;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.walmartlabs.concord.client2.ProcessEntry;
import com.walmartlabs.concord.client2.ProjectEntry;
import com.walmartlabs.concord.client2.ProjectsApi;
import com.walmartlabs.concord.common.IOUtils;
import com.walmartlabs.concord.plugins.argocd.ObjectMapper;
import com.walmartlabs.concord.plugins.argocd.openapi.model.V1ObjectMeta;
import com.walmartlabs.concord.plugins.argocd.openapi.model.V1alpha1Application;
import com.walmartlabs.concord.plugins.argocd.openapi.model.V1alpha1ApplicationSource;
import com.walmartlabs.concord.plugins.argocd.openapi.model.V1alpha1ApplicationSpec;
import com.walmartlabs.concord.plugins.argocd.openapi.model.V1alpha1ApplicationStatus;
import com.walmartlabs.concord.plugins.argocd.openapi.model.V1alpha1HealthStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;

import static ca.ibodrov.concord.testcontainers.Utils.randomString;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;

@WireMockTest
public class ArgoIT {

    @RegisterExtension
    public static final ConcordRule concord = new ConcordRule()
            .mode(Concord.Mode.DOCKER)
            .streamServerLogs(false)
            .streamAgentLogs(false)
            .useLocalMavenRepository(true);

    private static final String CURRENT_VERSION = getCurrentVersion();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeEach
    void setUp() {
        stubFor(get(urlEqualTo("/auth/login?connector_id=ldap"))
                .willReturn(aResponse()
                        .withBody("{}")
                        .withStatus(200)));

        stubFor(post(urlEqualTo("/auth/login?connector_id=ldap"))
                .willReturn(aResponse()
                        .withBody("{}")
                        .withStatus(200)));

        stubFor(get(urlEqualTo("/api/v1/applications/test?refresh=false"))
                .willReturn(aResponse()
                        .withBody(mockApplication())
                        .withStatus(200)));
    }

    @Test
    void testRuntimeV2(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
        String concordYmlSource = "runtimeV2/concord.yml";

        String orgName = "org_" + randomString();
        concord.organizations().create(orgName);

        String projectName = "project_" + randomString();
        concord.projects().create(orgName, projectName);

        var projectsApi = new ProjectsApi(concord.apiClient());
        var project = projectsApi.getProject(orgName, projectName);
        project.setAcceptsRawPayload(true);
        project.rawPayloadMode(ProjectEntry.RawPayloadModeEnum.EVERYONE);
        projectsApi.createOrUpdateProject(orgName, project);

        Payload payload = new Payload()
                .org(orgName)
                .project(projectName)
                .arg("argoBaseUrl", "http://host.docker.internal:" + wmRuntimeInfo.getHttpPort())
                .concordYml(new String(readToBytes(concordYmlSource))
                        .replace("%%version%%", CURRENT_VERSION));

        //  ---

        ConcordProcess proc = concord.processes().start(payload);
        proc.waitForStatus(ProcessEntry.StatusEnum.FINISHED);

        //  ---

        proc.assertLog(".*got app status: Healthy.*");
    }

    private static String getCurrentVersion() {
        Properties props = new Properties();
        try (InputStream in = ClassLoader.getSystemResourceAsStream("version.properties")) {
            props.load(in);
            return props.getProperty("version");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static byte[] readToBytes(String resource) {
        try (InputStream in = ArgoIT.class.getResourceAsStream(resource)) {
            if (Objects.isNull(in)) {
                throw new IllegalStateException("Failed to load resource: " + resource);
            }

            return IOUtils.toByteArray(in);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String mockApplication() {

        var app = new V1alpha1Application()
                .metadata(new V1ObjectMeta()
                        .name("test")
                        .namespace("test-ns"))
                .spec(new V1alpha1ApplicationSpec()
                        .project("default")
                        .source(new V1alpha1ApplicationSource()
                                .repoURL("https://github.com/argoproj/argocd-example-apps.git")
                                .targetRevision("HEAD")))
                .operation(null)
                .status(new V1alpha1ApplicationStatus()
                        .health(new V1alpha1HealthStatus()
                                .status("Healthy")));

        try {
            return MAPPER.writeValueAsString(app);
        } catch (IOException e) {
            throw new IllegalStateException("Error generating mock argo application: " + e.getMessage());
        }
    }
}
