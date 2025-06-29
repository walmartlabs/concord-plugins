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
import com.walmartlabs.concord.plugins.git.GitSecretService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class RepoTest {

    @RegisterExtension
    protected static WireMockExtension httpRule = WireMockExtension.newInstance()
            .options(wireMockConfig()
                    .dynamicPort()
                    .usingFilesUnderClasspath("wiremock/repo")
                    .notifier(new ConsoleNotifier(false))) // set to true for verbose logging
            .build();

    @Mock
    GitSecretService secretService;

    @Test
    void testCreateRepo() {
        Map<String, Object> input = Map.of(
                "action", "createRepo",
                "accessToken", "mockToken",
                "org", "mock-org",
                "repo", "repo-to-create",
                "apiUrl", httpRule.baseUrl()
        );

        var result = new GitHubTask().execute(input, Map.of(), secretService);

        httpRule.verify(1, getRequestedFor(urlEqualTo("/api/v3/repos/mock-org/repo-to-create")));
        httpRule.verify(1, postRequestedFor(urlEqualTo("/api/v3/orgs/mock-org/repos"))
                .withRequestBody(matchingJsonPath("$.name", equalTo("repo-to-create"))));

        assertEquals("https://api.github.com/repos/mock-org/repo-to-create", result.get("scmUrl"));
        assertEquals("https://github.com/mock-org/repo-to-create.git", result.get("cloneUrl"));
    }

    @Test
    void testCreateRepoHook() {
        Map<String, Object> input = Map.of(
                "action", "createHook",
                "accessToken", "mockToken",
                "org", "octocat",
                "repo", "mock-repo",
                "url", "https://localhost:123/my_callback",
                "content_type", "json",
                "secret", "mock-secret",
                "insecure_ssl", "1",
                "events", List.of("push", "pull_request"),
                "apiUrl", httpRule.baseUrl()
        );

        var result = new GitHubTask().execute(input, Map.of(), secretService);

        httpRule.verify(1, postRequestedFor(urlEqualTo("/api/v3/repos/octocat/mock-repo/hooks")));

        var hook = assertInstanceOf(Map.class, result.get("hook"));
        assertEquals("web", hook.get("name"));
    }

    @Test
    void testCreateTag() {
        Map<String, Object> input = Map.of(
                "action", "createTag",
                "accessToken", "mockToken",
                "org", "octocat",
                "repo", "mock-repo",
                "tagVersion", "v1.0",
                "tagMessage", "new tag",
                "tagAuthorName", "tagAuthor",
                "tagAuthorEmail", "author@email.local",
                "commitSHA", "c3d0be41ecbe669545ee3e94d31ed9a4bc91ee3c",
                "apiUrl", httpRule.baseUrl()
        );

        new GitHubTask().execute(input, Map.of(), secretService);

        httpRule.verify(1, postRequestedFor(urlEqualTo("/api/v3/repos/octocat/mock-repo/git/tags")));
        httpRule.verify(1, postRequestedFor(urlEqualTo("/api/v3/repos/octocat/mock-repo/git/refs")));
    }

    @Test
    void testDeleteTag() {
        Map<String, Object> input = Map.of(
                "action", "deleteTag",
                "accessToken", "mockToken",
                "org", "octocat",
                "repo", "mock-repo",
                "tagName", "v1.0",
                "apiUrl", httpRule.baseUrl()
        );

        new GitHubTask().execute(input, Map.of(), secretService);

        httpRule.verify(1, deleteRequestedFor(urlEqualTo("/api/v3/repos/octocat/mock-repo/git/refs/tags/v1.0")));
    }

    @Test
    void testListTags() {
        Map<String, Object> input = Map.of(
                "action", "getTagList",
                "accessToken", "mockToken",
                "org", "octocat",
                "repo", "mock-repo",
                "apiUrl", httpRule.baseUrl()
        );

        var result = new GitHubTask().execute(input, Map.of(), secretService);

        httpRule.verify(1, getRequestedFor(urlEqualTo("/api/v3/repos/octocat/mock-repo/tags?per_page=100&page=1")));

        var hook = assertInstanceOf(List.class, result.get("tagList"));
        assertEquals("v0.1", hook.get(0));
    }

    @Test
    void testListBranches() {
        Map<String, Object> input = Map.of(
                "action", "getBranchList",
                "accessToken", "mockToken",
                "org", "octocat",
                "repo", "mock-repo",
                "apiUrl", httpRule.baseUrl()
        );

        var result = new GitHubTask().execute(input, Map.of(), secretService);

        httpRule.verify(1, getRequestedFor(urlEqualTo("/api/v3/repos/octocat/mock-repo/branches?per_page=100&page=1")));

        var hook = assertInstanceOf(List.class, result.get("branchList"));
        assertEquals("main", hook.get(0));
    }

    @Test
    void testGetLatestSHA() {
        Map<String, Object> input = Map.of(
                "action", "getLatestSHA",
                "accessToken", "mockToken",
                "org", "octocat",
                "repo", "mock-repo",
                "branch", "main",
                "apiUrl", httpRule.baseUrl()
        );

        var result = new GitHubTask().execute(input, Map.of(), secretService);

        httpRule.verify(1, getRequestedFor(urlEqualTo("/api/v3/repos/octocat/mock-repo/branches?per_page=100&page=1")));

        var latestCommitSHA = assertInstanceOf(String.class, result.get("latestCommitSHA"));
        assertEquals("c5b97d5ae6c19d5c5df71a34c7fbeeda2479ccbc", latestCommitSHA);
    }

    @Test
    void testGetContent() {
        Map<String, Object> input = Map.of(
                "action", "getContent",
                "accessToken", "mockToken",
                "org", "octocat",
                "repo", "mock-repo",
                "ref", "main",
                "path", "README.md",
                "apiUrl", httpRule.baseUrl()
        );

        var result = new GitHubTask().execute(input, Map.of(), secretService);

        httpRule.verify(1, getRequestedFor(urlEqualTo("/api/v3/repos/octocat/mock-repo/contents/README.md?ref=main")));

        var contents = assertInstanceOf(List.class, result.get("contents"));
        var contentItem = assertInstanceOf(Map.class, contents.get(0));
        assertEquals("# mock-repo", contentItem.get("content"));
        assertEquals(17L, contentItem.get("size"));
    }

    @Test
    void testDeleteBranch() {
        Map<String, Object> input = Map.of(
                "action", "deleteBranch",
                "accessToken", "mockToken",
                "org", "octocat",
                "repo", "mock-repo",
                "branch", "mock-branch",
                "apiUrl", httpRule.baseUrl()
        );

        new GitHubTask().execute(input, Map.of(), secretService);

        httpRule.verify(1, deleteRequestedFor(urlEqualTo("/api/v3/repos/octocat/mock-repo/git/refs/heads/mock-branch")));
    }

    @Test
    void testMerge() {
        Map<String, Object> input = Map.of(
                "action", "merge",
                "accessToken", "mockToken",
                "org", "octocat",
                "repo", "mock-repo",
                "head", "feature",
                "base", "main",
                "commitMessage", "mock commit message",
                "apiUrl", httpRule.baseUrl()
        );

        new GitHubTask().execute(input, Map.of(), secretService);

        httpRule.verify(1, postRequestedFor(urlEqualTo("/api/v3/repos/octocat/mock-repo/merges")));
    }

    @Test
    void testFork() {
        Map<String, Object> input = Map.of(
                "action", "forkRepo",
                "accessToken", "mockToken",
                "org", "octocat",
                "repo", "mock-repo",
                "targetOrg", "new-org",
                "apiUrl", httpRule.baseUrl()
        );

        var result = new GitHubTask().execute(input, Map.of(), secretService);

        httpRule.verify(1, postRequestedFor(urlEqualTo("/api/v3/repos/octocat/mock-repo/forks?org=new-org")));

        assertTrue(result.isEmpty());
    }

    @Test
    void testDeleteRepo() {
        Map<String, Object> input = Map.of(
                "action", "deleteRepo",
                "accessToken", "mockToken",
                "org", "octocat",
                "repo", "repo-to-delete",
                "apiUrl", httpRule.baseUrl()
        );

        new GitHubTask().execute(input, Map.of(), secretService);

        httpRule.verify(1, getRequestedFor(urlEqualTo("/api/v3/repos/octocat/repo-to-delete")));
        httpRule.verify(1, deleteRequestedFor(urlEqualTo("/api/v3/repos/octocat/repo-to-delete")));
    }

    @Test
    void testError() {
        Map<String, Object> input = Map.of(
                "action", "deleteRepo",
                "accessToken", "mockToken",
                "org", "octocat",
                "repo", "error-repo",
                "apiUrl", httpRule.baseUrl()
        );

        Exception ex = assertThrows(Exception.class, () -> new GitHubTask().execute(input, Map.of()));

        assertTrue(ex.getCause().getMessage().contains("very unexpected error"));

        httpRule.verify(1, getRequestedFor(urlEqualTo("/api/v3/repos/octocat/error-repo")));
        httpRule.verify(0, deleteRequestedFor(urlEqualTo("/api/v3/repos/octocat/error-repo")));
    }
}
