package com.walmartlabs.concord.plugins.terraform;

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

import com.squareup.okhttp.OkHttpClient;
import com.walmartlabs.concord.ApiClient;
import com.walmartlabs.concord.client.ApiClientConfiguration;
import com.walmartlabs.concord.client.ApiClientFactory;
import com.walmartlabs.concord.client.ConcordApiClient;
import com.walmartlabs.concord.runtime.v2.sdk.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.*;

//
// To run this test you need to set the following env vars set:
//
// CONCORD_TMP_DIR      = /tmp/concord
// AWS_ACCESS_KEY       = <your_aws_access_key>
// AWS_SECRET_KEY       = <your_aws_secret_key>
// PRIVATE_KEY_PATH     = <your_aws_pem_file>
// TF_TEST_FILE	        = <path_to>/concord-plugins/tasks/terraform/src/test/terraform/main.tf or another file you want to test
// TF_TEST_DOCKER_IMAGE = docker image in which to execute terraform
// TF_TEST_HOSTNAME     = local hostname that's compatible both inside and outside a docker container
// TF_TOOL_URL          = optional, terraform binary zip URL
//
// Alternatively you can use the following:
//
// - An ~/.aws/credentials with a [concord-integration-tests] stanza where the access key id and secret key will be taken from
// - The default src/test/terraform/main.tf test file
// - A ~/.aws/conconrd-integration-tests.pem that will be used as the private key
// - The CONCORD_TMP_DIR envar will be set for you to /tmp/concord
//
// Once setup this should just allow you to run the test.
//
// TODO: need to test destroy, computes are left in AWS
// TODO: split test apart to prepare for testing OCI/GCP
//
@Disabled
class TerraformTaskV2Test extends AbstractTerraformTest {
    private ApiClient apiClient;
    private SecretService secretService;
    private final LockService lockService = mock(LockService.class);

    @BeforeEach
    public void setup() throws Exception {
        abstractSetup();
        secretService = createSecretService(workDir);

        wireMockRule.stubFor(get(urlPathMatching("/api/v1/org/.*/jsonstore/.*"))
                .willReturn(aResponse()
                        .withHeader("keep_payload", Boolean.FALSE.toString())
                        .withStatus(404)));
        wireMockRule.stubFor(post(urlPathMatching("/api/v1/org/.*/jsonstore"))
                .willReturn(aResponse()
                        .withHeader("keep_payload", Boolean.FALSE.toString())
                        .withStatus(200)));
        wireMockRule.stubFor(post(urlPathMatching("/api/v1/org/.*/jsonstore/.*"))
                .willReturn(aResponse()
                        .withHeader("keep_payload", Boolean.TRUE.toString())
                        .withStatus(200)));
    }

    @Test
    void test() throws Exception {
        Map<String, Object> args = baseArguments(workDir, dstDir, Action.PLAN.name());
        args.put(TaskConstants.VARS_FILES, varFiles());
        args.put(TaskConstants.EXTRA_VARS_KEY, extraVars());
        args.put(TaskConstants.GIT_SSH_KEY, gitSsh());

        String hostname = apiHostName();
        String apiBaseUrl = String.format("http://%s:%s", hostname, wireMockRule.getPort());

        apiClient = getApiClientFactory().create(ApiClientConfiguration.builder()
                .baseUrl(apiBaseUrl)
                .build());

        TaskResult.SimpleResult result = (TaskResult.SimpleResult) task(args)
                .execute(new MapBackedVariables(args));

        // ---

        assertTrue(result.ok());
        assertNotNull(result.values().get("planPath"));

        // ---

        verify(lockService, times(1)).projectLock(any());
        verify(lockService, times(1)).projectUnlock(any());

        // ---

        System.out.println("===================================================================================");

        args = baseArguments(workDir, dstDir, Action.APPLY.name());
        args.put(TaskConstants.DESTROY_KEY, true);
        args.put(TaskConstants.PLAN_KEY, result.values().get("planPath"));

        result = (TaskResult.SimpleResult) task(args).execute(new MapBackedVariables(args));
        System.out.println(result);

        //
        // Check the output contains our two variables populated with values from our var files
        //
        // Outputs:
        //
        // message = "hello!"
        // name_from_varfile0 = "bob"
        // time_from_varfile1 = "now"
        //
        assertOutput(".*name_from_varfile0 = \"?bob\"?.*", ((String) result.values().get("output")));
        assertOutput(".*time_from_varfile1 = \"?now\"?.*", ((String) result.values().get("output")));

        // ---

        System.out.println("===================================================================================");

        //
        // Simulate having some outputs being created in the terraform state. The output action will not work
        // correctly without outputs present in the terraform state.
        //
        String terraformStateWithOutputs = responseTemplate("terraform.tfstate");
        wireMockRule.stubFor(get("/test").willReturn(aResponse().withBody(terraformStateWithOutputs).withStatus(200)));

        args = baseArguments(workDir, dstDir, Action.OUTPUT.name());
        result = (TaskResult.SimpleResult) task(args).execute(new MapBackedVariables(args));

        // Validate expected output values were returned in 'data' attribute/key of 'result' variable
        String nameResult = findInMap(result.values(), "data.name_from_varfile0.value");
        String timeResult = findInMap(result.values(), "data.time_from_varfile1.value");

        assertEquals("bob", nameResult);
        assertEquals("now", timeResult);


        //
        // Cleanup time. State should be saved by our custom wiremock transformer.
        // Without the state, terraform won't actually delete the resource(s)
        args = baseArguments(workDir, dstDir, Action.DESTROY.name());
        args.put(TaskConstants.VARS_FILES, varFiles());
        args.put(TaskConstants.EXTRA_VARS_KEY, extraVars());
        result = (TaskResult.SimpleResult) task(args).execute(new MapBackedVariables(args));

        // Validate result
        assertTrue(result.ok());

    }

