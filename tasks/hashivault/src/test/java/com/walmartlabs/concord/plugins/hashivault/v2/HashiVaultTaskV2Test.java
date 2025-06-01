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

import com.walmartlabs.concord.plugins.hashivault.AbstractVaultTest;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.MapBackedVariables;
import com.walmartlabs.concord.runtime.v2.sdk.SecretService;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult.SimpleResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HashiVaultTaskV2Test extends AbstractVaultTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    Context ctx;

    @Mock
    SecretService secretService;

    @Test
    void testReadTokenFromSecretV2() throws Exception {
        var varMap = Map.of(
                "apiTokenSecret", Map.of(
                        "org", "my-org",
                        "name", "my-secret"
                ),
                "baseUrl", getVaultBaseUrl(),
                "path", "cubbyhole/hello"
        );

        when(secretService.exportAsString("my-org", "my-secret", null))
                .thenReturn(getApiToken());

        var result = getTask(false).execute(new MapBackedVariables(varMap));

        assertTrue(result.ok());
        var data = assertInstanceOf(Map.class, result.values().get("data"));
        assertEquals("cubbyVal", data.get("cubbyKey"));
    }

    @Test
    void testCubbyV2() {
        var vars = new MapBackedVariables(Map.of(
                "path", "cubbyhole/hello"
        ));

        var result = getTask(true).execute(vars);

        assertTrue(result.ok());
        var data = assertInstanceOf(Map.class, result.values().get("data"));
        assertEquals("cubbyVal", data.get("cubbyKey"));
    }

    @Test
    void testKvV2() {
        var vars = new MapBackedVariables(Map.of(
                "path", "secret/testing"
        ));
        SimpleResult result = getTask(true).execute(vars);

        assertTrue(result.ok());
        var data = assertInstanceOf(Map.class, result.values().get("data"));
        assertEquals("password1", data.get("top_secret"));
        assertEquals("dbpassword1", data.get("db_password"));
    }

    @Test
    void testIgnoreSslVerificationV2() {
        var vars = new MapBackedVariables(Map.of(
                "baseUrl", getVaultHttpsBaseUrl(),
                "path", "secret/testing"
        ));

        // -- expect ssl verification failure with self-signed certs

        assertThrows(Exception.class, () -> getTask(true).execute(vars),
                "HTTPS should fail with self-signed certs and verification enabled");

        // -- should work with verification disabled

        var result = getTask(true).execute(new MapBackedVariables(Map.of(
                "baseUrl", getVaultHttpsBaseUrl(),
                "path", "secret/testing",
                "verifySsl", false
        )));

        assertTrue(result.ok());
        var data = assertInstanceOf(Map.class, result.values().get("data"));
        assertEquals("password1", data.get("top_secret"));
        assertEquals("dbpassword1", data.get("db_password"));
    }

    @Test
    void testReadKvSingleV2() {
        var vars = new MapBackedVariables(Map.of(
                "path", "secret/testing",
                "key", "db_password"
        ));
        var result = getTask(true).execute(vars);

        assertTrue(result.ok());
        var data = assertInstanceOf(String.class, result.values().get("data"));
        assertEquals("dbpassword1", data);
    }

    @Test
    void testWriteCubbyV2() {
        writeAndRead("cubbyhole/newSecretTaskV2", "v2CubbyExecute");
    }

    @Test
    void testWriteKvV2() {
        writeAndRead("secret/newSecretTaskV2", "v2SecretExecute");
    }

    @Test
    void testReadKvSinglePublicMethodV2()  {
        String path = "secret/testing";
        String result = getTask(true).readKV(path, "db_password");

        assertEquals("dbpassword1", result);
    }

    @Test
    void testWriteCubbyPublicMethodV2() {
        var path = "cubbyhole/newSecretTaskPublicMethodV2";
        var kvPairs = Map.<String, Object>of(
                "key1", "cubbyValue1",
                "key2", "cubbyValue2"
        );

        getTask(true).writeKV(path, kvPairs);

        // -- now get the values back

        Map<String, Object> data = getTask(true) // resets context
                .readKV(path);

        assertEquals("cubbyValue1", data.get("key1"));
        assertEquals("cubbyValue2", data.get("key2"));
    }

    @Test
    void testWriteKvPublicMethodV2() {
        var path = "secret/newSecretTaskPublicMethodV2";
        var kvPairs = Map.<String, Object>of(
                "key1", "value1",
                "key2", "value2"
        );

        getTask(true).writeKV(path, kvPairs);

        // -- now get the values back

        var data = getTask(true) // resets context
                .readKV(path);

        assertEquals("value1", data.get("key1"));
        assertEquals("value2", data.get("key2"));
    }

    private void writeAndRead(String path, String prefix) {
        var input1 = new MapBackedVariables(Map.of(
                "action", "writeKV",
                "path", path,
                "kvPairs", Map.of(
                        "key1", prefix + "Value1",
                        "key2", prefix + "Value2"
                )
        ));

        var writeResult = getTask(true).execute(input1);

        assertTrue(writeResult.ok());

        // -- now get the values back

        var input2 = new MapBackedVariables(Map.of(
                "action", "readKV",
                "path", path
        ));

        var readResult = getTask(true) // resets context
                .execute(input2);

        assertTrue(readResult.ok());

        var data = assertInstanceOf(Map.class, readResult.values().get("data"));
        assertEquals(prefix + "Value1", data.get("key1"));
        assertEquals(prefix + "Value2", data.get("key2"));
    }

    private HashiVaultTask getTask(boolean setDefaults) {
        Map<String, Object> vars = new HashMap<>();

        if (setDefaults) {
            vars.put("hashivaultParams", Map.of(
                    "baseUrl", getVaultBaseUrl(),
                    "apiToken", getApiToken(),
                    "retryIntervalMs", 1
            ));
        }

        when(ctx.processConfiguration().dryRun()).thenReturn(false);
        when(ctx.variables()).thenReturn(new MapBackedVariables(vars));

        return new HashiVaultTask(ctx, secretService);
    }
}
