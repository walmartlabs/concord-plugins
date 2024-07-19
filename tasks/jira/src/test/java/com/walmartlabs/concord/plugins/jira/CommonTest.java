package com.walmartlabs.concord.plugins.jira;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2024 Walmart Inc.
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

import com.walmartlabs.concord.runtime.v2.sdk.MapBackedVariables;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommonTest {

    @Mock
    JiraClient jiraClient;

    @Mock
    JiraSecretService jiraSecretService;

    @Spy
    JiraTaskCommon delegate = new JiraTaskCommon(jiraSecretService);

    Map<String, Object> input;
    Map<String, Object> defaults;

    @BeforeEach
    public void setup() {
        input = new HashMap<>();
        defaults = new HashMap<>();
        defaults.put("apiUrl", "https://localhost:1234/");
        defaults.put(
                "auth", Map.of(
                        "basic", Map.of(
                                "username", "user",
                                "password", "pass"
                            )
                )
        );

        when(jiraClient.jiraAuth(any())).thenReturn(jiraClient);
        when(jiraClient.url(anyString())).thenReturn(jiraClient);
        when(jiraClient.successCode(anyInt())).thenReturn(jiraClient);

        doAnswer(invocation -> jiraClient).when(delegate).getClient(any());
    }

    @Test
    void testCreateIssueWithBasicAuth() throws Exception {
        input.put("action", "createIssue");
        input.put("projectKey", "mock-proj-key");
        input.put("summary", "mock-summary");
        input.put("description", "mock-description");
        input.put("requestorUid", "mock-uid");
        input.put("issueType", "bug");

        when(jiraClient.post(anyMap())).thenReturn(Map.of("key", "\"result-key\""));

        var result = delegate.execute(TaskParams.of(new MapBackedVariables(input), defaults));
        assertNotNull(result);
        assertEquals("result-key", result.get("issueId"));

        verify(jiraSecretService, times(0))
                .exportCredentials("organization", "secret", null);

        verify(delegate, times(1)).createIssue(any());
    }

    @Test
    void testCreateIssueWithSecret() throws Exception {
        input.put("action", "createIssue");
        input.put("projectKey", "mock-proj-key");
        input.put("summary", "mock-summary");
        input.put("description", "mock-description");
        input.put("requestorUid", "mock-uid");
        input.put("issueType", "bug");
        input.put("auth", Map.of(
                "secret", Map.of(
                        "org", "organization",
                        "name", "secret"
                )
        ));

        when(jiraSecretService.exportCredentials(any(), any(), any())).thenReturn(new JiraCredentials("user", "pwd"));
        when(jiraClient.post(anyMap())).thenReturn(Map.of("key", "\"result-key\""));
        doAnswer(invocation -> jiraSecretService).when(delegate).getSecretService();

        var result = delegate.execute(TaskParams.of(new MapBackedVariables(input), defaults));
        assertNotNull(result);
        assertEquals("result-key", result.get("issueId"));

        verify(jiraSecretService, times(1))
                .exportCredentials("organization", "secret", null);
        verify(delegate, times(1)).createIssue(any());
        verify(delegate, times(1)).getSecretService();
    }

    @Test
    void testAddComment() throws IOException {
        input.put("action", "addComment");
        input.put("issueKey", "mock-issue");
        input.put("comment", "mock-comment");

        when(jiraClient.post(Mockito.anyMap())).thenReturn(null);

        var result = delegate.execute(TaskParams.of(new MapBackedVariables(input), defaults));

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(delegate, times(1)).addComment(any());
    }

    @Test
    void testAddAttachment() throws IOException {
        input.put("action", "addAttachment");
        input.put("issueKey", "issueId");
        input.put("userId", "userId");
        input.put("password", "password");
        input.put("filePath", "src/test/resources/sample.txt");

        doNothing().when(jiraClient).post(Mockito.any(File.class));

        var result = delegate.execute(TaskParams.of(new MapBackedVariables(input), defaults));

        assertTrue(result.isEmpty());

        verify(delegate, times(1)).addAttachment(any());
    }

    @Test
    void testCreateComponent() throws IOException {
        input.put("action", "createComponent");
        input.put("projectKey", "mock-project");
        input.put("componentName", "mock-component");

        when(jiraClient.post(Mockito.anyMap())).thenReturn(Map.of("id", "\"321\""));

        var result = delegate.execute(TaskParams.of(new MapBackedVariables(input), defaults));

        assertNotNull(result);
        assertEquals("321", result.get("componentId"));

        verify(delegate, times(1)).createComponent(any());
    }

    @Test
    void testDeleteComponent() throws IOException {
        input.put("action", "deleteComponent");
        input.put("componentId", 321);

        doNothing().when(jiraClient).delete();

        var result = delegate.execute(TaskParams.of(new MapBackedVariables(input), defaults));

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(delegate, times(1)).deleteComponent(any());
    }

    @Test
    void testTransition() throws IOException {
        input.put("action", "transition");
        input.put("issueKey", "issue-123");
        input.put("transitionId", 543);
        input.put("transitionComment", "mock-transition-comment");

        when(jiraClient.post(Mockito.anyMap())).thenReturn(null);

        var result = delegate.execute(TaskParams.of(new MapBackedVariables(input), defaults));

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(delegate, times(1)).transition(any());
    }

    @Test
    void testDeleteIssue() throws IOException {
        input.put("action", "deleteIssue");
        input.put("issueKey", "issue-123");

        doNothing().when(jiraClient).delete();

        var result = delegate.execute(TaskParams.of(new MapBackedVariables(input), defaults));

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(delegate, times(1)).deleteIssue(any());
    }

    @Test
    void testUpdateIssue() throws IOException {
        input.put("action", "updateIssue");
        input.put("issueKey", "issue-123");
        input.put("fields", Map.of("field1", "value1", "field2", "value2"));

        doNothing().when(jiraClient).put(anyMap());

        var result = delegate.execute(TaskParams.of(new MapBackedVariables(input), defaults));

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(delegate, times(1)).updateIssue(any());
    }

    @Test
    void testCreateSubTask() throws IOException {
        input.put("action", "createSubTask");
        input.put("parentIssueKey", "parent-issue-123");
        input.put("projectKey", "mock-proj-key");
        input.put("summary", "mock-summary");
        input.put("description", "mock-description");
        input.put("requestorUid", "mock-uid");
        input.put("issueType", "bug");

        when(jiraClient.post(anyMap())).thenReturn(Map.of("key", "\"result-key\""));

        var result = delegate.execute(TaskParams.of(new MapBackedVariables(input), defaults));

        assertNotNull(result);
        assertEquals("result-key", result.get("issueId"));

        verify(delegate, times(1)).createSubTask(any());
    }

    @Test
    void testCurrentStatus() throws IOException {
        input.put("action", "currentStatus");
        input.put("issueKey", "issueId");

        when(jiraClient.get()).thenReturn(Map.of(
                "fields", Map.of(
                        "status", Map.of(
                                "name", "Open"
                        )
                )
        ));

        var result = delegate.execute(TaskParams.of(new MapBackedVariables(input), defaults));

        var status = assertInstanceOf(String.class, result.get("issueStatus"));
        assertNotNull(status);
        assertEquals("Open", status);
    }

    @Test
    void testGetIssues() throws IOException {
        input.put("action", "getIssues");
        input.put("projectKey", "mock-proj-key");
        input.put("summary", "mock-summary");
        input.put("description", "mock-description");
        input.put("requestorUid", "mock-uid");
        input.put("issueType", "bug");

        when(jiraClient.post(anyMap())).thenReturn(Map.of(
                "issues", List.of(
                        Map.of(
                                "id", "123"
                        )
                )
        ));

        var result = delegate.execute(TaskParams.of(new MapBackedVariables(input), defaults));
        assertNotNull(result);
        assertEquals(1, result.get("issueCount"));

        var issues = assertInstanceOf(List.class, result.get("issueList"));
        assertEquals(1, issues.size());

        verify(delegate, times(1)).getIssues(any());
    }
}
