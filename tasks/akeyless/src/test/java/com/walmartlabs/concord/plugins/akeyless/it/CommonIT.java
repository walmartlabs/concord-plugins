package com.walmartlabs.concord.plugins.akeyless.it;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2022 Walmart Inc., Concord Authors
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

import com.walmartlabs.concord.plugins.akeyless.AkeylessCommon;
import com.walmartlabs.concord.plugins.akeyless.AkeylessTaskResult;
import com.walmartlabs.concord.plugins.akeyless.model.TaskParams;
import com.walmartlabs.concord.plugins.akeyless.model.TaskParamsImpl;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class CommonIT extends AbstractIT {

    private static final String testPath = "/concord_its";
    private static final String txId = UUID.randomUUID().toString();

    @Test
    void testWithAuth() {
        testCreateUpdateGet(null);
    }

    @Test
    void testWithAccessToken() {
        final String accessToken = createAccessToken();

        testCreateUpdateGet(accessToken);
    }

    void testCreateUpdateGet(String accessToken) {
        final String path1 = String.join("/", testPath, randomString(6));
        final String value1 = randomString(10);
        final String path2 = String.join("/", testPath, randomString(6));
        final String value2 = randomString(10);

        createSecret(path1, value1, accessToken);
        createSecret(path2, value2, accessToken);

        getSecret(path1, value1, accessToken);

        getSecrets(path1, value1, path2, value2, accessToken);

        updateSecret(path1, value1 + "_new", accessToken);
        getSecret(path1, value1 + "_new", accessToken);

        deleteItem(path1, accessToken);
        deleteItem(path2, accessToken);
    }

    private String randomString(int len) {
        int lower = 97; // 'a'
        int upper = 122; // 'z'

        return new SecureRandom().ints(lower, upper + 1)
                .limit(len)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    private String createAccessToken() {
        Map<String, Object> cfg = baseConfig();
        cfg.put("apiBasePath", getITsProp("apiBasePath"));
        cfg.put("auth", createAuth());
        cfg.put("action", "auth");

        TaskParams params = TaskParamsImpl.of(cfg, Collections.emptyMap(), Collections.emptyMap(), null);
        AkeylessTaskResult result = new AkeylessCommon().execute(params);

        assertTrue(result.getOk());
        Map<String, String> data = result.getData();

        String token = data.get("accessToken");
        assertNotNull(token);

        return token;
    }

    private void createSecret(String path, String value, String accessToken) {
        Map<String, Object> cfg = baseConfig();

        cfg.put("apiBasePath", getITsProp("apiBasePath"));
        cfg.put("action", "createSecret");
        cfg.put("path", path);
        cfg.put("description", "Description for " + path);
        cfg.put("value", value);
        cfg.put("multiline", false);
        if (Objects.isNull(accessToken)) {
            cfg.put("auth", createAuth());
        } else {
            cfg.put("accessToken", accessToken);
        }

        TaskParams params = TaskParamsImpl.of(cfg, Collections.emptyMap(), Collections.emptyMap(), null);
        AkeylessTaskResult result = new AkeylessCommon().execute(params);

        assertTrue(result.getOk());
        Map<String, String> data = result.getData();
    }

    public void updateSecret(String path, String value, String accessToken) {
        Map<String, Object> cfg = baseConfig();

        cfg.put("apiBasePath", getITsProp("apiBasePath"));
        cfg.put("action", "updateSecret");
        cfg.put("path", path);
        cfg.put("value", value);
        cfg.put("multiline", false);
        cfg.put("keepPreviousVersion", false);
        if (Objects.isNull(accessToken)) {
            cfg.put("auth", createAuth());
        } else {
            cfg.put("accessToken", accessToken);
        }

        TaskParams params = TaskParamsImpl.of(cfg, Collections.emptyMap(), Collections.emptyMap(), null);
        AkeylessTaskResult result = new AkeylessCommon().execute(params);

        assertTrue(result.getOk());
    }

    public void deleteItem(String path, String accessToken) {
        Map<String, Object> cfg = baseConfig();

        cfg.put("apiBasePath", getITsProp("apiBasePath"));
        cfg.put("action", "deleteItem");
        cfg.put("path", path);
        cfg.put("deleteImmediately", true);
        if (Objects.isNull(accessToken)) {
            cfg.put("auth", createAuth());
        } else {
            cfg.put("accessToken", accessToken);
        }

        TaskParams params = TaskParamsImpl.of(cfg, Collections.emptyMap(), Collections.emptyMap(), null);
        AkeylessTaskResult result = new AkeylessCommon().execute(params);

        assertTrue(result.getOk());
    }

    public void getSecrets(String path1, String expected1, String path2, String expected2, String accessToken) {
        Map<String, Object> cfg = baseConfig();

        cfg.put("apiBasePath", getITsProp("apiBasePath"));

        Map<String, Object> apiKey = new HashMap<>();
        apiKey.put("accessId", getITsProp("accessId"));
        apiKey.put("accessKey", getITsProp("accessKey"));

        Map<String, Object> auth = Collections.singletonMap("apiKey", apiKey);

        cfg.put("action", "getSecrets");
        cfg.put("paths", Arrays.asList(path1, path2));
        if (Objects.isNull(accessToken)) {
            cfg.put("auth", createAuth());
        } else {
            cfg.put("accessToken", accessToken);
        }

        TaskParams params = TaskParamsImpl.of(cfg, Collections.emptyMap(), Collections.emptyMap(), null);
        AkeylessTaskResult result = new AkeylessCommon().execute(params);

        assertTrue(result.getOk());
        Map<String, String> data = result.getData();
        assertEquals(2, data.size());

        assertEquals(expected1, data.get(path1));
        assertEquals(expected2, data.get(path2));
    }

    public void getSecret(String path, String expectedValue, String accessToken) {
        Map<String, Object> cfg = baseConfig();

        cfg.put("apiBasePath", getITsProp("apiBasePath"));

        Map<String, Object> apiKey = new HashMap<>();
        apiKey.put("accessId", getITsProp("accessId"));
        apiKey.put("accessKey", getITsProp("accessKey"));

        Map<String, Object> auth = Collections.singletonMap("apiKey", apiKey);

        cfg.put("action", "getSecret");
        cfg.put("path", path);
        if (Objects.isNull(accessToken)) {
            cfg.put("auth", createAuth());
        } else {
            cfg.put("accessToken", accessToken);
        }

        TaskParams params = TaskParamsImpl.of(cfg, Collections.emptyMap(), Collections.emptyMap(), null);
        AkeylessTaskResult result = new AkeylessCommon().execute(params);

        assertTrue(result.getOk());
        Map<String, String> data = result.getData();

        assertEquals(expectedValue, data.get(path));
    }

    private static Map<String, Object> baseConfig() {
        Map<String, Object> cfg = new HashMap<>();
        cfg.put("txId", txId);

        return cfg;
    }
}
