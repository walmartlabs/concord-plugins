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
import com.walmartlabs.concord.plugins.akeyless.v2.AkeylessTask;
import com.walmartlabs.concord.runtime.v2.sdk.SecretService;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.*;

import static org.junit.Assert.*;

@Ignore("Requires auth info")
public class ITs {

    private Map<String, Object> itsProps;

    private static final String testPath = "/concord_its";

    @Before
    public void setup() throws Exception {
        itsProps = loadITProps();
    }

    @Test
    public void testCreateUpdateGet() {
        final String path1 = String.join("/", testPath, randomString(6));
        final String value1 = randomString(10);
        final String path2 = String.join("/", testPath, randomString(6));
        final String value2 = randomString(10);

        createSecret(path1, value1);
        createSecret(path2, value2);

        getSecret(path1, value1);

        getSecrets(path1, value1, path2, value2);

        updateSecret(path1, value1 + "_new");
        getSecret(path1, value1 + "_new");

    }


    private String randomString(int len) {
        int lower = 97; // 'a'
        int upper = 122; // 'z'

        return new SecureRandom().ints(lower, upper + 1)
                .limit(len)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    private Map<String, Object> createAuth() {
        Map<String, Object> apiKey = new HashMap<>();
        apiKey.put("accessId", getITsProp("accessId"));
        apiKey.put("accessKey", getITsProp("accessKey"));

        return Collections.singletonMap("apiKey", apiKey);
    }

    private void createSecret(String path, String value) {
        Map<String, Object> cfg = new HashMap<>();

        cfg.put("apiBasePath", getITsProp("apiBasePath"));
        cfg.put("auth", createAuth());
        cfg.put("action", "createSecret");
        cfg.put("secretPath", path);
        cfg.put("description", "Description for " + path);
        cfg.put("value", value);
        cfg.put("multiline", false);

        TaskParams params = TaskParamsImpl.of(cfg, Collections.emptyMap(), Collections.emptyMap(), null);
        AkeylessTaskResult result = new AkeylessCommon().execute(params);

        assertTrue(result.getOk());
        Map<String, String> data = result.getData();
    }

    public void updateSecret(String path, String value) {
        Map<String, Object> cfg = new HashMap<>();

        cfg.put("apiBasePath", getITsProp("apiBasePath"));
        cfg.put("auth", createAuth());
        cfg.put("action", "updateSecret");
        cfg.put("secretPath", path);
        cfg.put("value", value);
        cfg.put("multiline", false);
        cfg.put("keepPreviousVersion", false);

        TaskParams params = TaskParamsImpl.of(cfg, Collections.emptyMap(), Collections.emptyMap(), null);
        AkeylessTaskResult result = new AkeylessCommon().execute(params);

        assertTrue(result.getOk());
        Map<String, String> data = result.getData();
    }

    public void getSecrets(String path1, String expected1, String path2, String expected2) {
        Map<String, Object> cfg = new HashMap<>();

        cfg.put("apiBasePath", getITsProp("apiBasePath"));

        Map<String, Object> apiKey = new HashMap<>();
        apiKey.put("accessId", getITsProp("accessId"));
        apiKey.put("accessKey", getITsProp("accessKey"));

        Map<String, Object> auth = Collections.singletonMap("apiKey", apiKey);

        cfg.put("auth", auth);
        cfg.put("action", "getSecrets");
        cfg.put("secretPaths", Arrays.asList(path1, path2));

        TaskParams params = TaskParamsImpl.of(cfg, Collections.emptyMap(), Collections.emptyMap(), null);
        AkeylessTaskResult result = new AkeylessCommon().execute(params);

        assertTrue(result.getOk());
        Map<String, String> data = result.getData();
        assertEquals(2, data.size());

        assertEquals(expected1, data.get(path1));
        assertEquals(expected2, data.get(path2));
    }

    public void getSecret(String path, String expectedValue) {
        Map<String, Object> cfg = new HashMap<>();

        cfg.put("apiBasePath", getITsProp("apiBasePath"));

        Map<String, Object> apiKey = new HashMap<>();
        apiKey.put("accessId", getITsProp("accessId"));
        apiKey.put("accessKey", getITsProp("accessKey"));

        Map<String, Object> auth = Collections.singletonMap("apiKey", apiKey);

        cfg.put("auth", auth);
        cfg.put("action", "getSecret");
        cfg.put("secretPath", path);

        TaskParams params = TaskParamsImpl.of(cfg, Collections.emptyMap(), Collections.emptyMap(), null);
        AkeylessTaskResult result = new AkeylessCommon().execute(params);

        assertTrue(result.getOk());
        Map<String, String> data = result.getData();

        assertEquals(expectedValue, data.get(path));
    }

    public void testGetSecretPublic() {
        Map<String, Object> cfg = new HashMap<>();

        cfg.put("apiBasePath", getITsProp("apiBasePath"));

        Map<String, Object> apiKey = new HashMap<>();
        apiKey.put("accessId", getITsProp("accessId"));
        apiKey.put("accessKey", getITsProp("accessKey"));

        Map<String, Object> auth = Collections.singletonMap("apiKey", apiKey);
        cfg.put("auth", auth);

        Map<String, Object> arguments = Collections.singletonMap("akeylessParams", cfg);

        SecretService secretService = Mockito.mock(SecretService.class);


        AkeylessTask task = new AkeylessTask(new MockContext(Collections.emptyMap(), arguments), secretService);

        String data = task.getSecret(getITsProp("secretPath"));

        assertNotNull(data);
    }

    @SuppressWarnings("unchecked")
    protected <T> T getITsProp(String k) {
        if (!itsProps.containsKey(k)) {
            throw new IllegalArgumentException(String.format(
                    "Cannot find value for '%s' in ITs properties file", k));
        }

        return (T) itsProps.get(k);
    }

    private static Map<String, Object> loadITProps() throws Exception {
        String propsFileEnv = System.getenv("IT_PROPERTIES_FILE");

        if (propsFileEnv == null || propsFileEnv.isEmpty()) {
            throw new IllegalArgumentException("IT_PROPERTIES_FILE environment variable is required for tests.");
        }

        Path propsFile = Paths.get(propsFileEnv);
        if (!Files.exists(propsFile)) {
            throw new RuntimeException("Cannot find akeyless IT properties file: " + propsFile);
        }
        Properties props = new Properties();
        try (FileInputStream is = new FileInputStream(propsFile.toFile())) {
            props.load(is);
        }

        Map<String, Object> result = new HashMap<>(props.size());
        props.forEach((key, value) -> result.put((String) key, value));

        return result;
    }
}
