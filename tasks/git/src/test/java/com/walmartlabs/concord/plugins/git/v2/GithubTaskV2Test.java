package com.walmartlabs.concord.plugins.git.v2;

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


import com.walmartlabs.concord.plugins.git.GitHubTask;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.MapBackedVariables;
import com.walmartlabs.concord.runtime.v2.sdk.SecretService;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GithubTaskV2Test {

    @Mock
    GitHubTask delegate;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    Context ctx;

    @Mock
    SecretService secretService;

    GithubTaskV2 task;

    @BeforeEach
    void setUp() {
        when(ctx.defaultVariables().toMap()).thenReturn(defaults());
        task = Mockito.spy(new GithubTaskV2(ctx, secretService));

        doAnswer(invocation -> delegate)
                .when(task).getDelegate();
    }

    @Test
    void testExecute() {
        when(delegate.execute(any(), any(), any()))
                .thenReturn(Map.of("data", "value"));

        var result = assertInstanceOf(TaskResult.SimpleResult.class, task.execute(new MapBackedVariables(Map.of())));

        assertEquals("value", result.values().get("data"));
        verify(delegate, times(1))
                .execute(any(), any(), any());
    }

    @Test
    void testCreateAppAccessToken() {
        when(delegate.createAppToken(any(), any(), any()))
                .thenReturn("mock-token");

        var token = task.createAppAccessToken(Map.of());

        assertEquals("mock-token", token);
        verify(delegate, times(1))
                .createAppToken(any(), any(), any());
    }

    static Map<String, Object> defaults() {
        return Map.of(
                "apiUrl", "https://host.local",
                "org", "mock-org",
                "repo", "mock-repo"
        );
    }
}
