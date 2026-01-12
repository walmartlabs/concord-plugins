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
import com.walmartlabs.concord.runtime.v2.sdk.UserDefinedException;
import com.walmartlabs.concord.sdk.MapUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "GH_TEST_TOKEN", matches = ".+")
public class ListCommitsActionTest {

    @Test
    public void testOkAction() {
        var apiInfo = GitHubApiInfo.builder()
                .baseUrl("https://github.com")
                .accessTokenProvider(() -> Objects.requireNonNull(System.getenv("GH_TEST_TOKEN")))
                .build();

        var input = new GitHubTaskParams.ListCommits("walmartlabs", "concord-plugins",
                "2.9.0", null,
                "bf266671c4402bd0d8772556674d8aef7ebe4ab0", "0fd28388364c00ec8b3e37cd2e666a94c3d7edfb",
                2, 20, null);

        var action = new ListCommitsAction();
        var result = action.execute(UUID.randomUUID(), apiInfo, input);
        assertNotNull(result);

        var commits = MapUtils.<Map<String, Object>>assertList(result, "commits");
        assertEquals(5, commits.size());

        assertEquals(List.of(
                "0fd28388364c00ec8b3e37cd2e666a94c3d7edfb", "c4c395977f9ede2aec9404d4251df98c5afd6928",
                "739608a0dea7f7317a8ae494c05c09c5eb479b39", "9259740702286d9492f6f9cf21bfecddd49247ce",
                "6888b29db4d3d2edcd4c8ffc99a7e9a75ef818d9"), commits.stream().map(c -> c.get("sha")).toList());
    }

    @Test
    public void testOkWithFilter() {
        var apiInfo = GitHubApiInfo.builder()
                .baseUrl("https://github.com")
                .accessTokenProvider(() -> Objects.requireNonNull(System.getenv("GH_TEST_TOKEN")))
                .build();

        var input = new GitHubTaskParams.ListCommits("walmartlabs", "concord-plugins",
                "2.9.0", null,
                "bf266671c4402bd0d8772556674d8aef7ebe4ab0", "0fd28388364c00ec8b3e37cd2e666a94c3d7edfb",
                2, 20, Pattern.compile("^ssh: add basic SSH and SCP tasks.*"));

        var action = new ListCommitsAction();
        var result = action.execute(UUID.randomUUID(), apiInfo, input);
        assertNotNull(result);

        var commits = MapUtils.<Map<String, Object>>assertList(result, "commits");
        assertEquals(1, commits.size());

        assertEquals(List.of(
                "c4c395977f9ede2aec9404d4251df98c5afd6928"),
                commits.stream().map(c -> c.get("sha")).toList());
    }
}
