package com.walmartlabs.concord.plugins.hashivault.v1;

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
import com.walmartlabs.concord.sdk.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static com.walmartlabs.concord.plugins.hashivault.TestConstants.MAP_VAL;
import static com.walmartlabs.concord.plugins.hashivault.TestConstants.SECRET_PATH;
import static com.walmartlabs.concord.plugins.hashivault.TestConstants.STRING_VAL;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HashiVaultTaskV1Test extends AbstractTaskTest {

    private Context ctx;

    @Mock
    private SecretService secretService;

    @Test
    void testExecute() {
        AbstractTaskTest.mockReadMulti(common);

        getTask(true).execute(ctx);

        var result = assertInstanceOf(Map.class, ctx.getVariable("result"));

        assertTrue((boolean) result.get("ok"));
        var data = assertInstanceOf(Map.class, result.get("data"));
        assertEquals(MAP_VAL, data);
    }

    @Test
    void testReadTokenFromSecretV1() throws Exception {
        AbstractTaskTest.mockReadMulti(common);

        var task = getTask(false);
        ctx.setVariable("apiTokenSecret", Map.of(
                "org", "my-org",
                "name", "my-secret"
        ));
        ctx.setVariable("baseUrl", "https://mock-api.local");
        ctx.setVariable("path", SECRET_PATH);

        when(secretService.exportAsString(any(), any(), any(), any(), Mockito.nullable(String.class)))
                .thenReturn("mock-token");

        // apiToken wasn't given directly, it should be read from SecretService
        task.execute(ctx);
        var result = assertInstanceOf(Map.class, ctx.getVariable("result"));
        assertTrue((boolean) result.get("ok"));

        var data = assertInstanceOf(Map.class, result.get("data"));
        assertEquals(MAP_VAL, data);

        verify(secretService, times(1))
                .exportAsString(any(), any(), any(), any(), Mockito.nullable(String.class));
    }

    @Test
    void testReadKvSinglePublicMethodV1() {
        AbstractTaskTest.mockReadSingle("db_password", STRING_VAL, common);

        var result = getTask(true).readKV(ctx, SECRET_PATH, "db_password");

        assertEquals(STRING_VAL, result);
    }

    @Test
    void testWriteKvMultiPublicMethodV1() {
        AbstractTaskTest.mockWriteMulti(common);

        assertDoesNotThrow(() -> getTask(true).writeKV(ctx, SECRET_PATH, MAP_VAL));
    }

    @Test
    void testReadKvMultiPublicMethodV1() {
        AbstractTaskTest.mockReadMulti(common);

        var data = getTask(true).readKV(ctx, SECRET_PATH);

        assertEquals(MAP_VAL, data);
    }

    private HashiVaultTask getTask(boolean setDefaults) {
        ctx = new MockContext(new HashMap<>());
        ctx.setVariable("txId", "643cd26e-6d64-11eb-81f9-0800273425d4");
        var task = new HashiVaultTask(secretService) {
            @Override
            protected HashiVaultTaskCommon getDelegate() {
                return common;
            }
        };

        if (setDefaults) {
            task.setDefaults(Map.of(
                    "baseUrl", "https://mock-api.local",
                    "apiToken", "mock-key",
                    "retryIntervalMs", 1,
                    "path", SECRET_PATH
            ));
        }

        return task;
    }
}
