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

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.walmartlabs.concord.common.IOUtils;
import com.walmartlabs.concord.sdk.*;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

//
// To run this test you need to set the following envars set:
//
// CONCORD_TMP_DIR	= /tmp/concord
// AWS_ACCESS_KEY	= <your_aws_access_key>
// AWS_SECRET_KEY	= <your_aws_secret_key>
// TF_TEST_FILE	    = <path_to>/concord-plugins/tasks/terraform/src/test/terraform/main.tf
// PRIVATE_KEY_PATH = <your_aws_pem_file>
//

@Ignore
public class TerraformTaskTest {

    private String basedir;

    @Before
    public void setup() {
        basedir = new File("").getAbsolutePath();
    }

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.wireMockConfig()
            .port(12345));

    @Test
    @SuppressWarnings("unchecked")
    public void test() throws Exception {
        Path workDir = IOUtils.createTempDir("test");
        Files.createDirectories(workDir);
        Path dstDir = workDir;
        Path testFile = Paths.get(System.getenv("TF_TEST_FILE"));
        Files.copy(testFile, dstDir.resolve(testFile.getFileName()));

        // ---

        LockService lockService = mock(LockService.class);
        ObjectStorage objectStorage = createObjectStorage(wireMockRule);
        SecretService secretService = createSecretService(workDir);

        TerraformTask t = new TerraformTask(lockService, objectStorage, secretService);

        // ---

        Map<String, Object> args = baseArguments(workDir, dstDir, TerraformTask.Action.PLAN.name());
        //
        // We place our user supplied var files in the workspace and they are declared as being relative
        // to the Concord workspace. So we copy them into the workspace at the root so we just refer to
        // them as "varfile0.tfvars" and varfile1.tfvars.
        //
        Path varfile0 = varFile("varfile0.tfvars");
        Files.copy(varfile0, dstDir.resolve(varfile0.getFileName()));
        Path varfile1 = varFile("varfile1.tfvars");
        Files.copy(varfile1, dstDir.resolve(varfile1.getFileName()));


        args.put(Constants.VARS_FILES, new ArrayList<>(Arrays.asList("varfile0.tfvars", "varfile1.tfvars")));
        Map<String, Object> extraVars = new HashMap<>();
        extraVars.put("aws_access_key", System.getenv("AWS_ACCESS_KEY"));
        extraVars.put("aws_secret_key", System.getenv("AWS_SECRET_KEY"));
        args.put(Constants.EXTRA_VARS_KEY, extraVars);

        Map<String, Object> gitSsh = new HashMap<>();
        gitSsh.put(GitSshWrapper.PRIVATE_KEYS_KEY, Collections.singletonList(Files.createTempFile("test", ".key").toAbsolutePath().toString()));
        gitSsh.put(GitSshWrapper.SECRETS_KEY, Collections.singletonList(Collections.singletonMap("secretName", "test")));
        args.put(Constants.GIT_SSH_KEY, gitSsh);

        Context ctx = new MockContext(args);
        t.execute(ctx);

        // ---

        Map<String, Object> result = (Map<String, Object>) ctx.getVariable(Constants.RESULT_KEY);
        assertTrue((boolean) result.get("ok"));
        assertNotNull(result.get("planPath"));

        // ---

        verify(lockService, times(1)).projectLock(any(), any());
        verify(lockService, times(1)).projectUnlock(any(), any());

        // ---

        System.out.println("===================================================================================");

        args = baseArguments(workDir, dstDir, TerraformTask.Action.APPLY.name());
        args.put(Constants.DESTROY_KEY, true);
        args.put(Constants.PLAN_KEY, result.get("planPath"));

        ctx = new MockContext(args);
        t.execute(ctx);
        result = (Map<String, Object>) ctx.getVariable(Constants.RESULT_KEY);
        System.out.println(result);

        //
        // Check the output contains our two variables populated with values from our var files
        //
        // Outputs:
        //
        // message = hello!
        // name_from_varfile0 = bob
        // time_from_varfile1 = now
        //
        assertTrue(((String)result.get("output")).contains("name_from_varfile0 = bob"));
        assertTrue(((String)result.get("output")).contains("time_from_varfile1 = now"));

        // ---

        System.out.println("===================================================================================");

        //
        // Simulate having some outputs being created in the terraform state. The output action will not work
        // correctly without outputs present in the terraform state.
        //
        String terraformStateWithOutputs = responseTemplate("terraform.tfstate");
        wireMockRule.stubFor(get("/test").willReturn(aResponse().withBody(terraformStateWithOutputs).withStatus(200)));

        args = baseArguments(workDir, dstDir, TerraformTask.Action.OUTPUT.name());
        ctx = new MockContext(args);
        t.execute(ctx);
    }

    private static ObjectStorage createObjectStorage(WireMockRule wireMockRule) throws Exception {
        String osAddress = "http://localhost:" + wireMockRule.port() + "/test";
        wireMockRule.stubFor(get("/test").willReturn(aResponse().withStatus(404)));
        wireMockRule.stubFor(post("/test").willReturn(aResponse().withStatus(200)));

        ObjectStorage os = mock(ObjectStorage.class);
        when(os.createBucket(any(), anyString())).thenReturn(ImmutableBucketInfo.builder()
                .address(osAddress)
                .build());

        return os;
    }

    private static SecretService createSecretService(Path workDir) throws Exception {
        Path src = Paths.get(System.getenv("PRIVATE_KEY_PATH"));
        Path dst = Files.createTempFile(workDir, "private", ".key");
        Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);

        Map<String, String> m = Collections.singletonMap("private", workDir.relativize(dst).toString());

        SecretService ss = mock(SecretService.class);
        when(ss.exportKeyAsFile(any(), any(), any(), any(), any(), any())).thenReturn(m);

        return ss;
    }

    private Map<String, Object> baseArguments(Path workDir, Path dstDir, String actionKey) {
        Map<String, Object> args = new HashMap<>();
        args.put(com.walmartlabs.concord.sdk.Constants.Request.PROCESS_INFO_KEY, Collections.singletonMap("sessionKey", "xyz"));
        args.put(com.walmartlabs.concord.sdk.Constants.Context.WORK_DIR_KEY, workDir.toAbsolutePath().toString());
        args.put(Constants.ACTION_KEY, actionKey);
        args.put(Constants.DEBUG_KEY, true);
        args.put(Constants.STATE_ID_KEY, "testState");
        args.put(Constants.DIR_KEY, dstDir.toAbsolutePath().toString());
        return args;
    }

    private Path varFile(String name) {
        return new File(basedir, "src/test/terraform/" + name).toPath();
    }

    private String responseTemplate(String name) throws IOException {
        return new String(Files.readAllBytes(new File(basedir, "src/test/terraform/" + name).toPath()));
    }
}
