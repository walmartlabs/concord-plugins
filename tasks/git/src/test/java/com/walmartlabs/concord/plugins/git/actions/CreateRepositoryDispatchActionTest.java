package com.walmartlabs.concord.plugins.git.actions;

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

import com.walmartlabs.concord.plugins.git.GitHubTaskParams;
import com.walmartlabs.concord.plugins.git.model.GitHubApiInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfEnvironmentVariable(named = "GH_TEST_TOKEN", matches = ".+")
public class CreateRepositoryDispatchActionTest {

    @Test
    public void testOkAction() {
        var apiInfo = GitHubApiInfo.builder()
                .baseUrl("https://github.com")
                .accessTokenProvider(() -> Objects.requireNonNull(System.getenv("GH_TEST_TOKEN")))
                .build();

        var input = new GitHubTaskParams.CreateRepositoryDispatch("walmartlabs", "concord-plugins",
                "test-event", Map.of("key", "value"));

        var action = new CreateRepositoryDispatchAction();
        var result = action.execute(UUID.randomUUID(), apiInfo, false, input);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testDryRun() {
        var apiInfo = GitHubApiInfo.builder()
                .baseUrl("https://github.com")
                .accessTokenProvider(() -> "fake-token")
                .build();

        var input = new GitHubTaskParams.CreateRepositoryDispatch("walmartlabs", "concord-plugins",
                "test-event", Map.of("key", "value"));

        var action = new CreateRepositoryDispatchAction();
        var result = action.execute(UUID.randomUUID(), apiInfo, true, input);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
