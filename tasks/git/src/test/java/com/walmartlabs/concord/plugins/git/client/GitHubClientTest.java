package com.walmartlabs.concord.plugins.git.client;

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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class GitHubClientTest {

    @Test
    void nextAndLast_returnsNextUrl() {
        var h = "<https://api.github.com/repos/o/r/issues?per_page=100&page=2>; rel=\"next\", " +
                "<https://api.github.com/repos/o/r/issues?per_page=100&page=34>; rel=\"last\"";
        var next = GitHubClient.parseNextLink(h);
        assertEquals("https://api.github.com/repos/o/r/issues?per_page=100&page=2", next);
    }

    @Test
    void onlyLast_returnsNull() {
        var h = "<https://api.github.com/repos/o/r/issues?per_page=100&page=34>; rel=\"last\"";
        assertNull(GitHubClient.parseNextLink(h));
    }

    @Test
    void emptyOrNull_returnsNull() {
        assertNull(GitHubClient.parseNextLink(null));
        assertNull(GitHubClient.parseNextLink(""));
        assertNull(GitHubClient.parseNextLink("   "));
    }

    @Test
    void mixedOrder_spaces_tabs_stillParses() {
        var h = " <https://ghe.example.com/api/v3/x?page=3>\t; rel=\"prev\", " +
                "<https://ghe.example.com/api/v3/x?page=5>;   rel=\"next\" , " +
                "<https://ghe.example.com/api/v3/x?page=9>; rel=\"last\"";
        var next = GitHubClient.parseNextLink(h);
        assertEquals("https://ghe.example.com/api/v3/x?page=5", next);
    }

    @Test
    void multipleLinks_noNext_returnsNull() {
        var h = "<https://api.github.com/x?page=1>; rel=\"first\", " +
                "<https://api.github.com/x?page=3>; rel=\"prev\", " +
                "<https://api.github.com/x?page=9>; rel=\"last\"";
        assertNull(GitHubClient.parseNextLink(h));
    }
}
