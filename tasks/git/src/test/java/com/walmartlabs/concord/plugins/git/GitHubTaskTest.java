package com.walmartlabs.concord.plugins.git;

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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Disabled("Requires a GitHub API key")
public class GitHubTaskTest {

    private static final String ORG = System.getenv("GITHUB_ORG");
    private static final String REPO = System.getenv("GITHUB_REPO");
    private static final String ACCESS_TOKEN = System.getenv("GITHUB_ACCESS_TOKEN");
    private static final Map<String, Object> DEFAULTS = Map.of("apiUrl", "https://api.github.com");

    @Test
    public void test() throws Exception {
        var task = new GitHubTask();

        var result = task.execute(Map.of(
                "accessToken", ACCESS_TOKEN,
                "org", ORG,
                "repo", REPO,
                "action", "createPr",
                "prTitle", "test#" + System.currentTimeMillis(),
                "prBody", "just a test",
                "prSourceBranch", "pr-test",
                "prDestinationBranch", "main"
        ), DEFAULTS);

        var prId = result.get("prId");
        assertNotNull(prId);

        result = task.execute(Map.of(
                "accessToken", ACCESS_TOKEN,
                "org", ORG,
                "repo", REPO,
                "action", "getPrCommitList",
                "prTitle", "test#" + System.currentTimeMillis(),
                "prId", prId
        ), DEFAULTS);

        var commits = assertInstanceOf(List.class, result.get("commits"));
        assertEquals(1, commits.size());

        result = task.execute(Map.of(
                "accessToken", ACCESS_TOKEN,
                "org", ORG,
                "repo", REPO,
                "action", "commentPr",
                "prId", prId,
                "prComment", "comment#" + System.currentTimeMillis()
        ), DEFAULTS);

        assertNotNull(result.get("id"));

        result = task.execute(Map.of(
                "accessToken", ACCESS_TOKEN,
                "org", ORG,
                "repo", REPO,
                "action", "closePr",
                "prId", prId
        ), DEFAULTS);

        assertTrue(result.isEmpty());

        result = task.execute(Map.of(
                "accessToken", ACCESS_TOKEN,
                "org", ORG,
                "repo", REPO,
                "action", "getContent",
                "ref", "main",
                "path", "TEST"
        ), DEFAULTS);

        var contents = assertInstanceOf(List.class, result.get("contents"));
        assertEquals(1, contents.size());
    }
}
