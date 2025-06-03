package com.walmartlabs.concord.plugins.hashivault;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2021 Walmart Inc., Concord Authors
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

import static org.junit.jupiter.api.Assertions.*;

class UnitTest {

    private static final TaskParams.SecretExporter exporter =
            (o, n, p) -> "a-secret";

    @Test
    void defaultActionTest() {
        var vars = Map.<String, Object>of("baseUrl", "http://example.com:8200");

        var params = TaskParams.of(new MapBackedVariables(vars), null, exporter);
        assertEquals(TaskParams.Action.READKV, params.action());
    }

    @Test
    void invalidActionTest() {
        var vars = new MapBackedVariables(Map.of("action", "not-an-action"));

        assertThrows(IllegalArgumentException.class, () -> TaskParams.of(vars, null, exporter));
    }

    @Test
    void requiredParametersTest() {
        var params = TaskParams.of(new MapBackedVariables(Map.of()), null, exporter);

        var e1 = assertThrows(IllegalArgumentException.class, params::baseUrl);
        assertTrue(e1.getMessage().contains("'baseUrl' is required"));

        var e2 = assertThrows(IllegalArgumentException.class, params::apiToken);
        assertTrue(e2.getMessage().contains("'apiToken' is required"));

        var e3 = assertThrows(IllegalArgumentException.class, params::path);
        assertTrue(e3.getMessage().contains("'path' is required"));

        assertDoesNotThrow(params::kvPairs); // default action

        // kvPairs required when action is writeKv
        params = TaskParams.of(
                new MapBackedVariables(Map.of("action", "writeKv")), null, exporter);
        var e4 = assertThrows(IllegalArgumentException.class, params::kvPairs);
        assertTrue(e4.getMessage().contains("'kvPairs' is required"));

        // cubbyhole is a v1 engine
        params = TaskParams.of(new MapBackedVariables(
                Map.of("path", "cubbyhole/mysecret", "engineVersion", 2)), null, exporter);
        assertEquals(1, params.engineVersion());
    }

    /**
     * The data returned by the task may be either a Map containing all of the
     * key/value pairs in the Vault secret OR a single String value from the
     * Vault secret when a specific secret "key" is specified.
     */
    @Test
    void mapDataTest(){
        var vars = new MapBackedVariables(Map.of("path", "secret/mysecret"));
        var params = TaskParams.of(vars, null, exporter);

        var result = HashiVaultTaskResult
                .of(true, Map.of("top_secret", "value"), null, params);

        var m = assertInstanceOf(Map.class, result.data(),
                "data should be Map when key param is not given");
        assertEquals(1, m.size());
        assertEquals("value", m.get("top_secret"));
    }

    @Test
    void stringDataTest() {
        var vars = new MapBackedVariables(
                Map.of("path", "secret/mysecret", "key", "top_secret"));
        var params = TaskParams.of(vars, null, exporter);

        var result = HashiVaultTaskResult
                .of(true, Map.of("top_secret", "value"), null, params);
        var s = assertInstanceOf(String.class, result.data(),
                "data should be String when key param is given");
        assertEquals("value", s);
    }

}
