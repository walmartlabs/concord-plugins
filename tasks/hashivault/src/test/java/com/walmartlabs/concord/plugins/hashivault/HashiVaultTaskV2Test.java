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

import com.walmartlabs.concord.plugins.hashivault.v2.HashiVaultTask;
import com.walmartlabs.concord.runtime.v2.sdk.Compiler;
import com.walmartlabs.concord.runtime.v2.sdk.*;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult.SimpleResult;
import com.walmartlabs.concord.sdk.MapUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class HashiVaultTaskV2Test extends AbstractVaultTest {

    @Test
    public void testCubbyV2() throws Exception {
        Map<String, Object> varMap = new HashMap<>();
        varMap.put("path", "cubbyhole/hello");

        Variables vars = new MapBackedVariables(varMap);
        SimpleResult result = getTask().execute(vars);

        Assert.assertTrue(result.ok());
        final Map<String, Object> data = MapUtils.assertMap(result.values(), "data");
        assertEquals("cubbyVal", data.get("cubbyKey"));
    }

    @Test
    public void testKvV2() throws Exception {
        Map<String, Object> varMap = new HashMap<>();
        varMap.put("path", "secret/testing");

        Variables vars = new MapBackedVariables(varMap);
        SimpleResult result = getTask().execute(vars);

        Assert.assertTrue(result.ok());
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
        SimpleResult result = getTask().execute(vars);

        Assert.assertTrue(result.ok());
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
    public void testReadKvSinglePublicMethodV2() throws Exception {
        String path = "secret/testing";
        String result = getTask().readKV(path, "db_password");

        assertEquals("dbpassword1", result);
    }

    @Test
    public void testWriteCubbyPublicMethodV2() throws Exception {
        String path = "cubbyhole/newSecretTaskPublicMethodV2";
        Map<String, Object> kvPairs = new HashMap<>();
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
    public void testWriteKvPublicMethodV2() throws Exception {
        String path = "secret/newSecretTaskPublicMethodV2";
        Map<String, Object> kvPairs = new HashMap<>();
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

    private void writeAndRead(String path, String prefix) throws Exception {
        HashiVaultTask task = getTask();
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

        task = getTask(); // resets context

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

    private HashiVaultTask getTask() {
        Map<String, Object> defaults = new HashMap<>();
        defaults.put("baseUrl", getBaseUrl());
        defaults.put("apiToken", getApiToken());

        Map<String, Object> vars = new HashMap<>();
        vars.put("hashivaultParams", defaults);

        Context ctx = new MockContext(vars, null);

        return new HashiVaultTask(ctx);
    }

    private static class MockContext implements Context {

        private final Variables variables;
        private final Variables defaultVariables;

        MockContext(Map<String, Object> vars, Map<String, Object> defs) {
            this.variables = new MapBackedVariables(vars);
            this.defaultVariables = new MapBackedVariables(defs);
        }

        @Override
        public Path workingDirectory() {
            return null;
        }

        @Override
        public UUID processInstanceId() {
            return null;
        }

        @Override
        public Variables variables() {
            return variables;
        }

        @Override
        public Variables defaultVariables() {
            return defaultVariables;
        }

        @Override
        public FileService fileService() {
            return null;
        }

        @Override
        public DockerService dockerService() {
            return null;
        }

        @Override
        public SecretService secretService() {
            return null;
        }

        @Override
        public LockService lockService() {
            return null;
        }

        @Override
        public ApiConfiguration apiConfiguration() {
            return null;
        }

        @Override
        public ProcessConfiguration processConfiguration() {
            return null;
        }

        @Override
        public Execution execution() {
            return null;
        }

        @Override
        public Compiler compiler() {
            return null;
        }

        @Override
        public <T> T eval(Object o, Class<T> aClass) {
            return null;
        }

        @Override
        public <T> T eval(Object o, Map<String, Object> map, Class<T> aClass) {
            return null;
        }

        @Override
        public void suspend(String s) {
        }

        @Override
        public void reentrantSuspend(String s, Map<String, Serializable> map) {
        }
    }
}
