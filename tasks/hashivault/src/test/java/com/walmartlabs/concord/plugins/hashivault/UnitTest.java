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
import com.walmartlabs.concord.runtime.v2.sdk.Variables;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class UnitTest {

    private static final TaskParams.SecretExporter exporter =
            (o, n, p) -> "a-secret";

    @BeforeEach
    public void setup() {
    }

    @Test
    public void defaultActionTest() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("baseUrl", "http://example.com:8200");

        TaskParams params = TaskParams.of(new MapBackedVariables(vars), null, exporter);
        assertEquals(TaskParams.Action.READKV, params.action());
    }

    @Test
    public void invalidActionTest() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("action", "not-an-action");

        Assertions.assertThrows(IllegalArgumentException.class, () -> TaskParams.of(new MapBackedVariables(vars), null, exporter));
    }

    @Test
    public void requiredParametersTest() {
        TaskParams params = TaskParams.of(new MapBackedVariables(getMap()), null, exporter);
        try {
            params.baseUrl();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("'baseUrl' is required"));
        }

        try {
            params.apiToken();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("'apiToken' is required"));
        }

        try {
            params.path();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("'path' is required"));
        }

        params.kvPairs(); // default action

        // kvPairs required when action is writeKv
        params = TaskParams.of(
                new MapBackedVariables(getMap("action", "writeKv")), null, exporter);
        try {
            params.kvPairs();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("'kvPairs' is required"));
        }

        // cubbyhole is a v1 engine
        params = TaskParams.of(new MapBackedVariables(
                getMap("path", "cubbyhole/mysecret", "engineVersion", 2)), null, exporter);
        assertEquals(1, params.engineVersion());
    }

    /**
     * The data returned by the task may be either a Map containing all of the
     * key/value pairs in the Vault secret OR a single String value from the
     * Vault secret when a specific secret "key" is specified.
     */
    @Test
    public void mapDataTest(){
        Variables vars = new MapBackedVariables(getMap("path", "secret/mysecret"));
        TaskParams params = TaskParams.of(vars, null, exporter);

        HashiVaultTaskResult result = HashiVaultTaskResult
                .of(true, getMap("top_secret", "value"), null, params);

        try {
            String s = result.data();
            fail("data should be Map when key param is not given");
        } catch (ClassCastException e) {
            // that's expected
        }

        // this should work
        Map<String, Object> m = result.data();
        assertEquals(1, m.size());
    }

    @Test
    public void stringDataTest() {
        Variables vars = new MapBackedVariables(
                getMap("path", "secret/mysecret", "key", "top_secret"));
        TaskParams params = TaskParams.of(vars, null, exporter);

        HashiVaultTaskResult result = HashiVaultTaskResult
                .of(true, getMap("top_secret", "value"), null, params);
        try {
            Map<String, Object> m = result.data();
            fail("data should be String when key param is given");
        } catch (ClassCastException e) {
            // that's expected
        }

        // this should work
        String s = result.data();
        assertEquals("value", s);
    }

    /**
     * <p>Creates a Map from an arbitrary list of keys and values.</p>
     * <pre>
     * Map&lt;String, Object&gt; m = getMap("key1", "value1", "key2", "value2");
     * </pre>
     * @param params An even number of alternating keys and values
     * @return Map of the given keys and values
     */
    @SuppressWarnings("unchecked")
    private static <V> Map<String, V> getMap(Object... params) {
        if (params.length % 2 != 0) {
            throw new RuntimeException("Must have even number of parameters");
        }

        if (params.length == 0) {
            return Collections.emptyMap();
        }

        Map<String, V> m = new HashMap<>(params.length / 2);
        for (int i=0; i<params.length; i+=2) {
            m.put(params[i].toString(), (V) params[i+1]);
        }

        return m;
    }
}
