package com.walmartlabs.concord.plugins.terraform;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2019 Walmart Inc.
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

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.walmartlabs.concord.plugins.terraform.backend.BackendFactoryV1;
import com.walmartlabs.concord.sdk.*;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.any;
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

@Ignore
public class TerraformTaskTest extends AbstractTerraformTest {

    private SecretService secretService;

    @Before
    public void setup() throws Exception {
        abstractSetup();
        objectStorage = createObjectStorage(wireMockRule);
        secretService = createSecretService(workDir);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void test() throws Exception {
        Map<String, Object> args = baseArguments(workDir, dstDir, Action.PLAN.name());
        args.put(TaskConstants.VARS_FILES, varFiles());
        args.put(TaskConstants.EXTRA_VARS_KEY, extraVars());
        args.put(TaskConstants.GIT_SSH_KEY, gitSsh());

        Context ctx = new MockContext(args);
        backendManager = new BackendFactoryV1(ctx, lockService, objectStorage);

        TerraformTask t = new TerraformTask(secretService, lockService,
                objectStorage, dependencyManager, dockerService);

        t.execute(ctx);

        // ---

        Map<String, Object> result = (Map<String, Object>) ctx.getVariable(TaskConstants.RESULT_KEY);
        assertTrue((boolean) result.get("ok"));
        assertNotNull(result.get("planPath"));

        // ---

        verify(lockService, times(1)).projectLock(any(), any());
        verify(lockService, times(1)).projectUnlock(any(), any());

        // ---

        System.out.println("===================================================================================");

        args = baseArguments(workDir, dstDir, Action.APPLY.name());
        args.put(TaskConstants.DESTROY_KEY, true);
        args.put(TaskConstants.PLAN_KEY, result.get("planPath"));

        ctx = new MockContext(args);
        t.execute(ctx);
        result = ContextUtils.getMap(ctx, "result");
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
        assertOutput(".*name_from_varfile0 = \"?bob\"?.*", ((String) result.get("output")));
        assertOutput(".*time_from_varfile1 = \"?now\"?.*", ((String) result.get("output")));

        // ---

        System.out.println("===================================================================================");

        //
        // Simulate having some outputs being created in the terraform state. The output action will not work
        // correctly without outputs present in the terraform state.
        //
        String terraformStateWithOutputs = responseTemplate("terraform.tfstate");
        wireMockRule.stubFor(get("/test").willReturn(aResponse().withBody(terraformStateWithOutputs).withStatus(200)));

        args = baseArguments(workDir, dstDir, Action.OUTPUT.name());
        ctx = new MockContext(args);
        t.execute(ctx);

        // Validate expected output values were returned in 'data' attribute/key of 'result' variable
        result = ContextUtils.getMap(ctx, "result");
        String nameResult = findInMap(result, "data.name_from_varfile0.value");
        String timeResult = findInMap(result, "data.time_from_varfile1.value");

        assertEquals("bob", nameResult);
        assertEquals("now", timeResult);


        //
        // Cleanup time. State should be saved by our custom wiremock transformer.
        // Without the state, terraform won't actually delete the resource(s)
        args = baseArguments(workDir, dstDir, Action.DESTROY.name());
        args.put(TaskConstants.VARS_FILES, varFiles());
        args.put(TaskConstants.EXTRA_VARS_KEY, extraVars());
        ctx = new MockContext(args);
        t.execute(ctx);

        // Validate result
        result = ContextUtils.getMap(ctx, "result");
        assertTrue(MapUtils.getBoolean(result, "ok", false));
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
        when(ss.exportKeyAsFile(any(), any(), any(), any(), any(), any())).thenReturn(m);

        return ss;
    }

    public static ObjectStorage createObjectStorage(WireMockRule wireMockRule) throws Exception {
        String osAddress = String.format("http://%s:%s/test", apiHostName(), wireMockRule.port());

        wireMockRule.stubFor(get("/test").willReturn(aResponse()
                .withHeader("keep_payload", Boolean.FALSE.toString())
                .withStatus(404)));
        wireMockRule.stubFor(post("/test").willReturn(aResponse()
                .withHeader("keep_payload", Boolean.TRUE.toString())
                .withStatus(200)));

        ObjectStorage os = mock(ObjectStorage.class);
        when(os.createBucket(any(), anyString())).thenReturn(ImmutableBucketInfo.builder()
                .address(osAddress)
                .build());

        return os;
    }
}
