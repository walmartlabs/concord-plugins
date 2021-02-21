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

import com.bettercloud.vault.SslConfig;
import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.response.LogicalResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.Map;

public class HashiVaultTaskCommon {
    Logger log = LoggerFactory.getLogger(HashiVaultTaskCommon.class);

    public HashiVaultTaskCommon() {
        // empty default constructor
    }

    private static VaultConfig buildConfig(TaskParams params) {
        final String apiToken = params.apiToken();
        final String apiBaseUrl = params.baseUrl();

        final VaultConfig config = new VaultConfig()
                .address(apiBaseUrl)
                .token(apiToken)
                .sslConfig(new SslConfig().verify(params.verifySsl()));

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

    public HashiVaultTaskResult execute(TaskParams params) {
        final VaultConfig config = buildConfig(params);
        final Vault vault = new Vault(config);
        HashiVaultTaskResult result;

        switch (params.action()) {
            case READKV:
                Map<String, String> data = readValue(vault, params);
                result = HashiVaultTaskResult.of(true, data, null, params);
                break;
            case WRITEKV:
                writeValue(vault, params);
                result = HashiVaultTaskResult.of(true, null, null, params);
                break;
            default:
                throw new HashiVaultTaskException("Unsupported action: " + params.action());
        }

        return result;
    }

    private Map<String, String> readValue(Vault vault, TaskParams params) {
        try {
            final LogicalResponse r = vault.withRetries(3, 5000).logical()
                    .withNameSpace(params.ns())
                    .read(params.path());
            final int status = r.getRestResponse().getStatus();

            if (status > 400 && status < 599) {
                // why didn't vault throw a VaultException?
                String body = new String(r.getRestResponse().getBody(), Charset.defaultCharset());
                throw new VaultException(body, status);
            }

            return r.getData();

        } catch (VaultException e) {
            String msg = String.format("Error reading from vault (%s): %s",
                    e.getHttpStatusCode(), e.getMessage());
            throw new HashiVaultTaskException(msg);
        } catch (Exception e) {
            String msg = "Unexpected error reading from vault: " + e.getMessage();
            throw new HashiVaultTaskException(msg);
        }
    }

    private void writeValue(Vault vault, TaskParams params) {
        try {
            final LogicalResponse r = vault.withRetries(3, 5000).logical()
                    .withNameSpace(params.ns())
                    .write(params.path(), params.kvPairs());
            final int status = r.getRestResponse().getStatus();

            if (status > 400 && status < 599) {
                // why didn't vault throw a VaultException?
                String body = new String(r.getRestResponse().getBody(), Charset.defaultCharset());
                throw new VaultException(body, status);
            }
        } catch (VaultException e) {
            String msg = String.format("Error writing to vault (%s): %s",
                    e.getHttpStatusCode(), e.getMessage());
            throw new HashiVaultTaskException(msg);
        } catch (Exception e) {
            String msg = "Unexpected error writing to vault: " + e.getMessage();
            log.error(msg);
            throw new HashiVaultTaskException(msg);
        }
    }
}
