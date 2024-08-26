package com.walmartlabs.concord.plugins.git.v2;

import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.walmartlabs.concord.plugins.git.GitHubTask;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommitTest {

    @RegisterExtension
    protected static WireMockExtension httpRule = WireMockExtension.newInstance()
            .options(wireMockConfig()
                    .dynamicPort()
                    .usingFilesUnderClasspath("wiremock/commit")
                    .notifier(new ConsoleNotifier(false))) // set to true for verbose logging
            .build();

    @Test
    void testGet() {
        Map<String, Object> input = Map.of(
                "action", "getCommit",
                "accessToken", "mockToken",
                "org", "octocat",
                "repo", "mock-repo",
                "commitSHA", "6dcb09b5b57875f334f61aebed695e2e4193db5e",
                "apiUrl", httpRule.baseUrl()
        );

        var result = new GitHubTask().execute(input, Map.of());

        httpRule.verify(1, getRequestedFor(urlEqualTo("/api/v3/repos/octocat/mock-repo/commits/6dcb09b5b57875f334f61aebed695e2e4193db5e")));
        httpRule.verify(1, getRequestedFor(urlEqualTo("/api/v3/repos/octocat/mock-repo")));

        var r = assertInstanceOf(Map.class, result.get("result"));
        var data = assertInstanceOf(Map.class, r.get("data"));
        var defaultBranch = assertInstanceOf(String.class, data.get("defaultBranch"));

        assertEquals(true, r.get("ok"));
        assertEquals("master", defaultBranch);
    }

    @Test
    void testAddStatus() {
        Map<String, Object> input = Map.of(
                "action", "addStatus",
                "accessToken", "mockToken",
                "org", "octocat",
                "repo", "mock-repo",
                "commitSHA", "6dcb09b5b57875f334f61aebed695e2e4193db5e",
                "context", "myContext",
                "state", "pending",
                "targetUrl", "https://concord.example.com/#/process/${txId}",
                "apiUrl", httpRule.baseUrl()
        );

        var result = new GitHubTask().execute(input, Map.of());

        httpRule.verify(1, postRequestedFor(urlEqualTo("/api/v3/repos/octocat/mock-repo/statuses/6dcb09b5b57875f334f61aebed695e2e4193db5e")));

        assertTrue(result.isEmpty());
    }

    @Test
    void testGetStatuses() {
        Map<String, Object> input = Map.of(
                "action", "getStatuses",
                "accessToken", "mockToken",
                "org", "octocat",
                "repo", "mock-repo",
                "commitSHA", "6dcb09b5b57875f334f61aebed695e2e4193db5e",
                "apiUrl", httpRule.baseUrl()
        );

        var result = new GitHubTask().execute(input, Map.of());

        httpRule.verify(1, getRequestedFor(urlEqualTo("/api/v3/repos/octocat/mock-repo/statuses/6dcb09b5b57875f334f61aebed695e2e4193db5e?per_page=100&page=1")));

        var commitStatuses = assertInstanceOf(List.class, result.get("commitStatuses"));

        assertFalse(commitStatuses.isEmpty());
        var status = assertInstanceOf(Map.class, commitStatuses.get(0));
        assertEquals("success", status.get("state"));
    }

}
