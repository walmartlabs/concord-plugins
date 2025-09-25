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

import com.walmartlabs.concord.plugins.git.model.GitHubApiInfo;
import com.walmartlabs.concord.plugins.git.tokens.AccessTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppAccessTokenTest {

    @Mock
    GitHubApiInfo apiInfo;

    @Mock
    GitSecretService secretService;

    @Mock
    AccessTokenProvider accessTokenProvider;

    @Spy
    GitHubTask delegate = new GitHubTask(UUID.randomUUID());

    @Test
    void testPublicMethod() {
        doAnswer(invocation -> apiInfo)
                .when(delegate).createApiInfo(any(), any(), any());

        when(apiInfo.accessTokenProvider()).thenReturn(accessTokenProvider);
        when(accessTokenProvider.getToken()).thenReturn("mock-token");

        var token = delegate.createAppToken(Map.of(), Map.of(), secretService);

        assertEquals("mock-token", token);
        verify(delegate, times(1)).createApiInfo(any(), any(), any());
        verify(accessTokenProvider, times(1)).getToken();
    }

    @Test
    void testExecute() {
        doAnswer(invocation -> apiInfo)
                .when(delegate).createApiInfo(any(), any(), any());

        when(apiInfo.accessTokenProvider()).thenReturn(accessTokenProvider);
        when(apiInfo.baseUrl()).thenReturn("https://host.local");
        when(accessTokenProvider.getToken()).thenReturn("mock-token");

        var result = delegate.execute(Map.of("action", "createAppToken"), Map.of(), secretService);

        assertEquals("mock-token", result.get("token"));
        verify(delegate, times(1)).createApiInfo(any(), any(), any());
        verify(accessTokenProvider, times(1)).getToken();
    }
}
