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

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.walmartlabs.concord.common.IOUtils;
import com.walmartlabs.concord.sdk.*;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@Ignore
public class TerraformTaskTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.wireMockConfig()
            .port(12345));

    @Test
    @SuppressWarnings("unchecked")
    public void test() throws Exception {
        Path workDir = IOUtils.createTempDir("test");

        Path dstDir = workDir.resolve("myDir");
        Files.createDirectories(dstDir);
//        Path dstDir = workDir;

        Path testFile = Paths.get(System.getenv("TF_TEST_FILE"));
        Files.copy(testFile, dstDir.resolve(testFile.getFileName()));

        // ---

        LockService lockService = mock(LockService.class);
        ObjectStorage objectStorage = createObjectStorage(wireMockRule);
        SecretService secretService = createSecretService(workDir);

        TerraformTask t = new TerraformTask(lockService, objectStorage, secretService);

        // ---

        Map<String, Object> args = new HashMap<>();
        args.put(com.walmartlabs.concord.sdk.Constants.Request.PROCESS_INFO_KEY, Collections.singletonMap("sessionKey", "xyz"));
        args.put(com.walmartlabs.concord.sdk.Constants.Context.WORK_DIR_KEY, workDir.toAbsolutePath().toString());
        args.put(Constants.ACTION_KEY, TerraformTask.Action.PLAN.name());
        args.put(Constants.DEBUG_KEY, true);
        args.put(Constants.DIR_KEY, "myDir");

        Map<String, Object> extraVars = new HashMap<>();
        extraVars.put("aws_access_key", System.getenv("AWS_ACCESS_KEY"));
        extraVars.put("aws_secret_key", System.getenv("AWS_SECRET_KEY"));
        args.put(Constants.EXTRA_VARS_KEY, extraVars);

        Map<String, Object> gitSsh = new HashMap<>();
        gitSsh.put(GitSshWrapper.PRIVATE_KEYS_KEY, Collections.singletonList(Files.createTempFile("test", ".key").toAbsolutePath().toString()));
        gitSsh.put(GitSshWrapper.SECRETS_KEY, Collections.singletonList(Collections.singletonMap("secretName", "test")));
        args.put(Constants.GIT_SSH_KEY, gitSsh);

        args.put(Constants.STATE_ID_KEY, "testState");

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

        args = new HashMap<>();
        args.put(com.walmartlabs.concord.sdk.Constants.Request.PROCESS_INFO_KEY, Collections.singletonMap("sessionKey", "xyz"));
        args.put(com.walmartlabs.concord.sdk.Constants.Context.WORK_DIR_KEY, workDir.toAbsolutePath().toString());
        args.put(Constants.ACTION_KEY, TerraformTask.Action.APPLY.name());
        args.put(Constants.DEBUG_KEY, true);
        args.put(Constants.DESTROY_KEY, true);
        args.put(Constants.DIR_KEY, "myDir");
        args.put(Constants.PLAN_KEY, result.get("planPath"));
        args.put(Constants.STATE_ID_KEY, "testState");

        ctx = new MockContext(args);
        t.execute(ctx);

        // ---

        System.out.println("===================================================================================");

        args = new HashMap<>();
        args.put(com.walmartlabs.concord.sdk.Constants.Context.WORK_DIR_KEY, workDir.toAbsolutePath().toString());
        args.put(Constants.ACTION_KEY, TerraformTask.Action.OUTPUT.name());
        args.put(Constants.DEBUG_KEY, true);
        args.put(Constants.DIR_KEY, "myDir");
        args.put(Constants.STATE_ID_KEY, "testState");

        ctx = new MockContext(args);
        t.execute(ctx);
    }

    private static ObjectStorage createObjectStorage(WireMockRule wireMockRule) throws Exception {
        String osAddress = "http://localhost:" + wireMockRule.port() + "/test";
        wireMockRule.stubFor(WireMock.get("/test").willReturn(WireMock.aResponse().withStatus(404)));
        wireMockRule.stubFor(WireMock.post("/test").willReturn(WireMock.aResponse().withStatus(200)));

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
}
