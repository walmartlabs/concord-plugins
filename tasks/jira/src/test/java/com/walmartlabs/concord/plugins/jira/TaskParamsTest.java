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

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskParamsTest {

    @Test
    void testHttpVersionDefault() {
        Map<String, Object> input = Map.of();
        var params = new TaskParams.DeleteIssueParams(new MapBackedVariables(input));

        assertEquals(JiraClientCfg.HttpVersion.DEFAULT, params.httpProtocolVersion());
    }

    @Test
    void testHttpVersion1() {
        Map<String, Object> input = Map.of("httpClientProtocolVersion", "http/1.1");
        var params = new TaskParams.DeleteIssueParams(new MapBackedVariables(input));

        assertEquals(JiraClientCfg.HttpVersion.HTTP_1_1, params.httpProtocolVersion());
    }

    @Test
    void testHttpVersion2() {
        Map<String, Object> input = Map.of("httpClientProtocolVersion", "http/2.0");
        var params = new TaskParams.DeleteIssueParams(new MapBackedVariables(input));

        assertEquals(JiraClientCfg.HttpVersion.HTTP_2, params.httpProtocolVersion());
    }

    @Test
    void testHttpVersionBlank() {
        Map<String, Object> input = Map.of("httpClientProtocolVersion", " ");
        var params = new TaskParams.DeleteIssueParams(new MapBackedVariables(input));

        assertEquals(JiraClientCfg.HttpVersion.DEFAULT, params.httpProtocolVersion());
    }

    @Test
    void testHttpVersionUnknown() {
        Map<String, Object> input = Map.of("httpClientProtocolVersion", "invalidValue");
        var params = new TaskParams.DeleteIssueParams(new MapBackedVariables(input));

        var expected = assertThrows(IllegalArgumentException.class, params::httpProtocolVersion);
        assertTrue(expected.getMessage().contains("Unsupported HTTP version"));
    }

    @Test
    void testUserAgent() {
        Map<String, Object> input = Map.of("txId", "ec82226b-a012-43d6-8571-f4a5849b2189");
        var params = new TaskParams.DeleteIssueParams(new MapBackedVariables(input));

        assertEquals("Concord-Jira-Plugin: ec82226b-a012-43d6-8571-f4a5849b2189", params.userAgent());
    }

    @Test
    void testCustomUserAgent() {
        Map<String, Object> input = Map.of("userAgent", "custom-agent");
        var params = new TaskParams.DeleteIssueParams(new MapBackedVariables(input));

        assertEquals("custom-agent", params.userAgent());
    }
}
