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

import com.walmartlabs.concord.plugins.hashivault.AbstractVaultTest;
import com.walmartlabs.concord.sdk.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HashiVaultTaskV1Test extends AbstractVaultTest {

    private Context ctx;

    @Mock
    private SecretService secretService;

    private HashiVaultTask getTask(boolean setDefaults) {
        ctx = new MockContext(new HashMap<>());
        ctx.setVariable("txId", "643cd26e-6d64-11eb-81f9-0800273425d4");
        var task = new HashiVaultTask(secretService);

        if (setDefaults) {
            task.setDefaults(Map.of(
                    "apiToken", getApiToken(),
                    "baseUrl", getVaultBaseUrl(),
                    "retryIntervalMs", 1
            ));
        }

        return task;
    }

    @Test
    void testReadTokenFromSecretV1() throws Exception {
        var task = getTask(false);
        ctx.setVariable("apiTokenSecret", Map.of(
                "org", "my-org",
                "name", "my-secret"
        ));
        ctx.setVariable("baseUrl", getVaultBaseUrl());
        ctx.setVariable("path", "cubbyhole/hello");

        when(secretService.exportAsString(any(), any(), any(), any(), Mockito.nullable(String.class)))
                .thenReturn(getApiToken());

        // apiToken wasn't given directly, it should be read from SecretService
        task.execute(ctx);
        var result = assertInstanceOf(Map.class, ctx.getVariable("result"));
        assertTrue((boolean) result.get("ok"));

        var data = assertInstanceOf(Map.class, result.get("data"));
        assertEquals("cubbyVal", data.get("cubbyKey"));

        verify(secretService, times(1))
                .exportAsString(any(), any(), any(), any(), Mockito.nullable(String.class));
    }

    @Test
    void testReadCubbyV1() throws Exception {
        Task task = getTask(true);
        ctx.setVariable("path", "cubbyhole/hello");

        task.execute(ctx);
        var result = assertInstanceOf(Map.class, ctx.getVariable("result"));
        assertTrue((boolean) result.get("ok"));

        var data = assertInstanceOf(Map.class, result.get("data"));
        assertEquals("cubbyVal", data.get( "cubbyKey"));
    }

    @Test
    void testReadKvV1() throws Exception {
        Task task = getTask(true);
        ctx.setVariable("path", "secret/testing");

        task.execute(ctx);
        var result = assertInstanceOf(Map.class, ctx.getVariable("result"));
        assertTrue((boolean) result.get("ok"));

        var data = assertInstanceOf(Map.class, result.get("data"));
        assertEquals("password1", data.get("top_secret"));
        assertEquals("dbpassword1", data.get("db_password"));
    }

    @Test
    void testIgnoreSslVerificationV1() {
        var task = getTask(true);
        ctx.setVariable("baseUrl", getVaultHttpsBaseUrl());
        ctx.setVariable("path", "secret/testing");

        // -- expect ssl verification failure with self-signed certs

        assertThrows(Exception.class, () -> task.execute(ctx),
                "HTTPS should fail with self-signed certs");

        // -- should work with verification disabled

        ctx.setVariable("verifySsl", false);

        task.execute(ctx);
        var result = assertInstanceOf(Map.class, ctx.getVariable("result"));
        assertTrue((boolean) result.get("ok"));

        var data = assertInstanceOf(Map.class, result.get("data"));
        assertEquals("password1", data.get("top_secret"));
        assertEquals("dbpassword1", data.get("db_password"));
    }

    @Test
    void testReadKvSingleV1() throws Exception {
        var task = getTask(true);
        ctx.setVariable("path", "secret/testing");
        ctx.setVariable("key", "db_password");

        task.execute(ctx);
        var result = assertInstanceOf(Map.class, ctx.getVariable("result"));
        assertTrue((boolean) result.get("ok"));

        var data = assertInstanceOf(String.class, result.get("data"));
        assertEquals("dbpassword1", data);
    }

    @Test
    void testWriteCubbyV1() throws Exception {
        testWriteAndRead("cubbyhole/newSecretTaskV1", "v1CubbyExecute");
    }

    @Test
    void testWriteKvV1() throws Exception {
        testWriteAndRead("secret/newSecretTaskV1", "v1SecretExecute");
    }

    private void testWriteAndRead(String path, String prefix) throws Exception {
        var task = getTask(true);
        ctx.setVariable("action", "writeKV");
        ctx.setVariable("path", path);
        ctx.setVariable("kvPairs",  Map.of(
                "key1", prefix + "Value1",
                "key2", prefix + "Value2"
        ));

        task.execute(ctx);
        var writeResult = assertInstanceOf(Map.class, ctx.getVariable("result"));
        assertTrue((boolean) writeResult.get("ok"));

        // -- now get the values back

        task = getTask(true); // resets context
        ctx.setVariable("action", "readKV");
        ctx.setVariable("path", path);

        task.execute(ctx);
        var readResult = assertInstanceOf(Map.class, ctx.getVariable("result"));
        assertTrue((boolean) readResult.get("ok"));

        var data = assertInstanceOf(Map.class, readResult.get("data"));
        assertEquals(prefix + "Value1", data.get("key1"));
        assertEquals(prefix + "Value2", data.get("key2"));
    }

    @Test
    void testReadKvSinglePublicMethodV1() {
        var path = "secret/testing";
        var result = getTask(true).readKV(ctx, path, "db_password");

        assertEquals("dbpassword1", result);
    }

    @Test
    void testWriteCubbyPublicMethodV1() {
        var path = "cubbyhole/newSecretTaskPublicMethodV1";
        var kvPairs = new HashMap<String, Object>(2);
        kvPairs.put("key1", "cubbyValue1");
        kvPairs.put("key2", "cubbyValue2");

        getTask(true).writeKV(ctx, path, kvPairs);

        // -- now get the values back

        var data = getTask(true) // resets context
                .readKV(ctx, path);

        assertEquals("cubbyValue1", data.get("key1"));
        assertEquals("cubbyValue2", data.get("key2"));
    }

    @Test
    void testWriteKvPublicMethodV1() {
        var path = "secret/newSecretTaskPublicMethodV1";
        var kvPairs = new HashMap<String, Object>(2);
        kvPairs.put("key1", "value1");
        kvPairs.put("key2", "value2");

        getTask(true).writeKV(ctx, path, kvPairs);

        // -- now get the values back

        Map<String, Object> data = getTask(true) // resets context
                .readKV(ctx, path);

        assertEquals("value1", data.get("key1"));
        assertEquals("value2", data.get("key2"));
    }
}
