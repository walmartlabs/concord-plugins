package com.walmartlabs.concord.plugins.git;

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

import org.eclipse.egit.github.core.client.GitHubClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class GitHubCommonTest {

    @Test
    void testCreateClient() {
        GitHubClient client = GitHubTask.createClient("https://mock.github.local");

        assertNotNull(client);
    }

    @Test
    void testCreateClientWithPort() {
        GitHubClient client = GitHubTask.createClient("https://mock.github.local:8080");

        assertNotNull(client);
    }

}
