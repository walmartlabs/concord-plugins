package com.walmartlabs.concord.plugins.git.actions;

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

import com.walmartlabs.concord.common.ConfigurationUtils;
import com.walmartlabs.concord.plugins.git.GitHubTaskParams;
import com.walmartlabs.concord.plugins.git.model.GitHubApiInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.Objects;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "GH_TEST_TOKEN", matches = ".+")
public class GetTagActionTest {

    @Test
    public void testOkAction() {
        var apiInfo = GitHubApiInfo.builder()
                .baseUrl("https://github.com")
                .accessTokenProvider(() -> Objects.requireNonNull(System.getenv("GH_TEST_TOKEN")))
                .build();

        var input = new GitHubTaskParams.GetTag("walmartlabs", "concord-plugins", "ab7e34aff888af24a1223839b3afc5588b061db3", null, true);

        var action = new GetTagAction();
        var result = action.execute(UUID.randomUUID(), apiInfo, false, input);

        assertNotNull(result);
        assertEquals("2.11.0", ConfigurationUtils.get(result, "tag", "tag"));
        assertEquals("e19883074c5d45548e630271d73733f2ef3f8e74", ConfigurationUtils.get(result, "tag", "object", "sha"));
    }

    @Test
    public void testOkByName() {
        var apiInfo = GitHubApiInfo.builder()
                .baseUrl("https://github.com")
                .accessTokenProvider(() -> Objects.requireNonNull(System.getenv("GH_TEST_TOKEN")))
                .build();

        var input = new GitHubTaskParams.GetTag("walmartlabs", "concord-plugins", null, "2.11.0", true);

        var action = new GetTagAction();
        var result = action.execute(UUID.randomUUID(), apiInfo, false, input);

        assertNotNull(result);
        assertEquals("2.11.0", ConfigurationUtils.get(result, "tag", "tag"));
        assertEquals("e19883074c5d45548e630271d73733f2ef3f8e74", ConfigurationUtils.get(result, "tag", "object", "sha"));
    }

    @Test
    public void testNotFoundByName() {
        var apiInfo = GitHubApiInfo.builder()
                .baseUrl("https://github.com")
                .accessTokenProvider(() -> Objects.requireNonNull(System.getenv("GH_TEST_TOKEN"), "GH_TEST_TOKEN is enpty"))
                .build();

        var input = new GitHubTaskParams.GetTag("walmartlabs", "concord-plugins", null, "xyz", false);

        var action = new GetTagAction();
        var result = action.execute(UUID.randomUUID(), apiInfo, false, input);

        assertNotNull(result);
        assertNull(result.get("tag"));
    }

    @Test
    public void testNotFoundBySha() {
        var apiInfo = GitHubApiInfo.builder()
                .baseUrl("https://github.com")
                .accessTokenProvider(() -> Objects.requireNonNull(System.getenv("GH_TEST_TOKEN"), "GH_TEST_TOKEN is empty"))
                .build();

        var input = new GitHubTaskParams.GetTag("walmartlabs", "concord-plugins", "1234567890123456789012345678901234567890", null, false);

        var action = new GetTagAction();
        var result = action.execute(UUID.randomUUID(), apiInfo, false, input);

        assertNotNull(result);
        assertNull(result.get("tag"));
    }
}
