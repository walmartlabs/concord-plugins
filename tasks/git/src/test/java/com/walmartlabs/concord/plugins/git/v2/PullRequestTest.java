package com.walmartlabs.concord.plugins.git.v2;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2024 Walmart Inc., Concord Authors
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

import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.walmartlabs.concord.plugins.git.GitHubTask;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class PullRequestTest {

    @RegisterExtension
    protected static WireMockExtension httpRule = WireMockExtension.newInstance()
            .options(wireMockConfig()
                    .dynamicPort()
                    .usingFilesUnderClasspath("wiremock/pullRequest")
                    .notifier(new ConsoleNotifier(false))) // set to true for verbose logging
            .build();

    @Test
    void testCreate() {
        Map<String, Object> input = Map.of(
                "action", "createPr",
                "accessToken", "mockToken",
                "org", "octocat",
                "repo", "mock-repo",
                "prTitle", "Feature A",
                "prBody", "Feature A implements the requirements from request 12.",
                "prSourceBranch", "feature-a",
                "prDestinationBranch", "master",
                "apiUrl", httpRule.baseUrl()
        );

        var result = new GitHubTask(UUID.randomUUID()).execute(input, Map.of());

        httpRule.verify(1, postRequestedFor(urlEqualTo("/api/v3/repos/octocat/mock-repo/pulls")));

        assertEquals(1347, result.get("prId"));
    }

    @Test
    void testMerge() {
        Map<String, Object> input = Map.of(
                "action", "mergePr",
                "accessToken", "mockToken",
                "org", "octocat",
                "repo", "mock-repo",
                "prId", 1347,
                "commitMessage", "my custom merge commit message",
                "mergeMethod", "squash",
                "apiUrl", httpRule.baseUrl()
        );

        new GitHubTask(UUID.randomUUID()).execute(input, Map.of());

        httpRule.verify(1, putRequestedFor(urlEqualTo("/api/v3/repos/octocat/mock-repo/pulls/1347/merge")));
    }

    @Test
    void testComment() {
        Map<String, Object> input = Map.of(
                "action", "commentPr",
                "accessToken", "mockToken",
                "org", "octocat",
                "repo", "mock-repo",
                "prId", 1347,
                "prComment", "Some pr comment",
                "apiUrl", httpRule.baseUrl()
        );

        new GitHubTask(UUID.randomUUID()).execute(input, Map.of());

        httpRule.verify(1, getRequestedFor(urlEqualTo("/api/v3/repos/octocat/mock-repo/pulls/1347")));
        httpRule.verify(1, postRequestedFor(urlEqualTo("/api/v3/repos/octocat/mock-repo/issues/1347/comments")));
    }

    @Test
    void testGetCommitList() {
        Map<String, Object> input = Map.of(
                "action", "getPrCommitList",
                "accessToken", "mockToken",
                "org", "octocat",
                "repo", "mock-repo",
                "prId", 1347,
                "apiUrl", httpRule.baseUrl()
        );

        var result = new GitHubTask(UUID.randomUUID()).execute(input, Map.of());

        httpRule.verify(1, getRequestedFor(urlEqualTo("/api/v3/repos/octocat/mock-repo/pulls/1347/commits?per_page=100&page=1")));

        var commits = assertInstanceOf(List.class, result.get("commits"));
        assertEquals(1, commits.size());
    }

    @Test
    void testClose() {
        Map<String, Object> input = Map.of(
                "action", "closePr",
                "accessToken", "mockToken",
                "org", "octocat",
                "repo", "mock-repo",
                "prId", 1347,
                "apiUrl", httpRule.baseUrl()
        );

        new GitHubTask(UUID.randomUUID()).execute(input, Map.of());

        httpRule.verify(1, getRequestedFor(urlEqualTo("/api/v3/repos/octocat/mock-repo/pulls/1347")));
        httpRule.verify(1, postRequestedFor(urlEqualTo("/api/v3/repos/octocat/mock-repo/pulls/1347")));
    }

    @Test
    void testGet() {
        Map<String, Object> input = Map.of(
                "action", "getPr",
                "accessToken", "mockToken",
                "org", "octocat",
                "repo", "mock-repo",
                "prNumber", 1347,
                "apiUrl", httpRule.baseUrl()
        );

        var result = new GitHubTask(UUID.randomUUID()).execute(input, Map.of());

        httpRule.verify(1, getRequestedFor(urlEqualTo("/api/v3/repos/octocat/mock-repo/pulls/1347")));

        var pr = assertInstanceOf(Map.class, result.get("pr"));
        assertEquals(1347, pr.get("number"));
    }

    @Test
    void getList() {
        Map<String, Object> input = Map.of(
                "action", "getPrList",
                "accessToken", "mockToken",
                "org", "octocat",
                "repo", "mock-repo",
                "apiUrl", httpRule.baseUrl()
        );

        var result = new GitHubTask(UUID.randomUUID()).execute(input, Map.of());

        httpRule.verify(1, getRequestedFor(urlEqualTo("/api/v3/repos/octocat/mock-repo/pulls?state=open&per_page=100&page=1")));

        var prList = (List<Map<String, Object>>) assertInstanceOf(List.class, result.get("prList"));
        assertEquals(1, prList.size());
        assertEquals(1347, prList.get(0).get("number"));
    }

    @Test
    void testGetFiles() {
        Map<String, Object> input = Map.of(
                "action", "getPRfiles",
                "accessToken", "mockToken",
                "org", "octocat",
                "repo", "mock-repo",
                "prNumber", 1347,
                "apiUrl", httpRule.baseUrl()
        );

        var result = new GitHubTask(UUID.randomUUID()).execute(input, Map.of());

        httpRule.verify(1, getRequestedFor(urlEqualTo("/api/v3/repos/octocat/mock-repo/pulls/1347/files?per_page=100&page=1")));

        var prFilesAdded = assertInstanceOf(List.class, result.get("prFilesAdded"));
        assertEquals(1, prFilesAdded.size());
        var prFilesRemoved = assertInstanceOf(List.class, result.get("prFilesRemoved"));
        assertEquals(0, prFilesRemoved.size());
        var prFiles = assertInstanceOf(List.class, result.get("prFiles"));
        assertEquals(1, prFiles.size());
        var prFilesAny = assertInstanceOf(List.class, result.get("prFilesAny"));
        assertEquals(1, prFilesAny.size());
        var prFilesModified = assertInstanceOf(List.class, result.get("prFilesModified"));
        assertEquals(0, prFilesModified.size());
    }
}
