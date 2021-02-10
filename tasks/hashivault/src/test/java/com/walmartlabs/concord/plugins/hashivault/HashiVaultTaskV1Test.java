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

import com.walmartlabs.concord.plugins.hashivault.v1.HashiVaultTask;
import com.walmartlabs.concord.sdk.*;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class HashiVaultTaskV1Test extends AbstractVaultTest {
    private static final Logger log = LoggerFactory.getLogger(HashiVaultTaskV1Test.class);

    private Context ctx;

    private HashiVaultTask getTask() {
        ctx = new MockContext(new HashMap<>());

        Map<String, Object> defaults = new HashMap<>(2);
        defaults.put("apiToken", getApiToken());
        defaults.put("baseUrl", getBaseUrl());

        ctx.setVariable("hashivaultParams", defaults);

        HashiVaultTask t = new HashiVaultTask();
        injectVariable(t, "hashivaultParams", defaults);
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
    public void testReadCubbyV1() throws Exception {
        Task task = getTask();
        ctx.setVariable("path", "cubbyhole/hello");

        task.execute(ctx);
        Map<String, Object> result = ContextUtils.assertMap(ctx, "result");

        assertTrue(MapUtils.getBoolean(result,"ok", false));

        Map<String, Object> data = MapUtils.getMap(result, "data", Collections.emptyMap());
        assertEquals("cubbyVal", MapUtils.getString(data, "cubbyKey"));

        log.info("{}", ContextUtils.assertMap(ctx, "result"));
    }

    @Test
    public void testReadKvV1() throws Exception {
        Task task = getTask();
        ctx.setVariable("path", "secret/testing");

        task.execute(ctx);
        Map<String, Object> result = ContextUtils.assertMap(ctx, "result");

        assertTrue(MapUtils.getBoolean(result,"ok", false));

        Map<String, Object> data = MapUtils.getMap(result, "data", Collections.emptyMap());
        assertEquals("password1", MapUtils.getString(data, "top_secret"));
        assertEquals("dbpassword1", MapUtils.getString(data, "db_password"));
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
        Task task = getTask();
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

        task = getTask(); // resets context
        ctx.setVariable("action", "readKV");
        ctx.setVariable("path", path);

        task.execute(ctx);
        Map<String, Object> readResult = ContextUtils.assertMap(ctx, "result");

        Map<String, Object> data = MapUtils.getMap(readResult, "data", Collections.emptyMap());
        assertEquals(prefix + "Value1", MapUtils.getString(data, "key1"));
        assertEquals(prefix + "Value2", MapUtils.getString(data, "key2"));
    }

    @Test
    public void testWriteCubbyPublicMethodV1() throws Exception {
        String path = "cubbyhole/newSecretTaskPublicMethodV1";
        Map<String, Object> kvPairs = new HashMap<>(2);
        kvPairs.put("key1", "cubbyValue1");
        kvPairs.put("key2", "cubbyValue2");

        HashiVaultTask task = getTask();
        task.writeKV(path, kvPairs);

        // -- now get the values back

        task = getTask(); // resets context
        Map<String, Object> data = task.readKV(path);

        assertEquals("cubbyValue1", MapUtils.getString(data, "key1"));
        assertEquals("cubbyValue2", MapUtils.getString(data, "key2"));
    }

    @Test
    public void testWriteKvPublicMethodV1() throws Exception {
        String path = "secret/newSecretTaskPublicMethodV1";
        Map<String, Object> kvPairs = new HashMap<>(2);
        kvPairs.put("key1", "value1");
        kvPairs.put("key2", "value2");

        HashiVaultTask task = getTask();
        task.writeKV(path, kvPairs);

        // -- now get the values back

        task = getTask(); // resets context
        Map<String, Object> data = task.readKV(path);

        assertEquals("value1", MapUtils.getString(data, "key1"));
        assertEquals("value2", MapUtils.getString(data, "key2"));
    }
}
