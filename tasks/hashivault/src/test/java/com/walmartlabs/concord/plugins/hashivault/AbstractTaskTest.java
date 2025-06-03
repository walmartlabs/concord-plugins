package com.walmartlabs.concord.plugins.hashivault;

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

import com.walmartlabs.concord.runtime.v2.sdk.MapBackedVariables;
import com.walmartlabs.concord.runtime.v2.sdk.Variables;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static com.walmartlabs.concord.plugins.hashivault.TestConstants.MAP_VAL;
import static com.walmartlabs.concord.plugins.hashivault.TestConstants.SECRET_PATH;
import static com.walmartlabs.concord.plugins.hashivault.TestConstants.STRING_VAL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AbstractTaskTest {

    @Mock
    protected HashiVaultTaskCommon common;

    @Mock
    private TaskParams.SecretExporter secretExporter;

    @Test
    void testReadTokenFromSecretV2() throws Exception {
        mockReadMulti(common);

        var varMap = Map.of(
                "apiTokenSecret", Map.of(
                        "org", "my-org",
                        "name", "my-secret"
                ),
                "baseUrl", "https://mock-api.local",
                "path", SECRET_PATH
        );

        when(secretExporter.exportAsString("my-org", "my-secret", null))
                .thenReturn("mock-token");

        var result = getTask(false).executeCommon(new MapBackedVariables(varMap), secretExporter);
        assertTrue(result.ok());
        assertEquals(MAP_VAL, result.data());

        verify(secretExporter, times(1)).exportAsString("my-org", "my-secret", null);
    }

    @Test
    void testReadKvSinglePublicMethod()  {
        mockReadSingle("db_password", STRING_VAL, common);

        var result = getTask(true).readKV(SECRET_PATH, "db_password", secretExporter);

        assertEquals(STRING_VAL, result);
    }

    @Test
    void testWriteKvMultiPublicMethod() {
        mockWriteMulti(common);

        getTask(true).writeKV(SECRET_PATH, MAP_VAL, secretExporter);
    }

    @Test
    void testReadKvMultiPublicMethod() {
        mockReadMulti(common);

        var data = getTask(true).readKV(SECRET_PATH, secretExporter);

        assertEquals(MAP_VAL, data);
    }

    @Test
    void testDefaultInjection() {
        var task = new BasicImpl();

        assertEquals(HashiVaultTaskCommon.class, task.getDelegate().getClass());
    }

    @Test
    void testDryRun() {
        var task = new BasicImpl();

        var varMap = Map.<String, Object>of(
                "action", "writeKv",
                "apiToken", "mock-token",
                "baseUrl", "https://mock-api.local",
                "path", SECRET_PATH,
                "dryRun", true
        );

        var result = task.executeCommon(new MapBackedVariables(varMap), secretExporter);
        assertTrue(result.ok());
        assertNull(result.data());
    }

    private static class BasicImpl extends AbstractHashiVaultTask {
        @Override
        public TaskParams createParams(Variables input, TaskParams.SecretExporter secretExporter) {
            return TaskParams.of(input, Map.of(), secretExporter);
        }

        @Override
        protected HashiVaultTaskCommon getDelegate() {
            return super.getDelegate();
        }
    }

    private AbstractHashiVaultTask getTask(boolean setDefaults) {

        return new AbstractHashiVaultTask() {
            @Override
            public TaskParams createParams(Variables input, TaskParams.SecretExporter secretExporter) {
                Map<String, Object> defaults = setDefaults
                        ? Map.of("baseUrl", "https://mock-api.local", "apiToken", "mock-key", "retryIntervalMs", 1)
                        : Map.of();
                return TaskParams.of(input, defaults, secretExporter);
            }

            @Override
            protected HashiVaultTaskCommon getDelegate() {
                return common;
            }
        };
    }

    public static void mockReadSingle(String key, String value, HashiVaultTaskCommon common) {
        doAnswer(invocation -> {
            var params = invocation.getArgument(0, TaskParams.class);
            assertEquals(SECRET_PATH, params.path());
            assertEquals(key, params.key());
            return HashiVaultTaskResult.of(true, Map.of(params.key(), value), null, params);
        }).when(common).execute(any());
    }

    public static void mockWriteMulti(HashiVaultTaskCommon common) {
        doAnswer(invocation -> {
            var params = invocation.getArgument(0, TaskParams.class);
            assertEquals(TaskParams.Action.WRITEKV, params.action());
            assertEquals(SECRET_PATH, params.path());
            assertEquals(MAP_VAL, params.kvPairs());
            return HashiVaultTaskResult.of(true, null, null, params);
        }).when(common).execute(any());
    }

    public static void mockReadMulti(HashiVaultTaskCommon common) {
        doAnswer(invocation -> {
            var params = invocation.getArgument(0, TaskParams.class);
            assertEquals(SECRET_PATH, params.path());
            return HashiVaultTaskResult.of(true, Map.of(
                    "key1", "value1",
                    "key2", "value2"
            ), null, params);
        }).when(common).execute(any());
    }

    public static void mockReadError(HashiVaultTaskCommon common) {
        doAnswer(invocation -> {
            var params = invocation.getArgument(0, TaskParams.class);
            assertEquals(SECRET_PATH, params.path());
            return HashiVaultTaskResult.of(false, Map.of(), "mock-error", params);
        }).when(common).execute(any());
    }
}
