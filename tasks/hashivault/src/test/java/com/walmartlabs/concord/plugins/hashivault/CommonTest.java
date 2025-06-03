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

import com.walmartlabs.concord.runtime.v2.sdk.MapBackedVariables;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.NginxContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;
import org.testcontainers.vault.VaultContainer;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@Testcontainers
@ExtendWith(MockitoExtension.class)
class CommonTest {

    private static final String TEST_VAULT_TOKEN = "my-test-token";
    private static final int VAULT_PORT = 8200;
    private static final int NGINX_SSL_PORT = 443;

    protected static final Network network = Network.newNetwork();
    @Container
    public static final VaultContainer<?> vaultContainer =
            new VaultContainer<>(DockerImageName
                    // set env var to point to specific image/custom repo
                    .parse(System.getenv("VAULT_IMAGE_VERSION"))
                    .asCompatibleSubstituteFor("vault"))
                    .withVaultToken(TEST_VAULT_TOKEN)
                    .withNetwork(network)
                    .withNetworkAliases("vault")
                    .withSecretInVault("secret/testing", "top_secret=password1","db_password=dbpassword1")
                    .withSecretInVault("cubbyhole/hello", "cubbyKey=cubbyVal")
                    // we could explicitly wait for the http endpoint to be available here,
                    // but the nginx check will implicitly handle that
                    .waitingFor(Wait.forLogMessage(".*Vault server started.*", 1));
    @Container
    public static final NginxContainer<?> nginxContainer =
            new NginxContainer<>(DockerImageName
                    // set env var to point to specific image/custom repo
                    .parse(System.getenv("NGINX_IMAGE_VERSION"))
                    .asCompatibleSubstituteFor("nginx"))
                    .withNetwork(network)
                    .withExposedPorts(NGINX_SSL_PORT)
                    .withEnv("VAULT_ENDPOINT", "http://vault:" + VAULT_PORT)
                    .withCopyFileToContainer(mountRes("nginx.conf"), "/etc/nginx/templates/default.conf.template")
                    .withCopyFileToContainer(mountRes("server.crt"), "/etc/nginx/certs/server.crt")
                    .withCopyFileToContainer(mountRes("server.key"), "/etc/nginx/certs/server.key")
                    .waitingFor(Wait.forHttps("/v1/sys/health")
                            .forPort(NGINX_SSL_PORT)
                            .forStatusCode(200)
                            .allowInsecure());

    @Mock
    private TaskParams.SecretExporter secretExporter;

    @Test
    void testReadTokenFromSecretV2() throws Exception {
        var varMap = Map.of(
                "apiTokenSecret", Map.of(
                        "org", "my-org",
                        "name", "my-secret"
                ),
                "baseUrl", getVaultBaseUrl(),
                "path", "cubbyhole/hello"
        );

        when(secretExporter.exportAsString("my-org", "my-secret", null))
                .thenReturn(getApiToken());

        var result = executeCommon(varMap, false);

        assertTrue(result.ok());
        var data = assertInstanceOf(Map.class, result.data());
        assertEquals("cubbyVal", data.get("cubbyKey"));
    }

    @Test
    void testCubbyV2() {
        var vars = Map.<String, Object>of(
                "path", "cubbyhole/hello"
        );

        var result = executeCommon(vars, true);

        assertTrue(result.ok());
        var data = assertInstanceOf(Map.class, result.data());
        assertEquals("cubbyVal", data.get("cubbyKey"));
    }

    @Test
    void testKvV2() {
        var vars = Map.<String, Object>of(
                "path", "secret/testing"
        );
        var result = executeCommon(vars, true);

        assertTrue(result.ok());
        var data = assertInstanceOf(Map.class, result.data());
        assertEquals("password1", data.get("top_secret"));
        assertEquals("dbpassword1", data.get("db_password"));
    }

    @Test
    void testIgnoreSslVerificationV2() {
        var vars = Map.<String, Object>of(
                "baseUrl", getVaultHttpsBaseUrl(),
                "path", "secret/testing"
        );

        // -- expect ssl verification failure with self-signed certs

        assertThrows(Exception.class, () -> executeCommon(vars, true),
                "HTTPS should fail with self-signed certs and verification enabled");

        // -- should work with verification disabled

        var result = executeCommon(Map.of(
                "baseUrl", getVaultHttpsBaseUrl(),
                "path", "secret/testing",
                "verifySsl", false
        ), true);

        assertTrue(result.ok());
        var data = assertInstanceOf(Map.class, result.data());
        assertEquals("password1", data.get("top_secret"));
        assertEquals("dbpassword1", data.get("db_password"));
    }

    @Test
    void testReadKvSingleV2() {
        var vars = Map.<String, Object>of(
                "path", "secret/testing",
                "key", "db_password"
        );
        var result = executeCommon(vars, true);

        assertTrue(result.ok());
        var data = assertInstanceOf(String.class, result.data());
        assertEquals("dbpassword1", data);
    }

    @Test
    void testWriteCubbyV2() {
        writeAndRead("cubbyhole/newSecretTaskV2", "v2CubbyExecute");
    }

    @Test
    void testWriteKvV2() {
        writeAndRead("secret/newSecretTaskV2", "v2SecretExecute");
    }

    private void writeAndRead(String path, String prefix) {
        var input1 = Map.of(
                "action", "writeKV",
                "path", path,
                "kvPairs", Map.of(
                        "key1", prefix + "Value1",
                        "key2", prefix + "Value2"
                )
        );

        var writeResult = executeCommon(input1, true);

        assertTrue(writeResult.ok());

        // -- now get the values back

        var input2 = Map.<String, Object>of(
                "action", "readKV",
                "path", path
        );

        var readResult = executeCommon(input2, true);

        assertTrue(readResult.ok());

        var data = assertInstanceOf(Map.class, readResult.data());
        assertEquals(prefix + "Value1", data.get("key1"));
        assertEquals(prefix + "Value2", data.get("key2"));
    }

    private Map<String, Object> defaultParams() {
        return Map.of(
            "baseUrl", getVaultBaseUrl(),
            "apiToken", getApiToken(),
            "retryIntervalMs", 1
        );
    }

    private HashiVaultTaskResult executeCommon(Map<String, Object> input, boolean setDefaults) {
        var defaults = setDefaults ? defaultParams() : Map.<String, Object>of();
        var params = TaskParams.of(new MapBackedVariables(input), defaults, secretExporter);

        return new HashiVaultTaskCommon(new VaultProvider.DefaultVaultProvider()).execute(params);
    }

    protected static String getVaultBaseUrl() {
        return String.format("http://%s:%s",
                vaultContainer.getHost(),
                vaultContainer.getMappedPort(8200));
    }

    protected String getVaultHttpsBaseUrl() {
        return String.format("https://%s:%s",
                nginxContainer.getHost(),
                nginxContainer.getMappedPort(443));
    }

    protected String getApiToken() {
        return TEST_VAULT_TOKEN;
    }

    /**
     * @param file resource to mount
     * @return a {@link MountableFile} from a given resource name
     */
    private static MountableFile mountRes(String file) {
        return MountableFile.forClasspathResource(file);
    }

}
