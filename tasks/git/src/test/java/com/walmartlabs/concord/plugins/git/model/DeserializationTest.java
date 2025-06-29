package com.walmartlabs.concord.plugins.git.model;

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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static com.walmartlabs.concord.plugins.git.Utils.getObjectMapper;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DeserializationTest {

    @Test
    void testAppInstallation() {
        var installation = readValue("""
                {
                  "id": 123456,
                  "client_id": "abc123",
                  "account": {
                    "login": "mock-login",
                    "id": 78910
                  },
                  "app_id": 111213,
                  "target_id": 4321,
                  "target_type": "Organization",
                  "permissions": {
                    "contents": "read",
                    "metadata": "read"
                  },
                  "events": ["push", "pull_request"],
                  "created_at": "2025-06-19T18:15:44.000Z",
                  "updated_at": "2025-06-20T13:46:03.000Z",
                  "access_tokens_url": "https://example.local/app/installations/1234/access_tokens"
                }
                """, AppInstallation.class);
        assertEquals("https://example.local/app/installations/1234/access_tokens", installation.accessTokensUrl());
    }

    @Test
     void testAppInstallationAccessToken() {
        var token = readValue("""
                {
                  "token": "mock-token",
                  "expires_at": "2025-06-20T15:00:43Z",
                  "permissions": {
                    "contents": "read",
                    "metadata": "read"
                  },
                  "repository_selection": "all",
                  "extra_field": null
                }
                """, AppInstallationAccessToken.class);
        assertEquals("mock-token", token.token());
        assertEquals(OffsetDateTime.parse("2025-06-20T15:00:43Z"), token.expiresAt());
    }

    private <T> T readValue(String json, Class<T> valueType) {
        return Assertions.assertDoesNotThrow(() -> getObjectMapper().readValue(json, valueType));
    }
}
