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

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class IssueTest {

    @RegisterExtension
    protected static WireMockExtension httpRule = WireMockExtension.newInstance()
            .options(wireMockConfig()
                    .dynamicPort()
                    .usingFilesUnderClasspath("wiremock/issue")
                    .notifier(new ConsoleNotifier(false))) // set to true for verbose logging
            .build();

    @Test
    void testCreate() {
        Map<String, Object> input = Map.of(
                "action", "createIssue",
                "accessToken", "mockToken",
                "org", "octocat",
                "repo", "mock-repo",
                "title", "Feature A",
                "body", "Feature A implements the requirements from request 12.",
                "assignee", "feature-a",
                "labels", List.of("Do not merge"),
                "apiUrl", httpRule.baseUrl()
        );

        var result = new GitHubTask().execute(input, Map.of());

        httpRule.verify(1, postRequestedFor(urlEqualTo("/api/v3/repos/octocat/mock-repo/issues")));

        var issue = assertInstanceOf(Map.class, result.get("issue"));
        assertEquals(1347, issue.get("number"));
    }

}
