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

import com.walmartlabs.concord.plugins.hashivault.model.MockContextV2;
import com.walmartlabs.concord.plugins.hashivault.model.MockSecretServiceDelegate;
import com.walmartlabs.concord.plugins.hashivault.model.MockSecretServiceV2;
import com.walmartlabs.concord.plugins.hashivault.v2.HashiVaultTask;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.MapBackedVariables;
import com.walmartlabs.concord.runtime.v2.sdk.SecretService;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult.SimpleResult;
import com.walmartlabs.concord.runtime.v2.sdk.Variables;
import com.walmartlabs.concord.sdk.MapUtils;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class HashiVaultTaskV2Test extends AbstractVaultTest {

    @Test
    public void testReadTokenFromSecretV2() throws Exception {
        Map<String, Object> varMap = new HashMap<>();
        Map<String, Object> secretInfo = new HashMap<>(3);
        secretInfo.put("org", "my-org");
        secretInfo.put("name", "my-secret");
        varMap.put("apiTokenSecret", secretInfo);
        varMap.put("baseUrl", getBaseUrl());
        varMap.put("path", "cubbyhole/hello");

        Variables vars = new MapBackedVariables(varMap);
        SimpleResult result = getTask(false).execute(vars);

        assertTrue(result.ok());
        final Map<String, Object> data = MapUtils.assertMap(result.values(), "data");
        assertEquals("cubbyVal", data.get("cubbyKey"));
    }

    @Test
    public void testCubbyV2() throws Exception {
        Map<String, Object> varMap = new HashMap<>();
        varMap.put("path", "cubbyhole/hello");

        Variables vars = new MapBackedVariables(varMap);
        SimpleResult result = getTask(true).execute(vars);

        assertTrue(result.ok());
        final Map<String, Object> data = MapUtils.assertMap(result.values(), "data");
        assertEquals("cubbyVal", data.get("cubbyKey"));
    }

    @Test
    public void testKvV2() throws Exception {
        Map<String, Object> varMap = new HashMap<>();
        varMap.put("path", "secret/testing");

        Variables vars = new MapBackedVariables(varMap);
        SimpleResult result = getTask(true).execute(vars);

        assertTrue(result.ok());
        final Map<String, Object> data = MapUtils.assertMap(result.values(), "data");
        assertEquals("password1", data.get("top_secret"));
        assertEquals("dbpassword1", data.get("db_password"));
    }

    @Test
    public void testReadKvSingleV2() throws Exception {
        Map<String, Object> varMap = new HashMap<>();
        varMap.put("path", "secret/testing");
        varMap.put("key", "db_password");

        Variables vars = new MapBackedVariables(varMap);
        SimpleResult result = getTask(true).execute(vars);

        assertTrue(result.ok());
        final String data = MapUtils.getString(result.values(), "data");
        assertEquals("dbpassword1", data);
    }

    @Test
    public void testWriteCubbyV2() throws Exception {
        writeAndRead("cubbyhole/newSecretTaskV2", "v2CubbyExecute");
    }

    @Test
    public void testWriteKvV2() throws Exception {
        writeAndRead("secret/newSecretTaskV2", "v2SecretExecute");
    }

    @Test
    public void testReadKvSinglePublicMethodV2() {
        String path = "secret/testing";
        String result = getTask(true).readKV(path, "db_password");

        assertEquals("dbpassword1", result);
    }

    @Test
    public void testWriteCubbyPublicMethodV2() {
        String path = "cubbyhole/newSecretTaskPublicMethodV2";
        Map<String, Object> kvPairs = new HashMap<>();
        kvPairs.put("key1", "cubbyValue1");
        kvPairs.put("key2", "cubbyValue2");

        HashiVaultTask task = getTask(true);
        task.writeKV(path, kvPairs);

        // -- now get the values back

        task = getTask(true); // resets context
        Map<String, Object> data = task.readKV(path);

        assertEquals("cubbyValue1", MapUtils.getString(data, "key1"));
        assertEquals("cubbyValue2", MapUtils.getString(data, "key2"));
    }

    @Test
    public void testWriteKvPublicMethodV2() {
        String path = "secret/newSecretTaskPublicMethodV2";
        Map<String, Object> kvPairs = new HashMap<>();
        kvPairs.put("key1", "value1");
        kvPairs.put("key2", "value2");

        HashiVaultTask task = getTask(true);
        task.writeKV(path, kvPairs);

        // -- now get the values back

        task = getTask(true); // resets context
        Map<String, Object> data = task.readKV(path);

        assertEquals("value1", MapUtils.getString(data, "key1"));
        assertEquals("value2", MapUtils.getString(data, "key2"));
    }

    private void writeAndRead(String path, String prefix) throws Exception {
        HashiVaultTask task = getTask(true);
        Map<String, Object> vars1 = new HashMap<>();
        vars1.put("action", "writeKV");
        vars1.put("path", path);

        Map<String, Object> kvPairs = new HashMap<>(2);
        kvPairs.put("key1", prefix + "Value1");
        kvPairs.put("key2", prefix + "Value2");
        vars1.put("kvPairs", kvPairs);
        Variables input1 = new MapBackedVariables(vars1);

        SimpleResult writeResult = task.execute(input1);

        assertTrue(writeResult.ok());

        // -- now get the values back

        task = getTask(true); // resets context

        Map<String, Object> vars2 = new HashMap<>();
        vars2.put("action", "readKV");
        vars2.put("path", path);
        Variables input2 = new MapBackedVariables(vars2);

        SimpleResult readResult = task.execute(input2);

        assertTrue(readResult.ok());

        Map<String, Object> data = MapUtils.getMap(readResult.values(), "data", Collections.emptyMap());

        assertEquals(prefix + "Value1", MapUtils.getString(data, "key1"));
        assertEquals(prefix + "Value2", MapUtils.getString(data, "key2"));
    }

    private HashiVaultTask getTask(boolean setDefaults) {
        Map<String, Object> vars = new HashMap<>();

        if (setDefaults) {
            Map<String, Object> defaults = new HashMap<>();
            defaults.put("baseUrl", getBaseUrl());
            defaults.put("apiToken", getApiToken());

            vars.put("hashivaultParams", defaults);
        }

        Context ctx = new MockContextV2(vars, null);
        SecretService s = new MockSecretServiceV2(new MockSecretServiceDelegate() {
            @Override
            public String exportString(String o, String n, String p) {
                return getApiToken();
            }
        });

        return new HashiVaultTask(ctx, s);
    }
}
