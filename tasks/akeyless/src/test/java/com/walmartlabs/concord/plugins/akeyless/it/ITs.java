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
import java.util.*;

import static org.junit.Assert.*;

@Ignore("Requires auth info")
public class ITs {

    private Map<String, Object> itsProps;

    @Before
    public void setup() throws Exception {
        itsProps = loadITProps();
    }

    @Test
    public void testGetSecrets() {
        Map<String, Object> cfg = new HashMap<>();

        cfg.put("apiBasePath", getITsProp("apiBasePath"));

        Map<String, Object> apiKey = new HashMap<>();
        apiKey.put("accessId", getITsProp("accessId"));
        apiKey.put("accessKey", getITsProp("accessKey"));

        Map<String, Object> auth = Collections.singletonMap("apiKey", apiKey);

        cfg.put("auth", auth);
        cfg.put("action", "getSecrets");
        String rawPaths = getITsProp("secretPaths");
        List<String> paths = Arrays.asList(rawPaths.split(","));
        cfg.put("secretPaths", paths);

        TaskParams params = TaskParamsImpl.of(cfg, Collections.emptyMap(), Collections.emptyMap(), null);
        AkeylessTaskResult result = new AkeylessCommon().execute(params);

        assertTrue(result.getOk());
        Map<String, String> data = result.getData();
        assertEquals(paths.size(), data.size());

        paths.forEach(p -> assertTrue(data.containsKey(p)));
    }

    @Test
    public void testUpdateSecret() {
        Map<String, Object> cfg = new HashMap<>();

        cfg.put("apiBasePath", getITsProp("apiBasePath"));

        Map<String, Object> apiKey = new HashMap<>();
        apiKey.put("accessId", getITsProp("accessId"));
        apiKey.put("accessKey", getITsProp("accessKey"));

        Map<String, Object> auth = Collections.singletonMap("apiKey", apiKey);

        cfg.put("auth", auth);
        cfg.put("action", "updateSecret");
        cfg.put("secretPath", getITsProp("secretPath"));
        cfg.put("value", "goodbyeWorld");

        TaskParams params = TaskParamsImpl.of(cfg, Collections.emptyMap(), Collections.emptyMap(), null);
        AkeylessTaskResult result = new AkeylessCommon().execute(params);

        assertTrue(result.getOk());
        Map<String, String> data = result.getData();

    }

    @Test
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
