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
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.SecretService;
import com.walmartlabs.concord.plugins.hashivault.v2.HashiVaultTask;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UnitTest {

    private static final TaskParams.SecretExporter exporter =
            (o, n, p) -> "a-secret";

    @Test
    void defaultActionTest() {
        var vars = Map.<String, Object>of("baseUrl", "http://example.com:8200");

        var params = TaskParams.of(vars, null, exporter);
        assertEquals(TaskParams.Action.READKV, params.action());
    }

    @Test
    void invalidActionTest() {
        var vars = Map.<String, Object>of("action", "not-an-action");

        assertThrows(IllegalArgumentException.class, () -> TaskParams.of(vars, null, exporter));
    }

    @Test
    void requiredParametersTest() {
        var params = TaskParams.of(Map.of(), null, exporter);

        var e1 = assertThrows(IllegalArgumentException.class, params::baseUrl);
        assertTrue(e1.getMessage().contains("'baseUrl' is required"));

        var e2 = assertThrows(IllegalArgumentException.class, params::apiToken);
        assertTrue(e2.getMessage().contains("'apiToken' is required"));

        var e3 = assertThrows(IllegalArgumentException.class, params::path);
        assertTrue(e3.getMessage().contains("'path' is required"));

        assertDoesNotThrow(params::kvPairs); // default action

        // kvPairs required when action is writeKv
        params = TaskParams.of(
                Map.of("action", "writeKv"), null, exporter);
        var e4 = assertThrows(IllegalArgumentException.class, params::kvPairs);
        assertTrue(e4.getMessage().contains("'kvPairs' is required"));

        // cubbyhole is a v1 engine
        params = TaskParams.of(
                Map.of("path", "cubbyhole/mysecret", "engineVersion", 2), null, exporter);
        assertEquals(1, params.engineVersion());
    }

    /**
     * The data returned by the task may be either a Map containing all of the
     * key/value pairs in the Vault secret OR a single String value from the
     * Vault secret when a specific secret "key" is specified.
     */
    @Test
    void mapDataTest(){
        var vars = Map.<String, Object>of("path", "secret/mysecret");
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
        var vars = Map.<String, Object>of("path", "secret/mysecret", "key", "top_secret");
        var params = TaskParams.of(vars, null, exporter);

        var result = HashiVaultTaskResult
                .of(true, Map.of("top_secret", "value"), null, params);
        var s = assertInstanceOf(String.class, result.data(),
                "data should be String when key param is given");
        assertEquals("value", s);
    }

    @Test
    void vaultHttp400IsErrorStatus() {
        assertFalse(HashiVaultTaskCommon.isErrorStatus(399));
        assertTrue(HashiVaultTaskCommon.isErrorStatus(400));
        assertTrue(HashiVaultTaskCommon.isErrorStatus(500));
        assertFalse(HashiVaultTaskCommon.isErrorStatus(600));
    }

    @Test
    void v2WriteKvHelperHonorsDryRunMode() {
        Context ctx = mock(Context.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
        when(ctx.variables()).thenReturn(new MapBackedVariables(Map.of(
                TaskParams.DEFAULT_PARAMS_KEY, Map.of(
                        TaskParams.BASE_URL_KEY, "http://127.0.0.1:1",
                        TaskParams.API_TOKEN_KEY, "token"
                )
        )));
        when(ctx.processConfiguration().dryRun()).thenReturn(true);

        HashiVaultTask task = new HashiVaultTask(ctx, mock(SecretService.class));

        assertDoesNotThrow(() -> task.writeKV("secret/test", Map.of("key", "value")));
    }

}
