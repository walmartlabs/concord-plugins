package com.walmartlabs.concord.plugins.akeyless;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2022 Walmart Inc., Concord Authors
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


import com.walmartlabs.concord.plugins.akeyless.model.Secret;
import com.walmartlabs.concord.plugins.akeyless.model.SecretCache;
import com.walmartlabs.concord.plugins.akeyless.model.SecretCacheImpl;
import com.walmartlabs.concord.runtime.v2.sdk.SecretService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

class CommonTest {

    private static final String DEFAULT_SALT = "test-salt";
    private SecretCache secretCache;

    @BeforeEach
    void setup() {
        secretCache = SecretCacheImpl.getInstance(DEFAULT_SALT, false);
    }

    @Test
    void testSecretCache() throws Exception {
        final String expectedValue = "the-value";
        final SecretService secretService = Mockito.mock(SecretService.class);

        when(secretService.exportAsString("myOrg", "mySecret", null))
                .thenReturn(expectedValue);

        // --- value from cache should be the same as a fresh call

        assertEquals(expectedValue, callCache(secretCache, secretService, "myOrg", "mySecret", null).getValue());
        assertEquals(expectedValue, callCache(secretCache, secretService, "myOrg", "mySecret", null).getValue());

        // --- Cache should only pull secret data once

        Mockito.verify(secretService, times(1))
                .exportAsString("myOrg", "mySecret", null);
    }

    @Test
    void testDirtyCache() throws Exception {
        final String expectedValue = "the-value";
        final SecretService secretService = Mockito.mock(SecretService.class);

        when(secretService.exportAsString("myOrg", "mySecret", null))
                .thenReturn(expectedValue);

        // first call misses cache
        assertEquals(expectedValue, callCache(secretCache, secretService, "myOrg", "mySecret", null).getValue());

        // changing the salt resets the cache
        SecretCache cache2 = SecretCacheImpl.getInstance("new-salt", false);
        assertEquals(expectedValue, callCache(cache2, secretService, "myOrg", "mySecret", null).getValue());
        assertEquals(expectedValue, callCache(cache2, secretService, "myOrg", "mySecret", null).getValue());
        assertEquals(expectedValue, callCache(cache2, secretService, "myOrg", "mySecret", null).getValue());

        // ---

        Mockito.verify(secretService, times(2))
                .exportAsString("myOrg", "mySecret", null);
    }

    private static Secret.StringSecret callCache(SecretCache cache, SecretService secretService, String org, String name, String password) {
        return (Secret.StringSecret) cache.get(org, name, () -> {
            try {
                String value = secretService.exportAsString(org, name, password);
                return new Secret.StringSecret(value);
            } catch (Exception e) {
                fail();
                return null;
            }
        });
    }
}
