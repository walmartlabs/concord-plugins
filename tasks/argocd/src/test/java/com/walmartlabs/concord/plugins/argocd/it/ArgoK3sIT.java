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
import com.walmartlabs.concord.client2.ProcessEntry;
import com.walmartlabs.concord.client2.ProjectEntry;
import com.walmartlabs.concord.client2.ProjectsApi;
import com.walmartlabs.concord.plugins.argocd.ArgoCdClient;
import com.walmartlabs.concord.plugins.argocd.ImmutableTestBasicAuth;
import com.walmartlabs.concord.plugins.argocd.ImmutableTestGetParams;
import com.walmartlabs.concord.plugins.argocd.ImmutableTestLdapAuth;
import com.walmartlabs.concord.plugins.argocd.TaskParams;
import org.immutables.value.Value;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

import static ca.ibodrov.concord.testcontainers.Utils.randomString;
import static com.github.tomakehurst.wiremock.core.Version.getCurrentVersion;
import static com.walmartlabs.concord.plugins.argocd.it.ArgoIT.readToBytes;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ArgoK3sIT {

    @RegisterExtension
    public static final ConcordRule concord = new ConcordRule()
            .mode(Concord.Mode.DOCKER)
            .streamServerLogs(false)
            .streamAgentLogs(false)
            .useLocalMavenRepository(true);

    private static final Logger log = LoggerFactory.getLogger(ArgoK3sIT.class);
    private static final String CURRENT_VERSION = getCurrentVersion();
    private static final ItArgs IT_ARGS = ItArgs.create();

    @BeforeAll
    static void setUp() {
        log.info("Concord IT server login: {}/#/login?useApiKey=true", concord.apiBaseUrl());
        log.info("Concord IT admin token: {}", concord.environment().apiToken());
    }

    @Test
    void testBasicAuth() throws Exception {
        var in = ImmutableTestGetParams.builder()
                .app("test-app")
                .baseUrl(IT_ARGS.baseUrl())
                .action(TaskParams.Action.GETPROJECT)
                .validateCerts(false)
                .auth(ImmutableTestBasicAuth.builder()
                        .username("admin")
                        .password(IT_ARGS.basicAdminPassword())
                        .build())
                .build();

        var out = new ArgoCdClient(in).listApplicationSets(List.of(), null);

        assertNotNull(out);
    }

    @Test
    void testLdapAuth() throws Exception {
        var in = ImmutableTestGetParams.builder()
                .app("test-app")
                .baseUrl(IT_ARGS.baseUrl())
                .action(TaskParams.Action.GETPROJECT)
                .validateCerts(false)
                .auth(ImmutableTestLdapAuth.builder()
                        .username(IT_ARGS.ldapUsername())
                        .password(IT_ARGS.ldapPassword())
                        .build())
                .build();

        var out = new ArgoCdClient(in).listApplicationSets(List.of(), null);

        assertNotNull(out);
    }

    @Test
    void testFlow() throws Exception {
        String concordYmlSource = "full/concord.yml";

        String orgName = "org_" + randomString();
        concord.organizations().create(orgName);

        String projectName = "project_" + randomString();
        concord.projects().create(orgName, projectName);

        var projectsApi = new ProjectsApi(concord.apiClient());
        var project = projectsApi.getProject(orgName, projectName);
        project.rawPayloadMode(ProjectEntry.RawPayloadModeEnum.EVERYONE);
        projectsApi.createOrUpdateProject(orgName, project);

        var payload = new Payload()
                .org(orgName)
                .project(projectName)
                .arg("argoBaseUrl", IT_ARGS.baseUrl())
                .arg("argoUsername", IT_ARGS.ldapUsername())
                .arg("argoPassword", IT_ARGS.ldapPassword())
                .arg("appNamespace", IT_ARGS.appNamespace())
                .concordYml(new String(readToBytes(concordYmlSource))
                        .replace("%%version%%", CURRENT_VERSION));

        //  ---

        ConcordProcess proc = concord.processes().start(payload);
        proc.waitForStatus(ProcessEntry.StatusEnum.FINISHED);

        //  ---

        // should have forces a timeout with short setting, then success on second longer-timeout attempt
        proc.assertLogAtLeast(".*Call attempt timed out after 1000ms.*", 1);
        proc.assertLog(".*got app status: Healthy.*");
        proc.assertLog(".*Deleted app: true.*");
    }

    protected static String getEnv(String name, String defValue) {
        String envValue = System.getenv(name);

        if (envValue == null) {
            return defValue;
        } else {
            return envValue;
        }
    }

    @Value.Immutable
    @Value.Style(jdkOnly = true)
    public interface ItArgs {
        Path kubeconfigPath();

        String baseUrl();

        String basicAdminPassword();

        String ldapUsername();

        String ldapPassword();

        String appNamespace();

        static ItArgs create() {
            var propsLocation = getEnv("ARGO_IT_TEST_INPUT", "local_argo/playbook/roles/argocd/files/generated/test_input.properties");
            var testInput = Paths.get(propsLocation);

            if (propsLocation.isBlank()) {
                throw new IllegalArgumentException("ARGO_IT_TEST_INPUT is blank");
            }

            if (!Files.exists(testInput)) {
                log.info("Test input file does not exist");
                log.info("HINT: Run ./installArgo.sh to generate file, or set " +
                        "ARGO_IT_TEST_INPUT to the path of the test input file");
                throw new IllegalArgumentException("Test input file does not exist: " + testInput);
            }

            Properties props = new Properties();
            try (var in = Files.newInputStream(testInput)) {
                props.load(in);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            return ImmutableItArgs.builder()
                    .kubeconfigPath(Paths.get(props.getProperty("ARGO_IT_KUBECONFIG_PATH")))
                    .baseUrl(props.getProperty("ARGO_IT_BASE_API"))
                    .basicAdminPassword(props.getProperty("ARGO_IT_BASIC_ADMIN_PASSWORD"))
                    .ldapUsername(props.getProperty("ARGO_IT_LDAP_USERNAME"))
                    .ldapPassword(props.getProperty("ARGO_IT_LDAP_PASSWORD"))
                    .appNamespace(props.getProperty("ARGO_IT_APP_NAMESPACE"))
                    .build();
        }
    }

}
