package com.walmartlabs.concord.plugins.terraform.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.walmartlabs.concord.plugins.terraform.Constants;
import com.walmartlabs.concord.plugins.terraform.TerraformTaskTest;
import com.walmartlabs.concord.sdk.Context;
import com.walmartlabs.concord.sdk.LockService;
import com.walmartlabs.concord.sdk.MockContext;
import com.walmartlabs.concord.sdk.ObjectStorage;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class BackendManagerTest {

    private LockService lockService;
    private ObjectStorage objectStorage;
    private BackendManager backendManager;
    private Path dstDir;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.wireMockConfig().port(12345));

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        lockService = mock(LockService.class);
        objectStorage = TerraformTaskTest.createObjectStorage(wireMockRule);
        backendManager = new BackendManager(lockService, objectStorage);
        dstDir = Files.createTempDirectory(Paths.get("/tmp/concord"), "test");
    }

    @Test
    public void validateBackendManagerRejectsUnsupportedBackends() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("Unsupported backend type: donkykong");
        backendManager.getBackend(backendConfiguration("donkykong", new HashMap<>()));
    }

    @Test
    public void validateBackendManagerRejectsMultipleBackendConfigurations() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("Only a single backend configuration is supported. There are 2 configured.");
        backendManager.getBackend(badBackendConfiguration());
    }

    @Test
    public void validateS3BackendConfigurationSerialization() throws Exception {

        Backend backend = backendManager.getBackend(s3BackendConfiguration());
        backend.init(context(), dstDir);

        File overrides = dstDir.resolve("concord_override.tf.json").toFile();
        try (Reader reader = new FileReader(overrides)) {
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String,Object> s3 = (Map)((Map)((Map) objectMapper.readValue(reader, Map.class).get("terraform")).get("backend")).get("s3");
            assertEquals("bucket-value", s3.get("bucket"));
            assertEquals("key-value", s3.get("key"));
            assertEquals("region-value", s3.get("region"));
            assertEquals("dynamodb_table-value", s3.get("dynamodb_table"));
            assertTrue((Boolean)s3.get("encrypt"));
        }
    }

    private Context context() {
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
    private Map<String,Object> s3BackendConfiguration() {
        Map<String, Object> backendParameters = new HashMap();
        backendParameters.put("bucket", "bucket-value");
        backendParameters.put("key", "key-value");
        backendParameters.put("region", "region-value");
        backendParameters.put("encrypt", true);
        backendParameters.put("dynamodb_table", "dynamodb_table-value");
        return backendConfiguration("s3", backendParameters);
    }

    private Map<String,Object> backendConfiguration(String backendId, Map<String,Object> backendParameters) {
        Map<String, Object> cfg = new HashMap<>();
        Map<String, Object> backend = new HashMap();
        backend.put(backendId, backendParameters);
        cfg.put(Constants.BACKEND_KEY, backend);
        return cfg;
    }

    private Map<String,Object> badBackendConfiguration() {
        Map<String, Object> cfg = new HashMap<>();
        Map<String, Object> backend = new HashMap();
        backend.put("one", new HashMap<>());
        backend.put("two", new HashMap<>());
        cfg.put(Constants.BACKEND_KEY, backend);
        return cfg;
    }

}
