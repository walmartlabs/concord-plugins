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
import com.walmartlabs.concord.sdk.MapUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@EnabledIfEnvironmentVariable(named = "GH_TEST_TOKEN", matches = ".+")
public class GetRefActionTest {

    @Test
    public void testOkAction() {
        var apiInfo = GitHubApiInfo.builder()
                .baseUrl("https://github.com")
                .accessTokenProvider(() -> Objects.requireNonNull(System.getenv("GH_TEST_TOKEN")))
                .build();

        var input = new GitHubTaskParams.GetRef("walmartlabs", "concord-plugins", "tags/2.11.0", false);

        var action = new GetRefAction();
        var result = action.execute(UUID.randomUUID(), apiInfo, false, input);
        assertNotNull(result);
        assertEquals("refs/tags/2.11.0", result.get("ref"));
        assertEquals("ab7e34aff888af24a1223839b3afc5588b061db3", MapUtils.getMap(result, "object", Map.of()).get("sha"));
    }
}
