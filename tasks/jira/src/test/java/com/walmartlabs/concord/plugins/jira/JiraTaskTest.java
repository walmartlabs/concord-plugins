package com.walmartlabs.concord.plugins.jira;

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

import com.walmartlabs.concord.sdk.Context;
import com.walmartlabs.concord.sdk.MockContext;
import com.walmartlabs.concord.sdk.SecretService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.nullable;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JiraTaskTest {

    @TempDir
    Path workDir;

    @Mock
    private SecretService secretService;

    @Mock
    private JiraTaskCommon common;

    @Spy
    private JiraTask task = new JiraTask(secretService);

    private Context mockContext;

    @BeforeEach
    public void setup() {
        mockContext = new MockContext(new HashMap<>());
        mockContext.setVariable("txId", UUID.randomUUID());
        mockContext.setVariable("workDir", workDir.toString());

    }

    @Test
    void testExecute() {
        mockContext.setVariable("action", "deleteIssue");

        when(task.delegate(any())).thenReturn(common);
        when(common.execute(any(TaskParams.DeleteIssueParams.class))).thenReturn(Map.of("ok", true));

        assertDoesNotThrow(() -> task.execute(mockContext));

        var ok = assertInstanceOf(Boolean.class, mockContext.getVariable("ok"));
        assertTrue(ok);
    }

    @Test
    void testGetStatus() {
        when(task.delegate(any())).thenReturn(common);
        when(common.execute(any(TaskParams.CurrentStatusParams.class))).thenReturn(Map.of("issueStatus", "Open"));

        var status = assertDoesNotThrow(() -> task.getStatus(mockContext, "issue-123"));

        assertEquals("Open", status);
    }

    @Test
    void testGetSecretService() throws Exception {
        when(secretService.exportCredentials(any(Context.class), anyString(), anyString(), anyString(), anyString(), nullable(String.class)))
                .thenReturn(Map.of("username", "foo", "password", "bar"));

        var v1SecretService = new JiraTask.V1SecretService(secretService, mockContext);

        var creds = v1SecretService.exportCredentials("org", "name", null);

        assertEquals("foo", creds.username());
        assertEquals("bar", creds.password());

        verify(secretService, times(1)).exportCredentials(any(Context.class), anyString(), anyString(), anyString(), anyString(), nullable(String.class));
    }

}
