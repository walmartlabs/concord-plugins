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

import com.walmartlabs.concord.plugins.hashivault.model.MockSecretServiceDelegate;
import com.walmartlabs.concord.plugins.hashivault.model.MockSecretServiceV1;
import com.walmartlabs.concord.plugins.hashivault.v1.HashiVaultTask;
import com.walmartlabs.concord.sdk.*;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class HashiVaultTaskV1Test extends AbstractVaultTest {

    private Context ctx;
    private final SecretService secretService = new MockSecretServiceV1(new MockSecretServiceDelegate() {
        @Override
        public String exportString(String o, String n, String p) {
            return getApiToken();
        }
    });

    private HashiVaultTask getTask(boolean setDefaults) {
        ctx = new MockContext(new HashMap<>());
        HashiVaultTask t = new HashiVaultTask( secretService);
        injectVariable(t, "execution", ctx);

        ctx.setVariable("txId", "643cd26e-6d64-11eb-81f9-0800273425d4");
        if (setDefaults) {
            Map<String, Object> defaults = new HashMap<>(2);
            defaults.put("apiToken", getApiToken());
            defaults.put("baseUrl", getBaseUrl());

            ctx.setVariable("hashivaultParams", defaults);
            injectVariable(t, "hashivaultParams", defaults);
        }

        return t;
    }

    private static void injectVariable(Object target, String key, Object value) {
        Field[] fields = target.getClass().getDeclaredFields();
        for (Field field : fields) {
            InjectVariable a = field.getAnnotation(InjectVariable.class);
            if (a != null && a.value().equals(key)) {
                setField(target, field, value);
            }
        }
    }

    /**
     * Set the value of a Field in and object
     *
     * @param target Target object in which to set the field value
     * @param field  Field to inject
     * @param value  Value to inject
     */
    private static void setField(Object target, Field field, Object value) {
        try {
            if (!field.isAccessible()) {
                field.setAccessible(true);
            }

            field.set(target, value);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException("Variable injection failed on field " + field.getName());
        }
    }

    @Test
    public void testReadTokenFromSecretV1() throws Exception {
        Task task = getTask(false);
        Map<String, Object> secretInfo = new HashMap<>(3);
        secretInfo.put("org", "my-org");
        secretInfo.put("name", "my-secret");
        ctx.setVariable("apiTokenSecret", secretInfo);
        ctx.setVariable("baseUrl", getBaseUrl());
        ctx.setVariable("path", "cubbyhole/hello");

        // apiToken wasn't given directly, it should be read from SecretService
        task.execute(ctx);
        Map<String, Object> result = ContextUtils.assertMap(ctx, "result");

        assertTrue(MapUtils.getBoolean(result,"ok", false));

        Map<String, Object> data = MapUtils.getMap(result, "data", Collections.emptyMap());
        assertEquals("cubbyVal", MapUtils.getString(data, "cubbyKey"));
    }


    @Test
    public void testReadCubbyV1() throws Exception {
        Task task = getTask(true);
        ctx.setVariable("path", "cubbyhole/hello");

        task.execute(ctx);
        Map<String, Object> result = ContextUtils.assertMap(ctx, "result");

        assertTrue(MapUtils.getBoolean(result,"ok", false));

        Map<String, Object> data = MapUtils.getMap(result, "data", Collections.emptyMap());
        assertEquals("cubbyVal", MapUtils.getString(data, "cubbyKey"));
    }

    @Test
    public void testReadKvV1() throws Exception {
        Task task = getTask(true);
        ctx.setVariable("path", "secret/testing");

        task.execute(ctx);
        Map<String, Object> result = ContextUtils.assertMap(ctx, "result");

        assertTrue(MapUtils.getBoolean(result,"ok", false));

        Map<String, Object> data = MapUtils.getMap(result, "data", Collections.emptyMap());
        assertEquals("password1", MapUtils.getString(data, "top_secret"));
        assertEquals("dbpassword1", MapUtils.getString(data, "db_password"));
    }

    @Test
    public void testReadKvSingleV1() throws Exception {
        Task task = getTask(true);
        ctx.setVariable("path", "secret/testing");
        ctx.setVariable("key", "db_password");

        task.execute(ctx);
        Map<String, Object> result = ContextUtils.assertMap(ctx, "result");

        assertTrue(MapUtils.getBoolean(result,"ok", false));

        String data = MapUtils.getString(result, "data");
        assertEquals("dbpassword1", data);
    }

    @Test
    public void testWriteCubbyV1() throws Exception {
        testWriteAndRead("cubbyhole/newSecretTaskV1", "v1CubbyExecute");
    }

    @Test
    public void testWriteKvV1() throws Exception {
        testWriteAndRead("secret/newSecretTaskV1", "v1SecretExecute");
    }

    private void testWriteAndRead(String path, String prefix) throws Exception {
        Task task = getTask(true);
        ctx.setVariable("action", "writeKV");
        ctx.setVariable("path", path);

        Map<String, Object> kvPairs = new HashMap<>(2);
        kvPairs.put("key1", prefix + "Value1");
        kvPairs.put("key2", prefix + "Value2");
        ctx.setVariable("kvPairs", kvPairs);

        task.execute(ctx);
        Map<String, Object> writeResult = ContextUtils.assertMap(ctx, "result");

        assertTrue(MapUtils.getBoolean(writeResult,"ok", false));

        // -- now get the values back

        task = getTask(true); // resets context
        ctx.setVariable("action", "readKV");
        ctx.setVariable("path", path);

        task.execute(ctx);
        Map<String, Object> readResult = ContextUtils.assertMap(ctx, "result");

        Map<String, Object> data = MapUtils.getMap(readResult, "data", Collections.emptyMap());
        assertEquals(prefix + "Value1", MapUtils.getString(data, "key1"));
        assertEquals(prefix + "Value2", MapUtils.getString(data, "key2"));
    }

    @Test
    public void testReadKvSinglePublicMethodV1() {
        String path = "secret/testing";
        String result = getTask(true).readKV(ctx, path, "db_password");

        assertEquals("dbpassword1", result);
    }

    @Test
    public void testWriteCubbyPublicMethodV1() {
        String path = "cubbyhole/newSecretTaskPublicMethodV1";
        Map<String, Object> kvPairs = new HashMap<>(2);
        kvPairs.put("key1", "cubbyValue1");
        kvPairs.put("key2", "cubbyValue2");

        HashiVaultTask task = getTask(true);
        task.writeKV(ctx, path, kvPairs);

        // -- now get the values back

        task = getTask(true); // resets context
        Map<String, Object> data = task.readKV(ctx, path);

        assertEquals("cubbyValue1", MapUtils.getString(data, "key1"));
        assertEquals("cubbyValue2", MapUtils.getString(data, "key2"));
    }

    @Test
    public void testWriteKvPublicMethodV1() {
        String path = "secret/newSecretTaskPublicMethodV1";
        Map<String, Object> kvPairs = new HashMap<>(2);
        kvPairs.put("key1", "value1");
        kvPairs.put("key2", "value2");

        HashiVaultTask task = getTask(true);
        task.writeKV(ctx, path, kvPairs);

        // -- now get the values back

        task = getTask(true); // resets context
        Map<String, Object> data = task.readKV(ctx, path);

        assertEquals("value1", MapUtils.getString(data, "key1"));
        assertEquals("value2", MapUtils.getString(data, "key2"));
    }
}
