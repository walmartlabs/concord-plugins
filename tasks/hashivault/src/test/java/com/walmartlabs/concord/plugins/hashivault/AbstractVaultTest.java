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

import org.junit.ClassRule;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.vault.VaultContainer;

public abstract class AbstractVaultTest {
    private static final String TEST_VAULT_TOKEN = "my-test-token";

    @ClassRule
    public final static VaultContainer<?> vaultContainer =
            new VaultContainer<>(DockerImageName
                    // set env var to point to specific image/custom repo
                    .parse(System.getenv("VAULT_IMAGE_VERSION"))
                    .asCompatibleSubstituteFor("vault"))
                    .withVaultToken(TEST_VAULT_TOKEN)
                    .withSecretInVault("secret/testing", "top_secret=password1","db_password=dbpassword1")
                    .withSecretInVault("cubbyhole/hello", "cubbyKey=cubbyVal");

    protected String getBaseUrl() {
        return String.format("http://%s:%s",
                vaultContainer.getHost(),
                vaultContainer.getMappedPort(8200));
    }

    protected String getApiToken() {
        return TEST_VAULT_TOKEN;
    }
}
