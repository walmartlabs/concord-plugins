package com.walmartlabs.concord.plugins.hashivault;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2025 Walmart Inc., Concord Authors
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

import com.bettercloud.vault.SslConfig;
import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;

public interface VaultProvider {

    Vault getVault(TaskParams taskParams);

    class DefaultVaultProvider implements VaultProvider {

        @Override
        public Vault getVault(TaskParams taskParams) {
            return new Vault(buildConfig(taskParams));
        }

        private static VaultConfig buildConfig(TaskParams params) {
            final String apiToken = params.apiToken();
            final String apiBaseUrl = params.baseUrl();
            VaultConfig config;

            try {
                config = new VaultConfig()
                        .address(apiBaseUrl)
                        .token(apiToken)
                        .sslConfig(new SslConfig().verify(params.verifySsl()).build());
            } catch (Exception e) {
                throw new HashiVaultTaskException("Error building Vault configuration. " + e.getMessage());
            }

            if (params.path().matches("^/?cubbyhole.*")) {
                config.engineVersion(1);
            } else {
                config.engineVersion(params.engineVersion());
            }

            try {
                return config.build();
            } catch (VaultException e) {
                String msg = String.format("Error building vault config: %s", e.getMessage());
                throw new HashiVaultTaskException(msg);
            }
        }
    }
}
