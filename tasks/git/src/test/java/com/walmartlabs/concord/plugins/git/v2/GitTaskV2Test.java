package com.walmartlabs.concord.plugins.git.v2;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2020 Walmart Inc., Concord Authors
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

import com.walmartlabs.concord.common.IOUtils;
import com.walmartlabs.concord.common.secret.UsernamePassword;
import com.walmartlabs.concord.plugins.git.GitTask;
import com.walmartlabs.concord.plugins.git.TokenSecret;
import com.walmartlabs.concord.plugins.git.Utils;
import com.walmartlabs.concord.runtime.v2.sdk.MapBackedVariables;
import com.walmartlabs.concord.runtime.v2.sdk.SecretService;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import com.walmartlabs.concord.runtime.v2.sdk.WorkingDirectory;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class GitTaskV2Test {

    private Path workDir;

    @Before
    public void setUp() throws Exception {
        this.workDir = Files.createTempDirectory("test");
    }

    public void tearDown() throws Exception {
        if (this.workDir != null && Files.exists(this.workDir)) {
            IOUtils.deleteRecursively(workDir);
        }
    }

    @Test
    public void test() throws Exception {
        Map<String, Object> input = new HashMap<>();
        input.put(GitTask.ACTION_KEY, GitTask.Action.CLONE.name());
        input.put(GitTask.GIT_URL, "https://github.com/walmartlabs/concord-plugins.git");

        GitTaskV2 task = new GitTaskV2(mock(SecretService.class), new WorkingDirectory(workDir));
        TaskResult.SimpleResult result = task.execute(new MapBackedVariables(input));
        assertTrue(result.ok());
    }

    @Test
    public void testHideSensitiveData() {
        // some strings which could be a password containing unintentional, invalid regex patterns
        List<String> inputs  = Arrays.asList("simple123", "[_}34@%$");

        for (String input : inputs) {
            UsernamePassword testUP = new UsernamePassword("user", input.toCharArray());
            String result = Utils.hideSensitiveData("beforeText " + input + " afterText", testUP);

            assertEquals("beforeText *** afterText", result);
        }

        for (String input : inputs) {
            TokenSecret testToken = new TokenSecret(input);
            String result = Utils.hideSensitiveData("beforeText " + input + " afterText", testToken);

            assertEquals("beforeText *** afterText", result);
        }
    }
}
