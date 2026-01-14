package com.walmartlabs.concord.plugins.confluence;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2026 Walmart Inc., Concord Authors
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.walmartlabs.concord.plugins.confluence.model.auth.AuthUtils;
import com.walmartlabs.concord.plugins.confluence.model.auth.BasicAuth;
import com.walmartlabs.concord.runtime.v2.sdk.MapBackedVariables;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TaskParamsTest {

    private static final String MOCK_API_URL = "https://confluence.local";
    private static final String MOCK_USER = "mock-user";
    private static final String MOCK_PASSWORD = "mock-password";
    private static final String MOCK_TOKEN = "mock-token";

    @Test
    void testDeprecatedAuth() {
        var params = params(Map.of(
                "userId", MOCK_USER,
                "password", MOCK_PASSWORD
        ));
        var auth = AuthUtils.parseAuth(params, new ObjectMapper());
        var basic = assertInstanceOf(BasicAuth.class, auth.basic());

        assertNotNull(basic);
        assertEquals(MOCK_USER, basic.username());
        assertEquals(MOCK_USER, basic.username());
    }

    @Test
    void testAuthToken() {
        var params = params(Map.of(
                "auth", Map.of("accessToken", MOCK_TOKEN)
        ));
        var auth = AuthUtils.parseAuth(params, new ObjectMapper());
        var token = auth.accessToken();

        assertNotNull(token);
        assertEquals(MOCK_TOKEN, token);
    }

    @Test
    void testBasicAuth() {
        var params = params(Map.of(
                "auth", Map.of("basic", Map.of(
                        "username", MOCK_USER,
                        "password", MOCK_PASSWORD
                ))
        ));
        var auth = AuthUtils.parseAuth(params, new ObjectMapper());
        var basic = assertInstanceOf(BasicAuth.class, auth.basic());

        assertNotNull(basic);
        assertEquals(MOCK_USER, basic.username());
        assertEquals(MOCK_PASSWORD, basic.password());
    }

    @Test
    void testGeneral() {
        var params = params(Map.of(
                "action", "createPage",
                "auth", Map.of("basic", Map.of(
                        "username", MOCK_USER,
                        "password", MOCK_PASSWORD
                ))
        ));

        assertNotNull(params);
        assertEquals(MOCK_API_URL, params.apiUrl());
        assertEquals(TaskParams.Action.CREATEPAGE, params.action());
    }

    private static TaskParams params(Map<String, Object> extra) {
        Map<String, Object> input = new HashMap<>();
        input.put("apiUrl", MOCK_API_URL);
        input.put("action", "getPageContent");

        input.putAll(extra);

        return TaskParams.of(new MapBackedVariables(input), Map.of());
    }
}