    private TerraformTaskV2 task(Map<String, Object> args) {
        return new TerraformTaskV2(initCtx(args), apiClient, secretService, lockService,
                dependencyManager, dockerService);
    }

    private Context initCtx(Map<String, Object> args) {
        ProjectInfo projectInfo = mock(ProjectInfo.class);
        when(projectInfo.orgName()).thenReturn("test-org");

        ProcessInfo processInfo = mock(ProcessInfo.class);
        when(processInfo.sessionToken()).thenReturn("faketoken");

        ProcessConfiguration processCfg = mock(ProcessConfiguration.class);
        when(processCfg.projectInfo()).thenReturn(projectInfo);
        when(processCfg.processInfo()).thenReturn(processInfo);

        Context ctx = mock(Context.class);
        when(ctx.defaultVariables()).thenReturn(new MapBackedVariables(Collections.emptyMap())); // policy-defaults
        when(ctx.variables()).thenReturn(new MapBackedVariables(Collections.emptyMap())); // process defaults (e.g. configuration.arguments
        when(ctx.secretService()).thenReturn(secretService);
        when(ctx.workingDirectory()).thenReturn(workDir);

        when(ctx.processConfiguration()).thenReturn(processCfg);
        when(ctx.processConfiguration().projectInfo()).thenReturn(projectInfo);

        return ctx;
    }

    private static SecretService createSecretService(Path workDir) throws Exception {
        String pemFileEnvar = System.getenv("PRIVATE_KEY_PATH");
        Path dst = Files.createTempFile(workDir, "private", ".key");
        if (pemFileEnvar != null) {
            Files.copy(Paths.get(pemFileEnvar), dst, StandardCopyOption.REPLACE_EXISTING);
        } else {
            // Look for an ~/.aws/concord-integration-tests.pem file
            File pemFile = new File(System.getProperty("user.hom"), ".aws/" + CONCORD_AWS_CREDENTIALS_KEY + ".pem");
            if (pemFile.exists()) {
                Files.copy(pemFile.toPath(), dst, StandardCopyOption.REPLACE_EXISTING);
            }
        }

        Map<String, String> m = Collections.singletonMap("private", workDir.relativize(dst).toString());

        SecretService ss = mock(SecretService.class);
        Mockito.doAnswer(invocationOnMock -> SecretService.KeyPair.builder()
                .privateKey(dst)
                .publicKey(Paths.get("we/dont/care"))
                .build()).when(ss).exportKeyAsFile(any(), any(), any());

        return ss;

    }

    ApiClientFactory getApiClientFactory() {
        return cfg -> {
            ApiClient apiClient = new ConcordApiClient(cfg.baseUrl(), new OkHttpClient());
            apiClient.setReadTimeout(60000);
            apiClient.setConnectTimeout(10000);
            apiClient.setWriteTimeout(60000);

            apiClient.addDefaultHeader("X-Concord-Trace-Enabled", "true");

            apiClient.setApiKey(cfg.apiKey());
            return apiClient;
        };
    }
}
