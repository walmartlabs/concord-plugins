package com.walmartlabs.concord.plugins.terraform.backend;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2019 Walmart Inc.
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.walmartlabs.concord.plugins.terraform.TaskConstants;
import com.walmartlabs.concord.plugins.terraform.TerraformTaskTest;
import com.walmartlabs.concord.sdk.Context;
import com.walmartlabs.concord.sdk.LockService;
import com.walmartlabs.concord.sdk.MockContext;
import com.walmartlabs.concord.sdk.ObjectStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class BackendManagerTest {

    private BackendFactoryV1 backendManager;
    private Path dstDir;

    @RegisterExtension
    static WireMockExtension wireMockRule = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @BeforeEach
    public void setUp() throws Exception {
        Path tmpDir = Paths.get("/tmp/concord");
        if (!Files.exists(tmpDir)) {
            Files.createDirectories(tmpDir);
        }
        dstDir = Files.createTempDirectory(tmpDir, "test");

        Context ctx = new MockContext(Collections.singletonMap("workDir", dstDir.toAbsolutePath().toString()));

        LockService lockService = mock(LockService.class);
        ObjectStorage objectStorage = TerraformTaskTest.createObjectStorage(wireMockRule);
        backendManager = new BackendFactoryV1(ctx, lockService, objectStorage);
    }

    @Test
    public void validateBackendManagerRejectsUnsupportedBackends() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> backendManager.getBackend(backendConfiguration("donkykong", new HashMap<>())));
        assertEquals("Unsupported backend type: donkykong", exception.getMessage());
    }

    @Test
    public void validateBackendManagerRejectsMultipleBackendConfigurations() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> backendManager.getBackend(badBackendConfiguration()));
        assertEquals("Only a single backend configuration is supported. There are 2 configured.", exception.getMessage());
    }

    @Test
    @SuppressWarnings({"rawtypes"})
    public void validateS3BackendConfigurationSerialization() throws Exception {
        Backend backend = backendManager.getBackend(s3BackendConfiguration());
        backend.init(dstDir);

        File overrides = dstDir.resolve("concord_override.tf.json").toFile();
        try (Reader reader = new FileReader(overrides)) {
            ObjectMapper objectMapper = new ObjectMapper();
            Map s3 = (Map) ((Map) ((Map) objectMapper.readValue(reader, Map.class).get("terraform")).get("backend")).get("s3");
            assertEquals("bucket-value", s3.get("bucket"));
            assertEquals("key-value", s3.get("key"));
            assertEquals("region-value", s3.get("region"));
            assertEquals("dynamodb_table-value", s3.get("dynamodb_table"));
            assertTrue((Boolean) s3.get("encrypt"));
        }
    }

    @Test
    public void validateRemoteBackendTfCliConfigFile() throws Exception {
        Map<String, Object> cfg = remoteBackendConfiguration();

        Backend backend = backendManager.getBackend(cfg);
        backend.init(dstDir);
        Map<String, String> env = backend.prepareEnv(cfg);
        assertTrue(env.containsKey("TF_CLI_CONFIG_FILE"));
    }

    private static Context context() {
        return new MockContext(new HashMap<>());
    }

    //
    // - task: terraform
    //     in:
    //       backend:
    //         s3:
    //           bucket: "${bucket}"
    //           key: "${key}"
    //           region: "${region}"
    //           encrypt: ${encrypt}
    //           dynamodb_table: "${dynamodb_table}"
    //
    private static Map<String, Object> s3BackendConfiguration() {
        Map<String, Object> m = new HashMap<>();
        m.put("bucket", "bucket-value");
        m.put("key", "key-value");
        m.put("region", "region-value");
        m.put("encrypt", true);
        m.put("dynamodb_table", "dynamodb_table-value");
        return backendConfiguration("s3", m);
    }

    // - task: terraform
    //   in:
    //     backend:
    //       remote:
    //         hostname: "tfe.example.com"
    //         organization:  "Concord"
    //         token: "abcxyz"
    //         workspaces:
    //           name: "test-workspace"
    private static Map<String, Object> remoteBackendConfiguration() {
        Map<String, Object> m = new HashMap<>();
        m.put("hostname", "tfe.example.com");
        m.put("organization", "Concord");
        m.put("token", "abcxyz");
        m.put("workspace", Collections.singletonMap("name", "test-workspaces"));
        return backendConfiguration("remote", m);
    }

    private static Map<String, Object> backendConfiguration(String backendId, Map<String, Object> backendParameters) {
        Map<String, Object> cfg = new HashMap<>();
        Map<String, Object> backend = new HashMap<>();
        backend.put(backendId, backendParameters);
        cfg.put(TaskConstants.BACKEND_KEY, backend);
        return cfg;
    }

    private static Map<String, Object> badBackendConfiguration() {
        Map<String, Object> cfg = new HashMap<>();
        Map<String, Object> backend = new HashMap<>();
        backend.put("one", new HashMap<>());
        backend.put("two", new HashMap<>());
        cfg.put(TaskConstants.BACKEND_KEY, backend);
        return cfg;
    }

}
