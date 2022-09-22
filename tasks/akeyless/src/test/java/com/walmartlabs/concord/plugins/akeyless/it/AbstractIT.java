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

import org.junit.jupiter.api.BeforeEach;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public abstract class AbstractIT {
    private Map<String, Object> itsProps;

    private static final String testPath = "/concord_its";

    @BeforeEach
    public void setup() throws Exception {
        itsProps = loadITProps();
    }

    protected String getAccessId() {
        return getITsProp("accessId");
    }

    protected String getAccessKey() {
        return getITsProp("accessKey");
    }

    protected Map<String, Object> createAuth() {
        Map<String, Object> cfg = new HashMap<>();
        cfg.put("accessId", getITsProp("accessId"));
        cfg.put("accessKey", getITsProp("accessKey"));

        return Collections.singletonMap("apiKey", cfg);
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
