package com.walmartlabs.concord.plugins.akeyless.it;

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

import ca.ibodrov.concord.testcontainers.Concord;
import ca.ibodrov.concord.testcontainers.ConcordProcess;
import ca.ibodrov.concord.testcontainers.NewSecretQuery;
import ca.ibodrov.concord.testcontainers.Payload;
import ca.ibodrov.concord.testcontainers.junit5.ConcordRule;
import com.walmartlabs.concord.client2.ApiException;
import com.walmartlabs.concord.client2.ProcessEntry;
import com.walmartlabs.concord.client2.ProjectEntry;
import com.walmartlabs.concord.client2.ProjectsApi;
import com.walmartlabs.concord.common.IOUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Properties;

import static ca.ibodrov.concord.testcontainers.Utils.randomString;

class TaskIT extends AbstractIT {

    @RegisterExtension
    public static final ConcordRule concord = new ConcordRule()
            .mode(Concord.Mode.DOCKER)
            .serverImage(getEnv("IT_SERVER_IMAGE", "walmartlabs/concord-server:2.21.0"))
            .agentImage(getEnv("IT_AGENT_IMAGE", "walmartlabs/concord-agent:2.21.0"))
            .streamServerLogs(false)
            .streamAgentLogs(false)
            .useLocalMavenRepository(true);

    private static final String CURRENT_VERSION = getCurrentVersion();
    private static final Logger log = LoggerFactory.getLogger(TaskIT.class);

    @BeforeAll
    static void beforeAll() {
        log.info("Concord url: {}", concord.apiBaseUrl());
        log.info("Admin token: {}", concord.environment().apiToken());
    }

    @Test
    void testWithRuntimeV1() throws Exception {
        test("runtimeV1/concord.yml");
    }

    @Test
    void testWithRuntimeV2() throws Exception {
        test("runtimeV2/concord.yml");
    }

    private void test(String concordYmlSource) throws Exception {
        String orgName = "org_" + randomString();
        concord.organizations().create(orgName);

        String projectName = "project_" + randomString();
        concord.projects().create(orgName, projectName);

        var projectsApi = new ProjectsApi(concord.apiClient());
        var project = projectsApi.getProject(orgName, projectName);
        project.setAcceptsRawPayload(true);
        project.rawPayloadMode(ProjectEntry.RawPayloadModeEnum.EVERYONE);
        projectsApi.createOrUpdateProject(orgName, project);

        createSecret(orgName, "dev-akeyless-id", getAccessId().getBytes(StandardCharsets.UTF_8));
        createSecret(orgName, "dev-akeyless-key", getAccessKey().getBytes(StandardCharsets.UTF_8));

        //  ---

        Payload payload = new Payload()
                .org(orgName)
                .project(projectName)
                .concordYml(new String(readToBytes(concordYmlSource))
                        .replace("%%version%%", CURRENT_VERSION));

        //  ---

        ConcordProcess proc = concord.processes().start(payload);
        proc.waitForStatus(ProcessEntry.StatusEnum.FINISHED);

        proc.assertLog(".*FINISHED.*");
    }

    private static void createSecret(String orgName, String name, byte[] value) throws ApiException {
        NewSecretQuery accessKeyQuery = NewSecretQuery.builder()
                .org(orgName)
                .name(name)
                .build();
        concord.secrets().createSecret(accessKeyQuery, value);
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

    private static byte[] readToBytes(String resource) {
        try (InputStream in = TaskIT.class.getResourceAsStream(resource)) {
            if (Objects.isNull(in)) {
                throw new IllegalStateException("Failed to load resource: " + resource);
            }

            return IOUtils.toByteArray(in);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected static String getEnv(String name, String defValue) {
        String envValue = System.getenv(name);

        if (envValue == null) {
            return defValue;
        } else {
            return envValue;
        }
    }

}
