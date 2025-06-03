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

import org.testcontainers.containers.Network;
import org.testcontainers.containers.NginxContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;
import org.testcontainers.vault.VaultContainer;

@Testcontainers
public abstract class AbstractVaultTest {

    private static final String TEST_VAULT_TOKEN = "my-test-token";
    private static final int VAULT_PORT = 8200;
    private static final int NGINX_SSL_PORT = 443;

    protected static final Network network = Network.newNetwork();
    @Container
    public final static VaultContainer<?> vaultContainer =
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
    public static NginxContainer<?> nginxContainer =
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
