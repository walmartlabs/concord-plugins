package com.walmartlabs.concord.plugins.jira;

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

import com.walmartlabs.concord.runtime.v2.sdk.MapBackedVariables;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class CommonClientLoaderTest {

    @Mock
    JiraSecretService jiraSecretService;

    @Spy
    JiraTaskCommon delegate = new JiraTaskCommon(jiraSecretService);

    @Test
    void testLoadClient() {
        Map<String, Object> input = new HashMap<>();
        input.put("action", "getIssues");

        var client = delegate.getClient(TaskParams.of(new MapBackedVariables(input), Map.of()));

        assertInstanceOf(NativeJiraHttpClient.class, client);
    }

    @Test
    void testLoadClientFallback() {
        Map<String, Object> input = new HashMap<>();
        input.put("action", "getIssues");

        doThrow(new NoClassDefFoundError()).when(delegate).getNativeClient(any());

        var client = delegate.getClient(TaskParams.of(new MapBackedVariables(input), Map.of()));
        assertInstanceOf(JiraClient.class, client);
    }

    @Test
    void testNoClient() {
        Map<String, Object> input = new HashMap<>();
        input.put("action", "getIssues");

        doThrow(new NoClassDefFoundError()).when(delegate).getNativeClient(any());
        doThrow(new NoClassDefFoundError()).when(delegate).getOkHttpClient(any());

        var params = TaskParams.of(new MapBackedVariables(input), Map.of());

        var expected = assertThrows(IllegalStateException.class, () -> delegate.getClient(params));
        assertTrue(expected.getMessage().contains("No jira http client found"));
    }
}
