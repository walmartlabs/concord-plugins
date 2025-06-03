package com.walmartlabs.concord.plugins.hashivault.v2;

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

import com.walmartlabs.concord.plugins.hashivault.AbstractTaskTest;
import com.walmartlabs.concord.plugins.hashivault.HashiVaultTaskCommon;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.MapBackedVariables;
import com.walmartlabs.concord.runtime.v2.sdk.SecretService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static com.walmartlabs.concord.plugins.hashivault.TestConstants.MAP_VAL;
import static com.walmartlabs.concord.plugins.hashivault.TestConstants.SECRET_PATH;
import static com.walmartlabs.concord.plugins.hashivault.TestConstants.STRING_VAL;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HashiVaultTaskV2Test  {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context ctx;

    @Mock
    private SecretService secretService;

    @Mock
    private HashiVaultTaskCommon common;

    @Test
    void testExecute() {
        AbstractTaskTest.mockReadMulti(common);

        var result = getTask(true).execute(new MapBackedVariables(Map.of()));
        assertTrue(result.ok());
        assertEquals(MAP_VAL, result.values().get("data"));
    }

    @Test
    void testExecuteError() {
        AbstractTaskTest.mockReadError(common);

        var result = getTask(true).execute(new MapBackedVariables(Map.of()));
        assertFalse(result.ok());
        assertEquals("mock-error", result.error());
    }

    @Test
    void testReadTokenFromSecretV2() throws Exception {
        var varMap = Map.of(
                "apiTokenSecret", Map.of(
                        "org", "my-org",
                        "name", "my-secret"
                ),
                "baseUrl", "https://mock-api.local",
                "path", SECRET_PATH
        );

        AbstractTaskTest.mockReadMulti(common);
        when(secretService.exportAsString("my-org", "my-secret", null))
                .thenReturn("mock-token");

        var result = getTask(false).execute(new MapBackedVariables(varMap));
        assertTrue(result.ok());

        var data = assertInstanceOf(Map.class, result.values().get("data"));
        assertEquals(MAP_VAL, data);

        verify(secretService, times(1)).exportAsString("my-org", "my-secret", null);
    }

    @Test
    void testReadKvSinglePublicMethodV2()  {
        AbstractTaskTest.mockReadSingle("db_password", STRING_VAL, common);

        var result = getTask(true).readKV(SECRET_PATH, "db_password");

        assertEquals(STRING_VAL, result);
    }

    @Test
    void testWriteKvMultiPublicMethodV2() {
        AbstractTaskTest.mockWriteMulti(common);

        assertDoesNotThrow(() -> getTask(true).writeKV(SECRET_PATH, MAP_VAL));
    }

    @Test
    void testReadKvMultiPublicMethodV2() {
        AbstractTaskTest.mockReadMulti(common);

        var data = getTask(true).readKV(SECRET_PATH);

        assertEquals(MAP_VAL, data);
    }

    private HashiVaultTask getTask(boolean setDefaults) {
        var vars = new HashMap<String, Object>();

        if (setDefaults) {
            vars.put("hashivaultParams", Map.of(
                    "baseUrl", "https://mock-api.local",
                    "apiToken", "mock-key",
                    "retryIntervalMs", 1,
                    "path", SECRET_PATH
            ));
        }

        when(ctx.variables()).thenReturn(new MapBackedVariables(vars));
        when(ctx.processConfiguration().dryRun()).thenReturn(false);

        return new HashiVaultTask(ctx, secretService) {
            @Override
            protected HashiVaultTaskCommon getDelegate() {
                return common;
            }
        };
    }
}
