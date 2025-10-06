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
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import com.walmartlabs.concord.runtime.v2.sdk.UserDefinedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.Objects;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "GH_TEST_TOKEN", matches = ".+")
public class ShortCommitShaActionTest {

    @Test
    public void testOkAction() {
        var apiInfo = GitHubApiInfo.builder()
                .baseUrl("https://github.com")
                .accessTokenProvider(() -> Objects.requireNonNull(System.getenv("GH_TEST_TOKEN")))
                .build();

        var input = new GitHubTaskParams.GetShortCommitSha("walmartlabs", "concord-plugins", "db89df46a95deb3a35cf31b76274258391ebc61d", 7);

        var action = new ShortCommitShaAction();
        var result = action.execute(UUID.randomUUID(), apiInfo, input);
        assertNotNull(result);
        assertEquals("db89df4", result.get("shortSha"));
    }

    @Test
    public void testFailAction() {
        var apiInfo = GitHubApiInfo.builder()
                .baseUrl("https://github.com")
                .accessTokenProvider(() -> Objects.requireNonNull(System.getenv("GH_TEST_TOKEN")))
                .build();

        var input = new GitHubTaskParams.GetShortCommitSha("walmartlabs", "concord-plugins", "0123456789abcdef0123456789abcdef01234567", 7);

        var action = new ShortCommitShaAction();
        var ex = assertThrows(UserDefinedException.class, () -> action.execute(UUID.randomUUID(), apiInfo, input));
        assertTrue(ex.getMessage().contains("not found in"));
    }
}
