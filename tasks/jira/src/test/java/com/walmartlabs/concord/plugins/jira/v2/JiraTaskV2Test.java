package com.walmartlabs.concord.plugins.jira.v2;

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

import com.walmartlabs.concord.plugins.jira.JiraTaskCommon;
import com.walmartlabs.concord.plugins.jira.TaskParams;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.MapBackedVariables;
import com.walmartlabs.concord.runtime.v2.sdk.SecretService;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import com.walmartlabs.concord.runtime.v2.sdk.Variables;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JiraTaskV2Test {

    @Mock
    private Context context;

    @Mock
    SecretService secretService;

    @Mock
    private JiraTaskCommon common;

    private JiraTaskV2 task;
    private Map<String, Object> input;
    private Variables defaultVariables;

    @BeforeEach
    public void setup() {
        task = spy(new JiraTaskV2(context));
        input = new HashMap<>();
        defaultVariables = new MapBackedVariables(Map.of());
    }

    @Test
    void testExecute() {
        input.put("action", "deleteIssue");
        when(task.getDelegate()).thenReturn(common);
        when(context.defaultVariables()).thenReturn(defaultVariables);
        when(common.execute(any(TaskParams.DeleteIssueParams.class))).thenReturn(Map.of("customResult", "customResultValue"));

        var result = assertDoesNotThrow(() -> task.execute(new MapBackedVariables(input)));
        var simpleResult = assertInstanceOf(TaskResult.SimpleResult.class, result);

        assertTrue(simpleResult.ok());
        assertEquals("customResultValue", simpleResult.values().get("customResult"));
    }

    @Test
    void testSecretService() throws Exception {
        when(secretService.exportCredentials(anyString(), anyString(), nullable(String.class)))
                .thenReturn(SecretService.UsernamePassword.of("foo", "bar"));

        var v2SecretService = new JiraTaskV2.V2SecretService(secretService);

        var creds = v2SecretService.exportCredentials("org", "name", null);

        assertEquals("foo", creds.username());
        assertEquals("bar", creds.password());

        verify(secretService, times(1)).exportCredentials(anyString(), anyString(), nullable(String.class));
    }
}
