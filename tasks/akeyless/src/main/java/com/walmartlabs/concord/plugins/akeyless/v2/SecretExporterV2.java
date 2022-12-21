package com.walmartlabs.concord.plugins.akeyless.v2;

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

import com.walmartlabs.concord.plugins.akeyless.SecretExporter;
import com.walmartlabs.concord.plugins.akeyless.model.Secret;
import com.walmartlabs.concord.plugins.akeyless.model.SecretCache;
import com.walmartlabs.concord.plugins.akeyless.model.SecretCacheImpl;
import com.walmartlabs.concord.plugins.akeyless.model.SecretCacheNoop;
import com.walmartlabs.concord.runtime.v2.sdk.SecretService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SecretExporterV2 implements SecretExporter {
    private static final Logger log = LoggerFactory.getLogger(SecretExporterV2.class);

    private final SecretService secretService;

    private SecretCache<Secret.StringSecret> stringCache;
    private SecretCache<Secret.CredentialsSecret> credentialCache;

    public SecretExporterV2(SecretService secretService) {
        this.secretService = secretService;
        this.stringCache = SecretCacheNoop.getStringCache();
        this.credentialCache = SecretCacheNoop.getCredentialCache();
    }

    public void initCache(String salt, boolean debug) {
        this.stringCache = SecretCacheImpl.getStringCache(salt, debug);
        this.credentialCache = SecretCacheImpl.getCredentialCache(salt, debug);
    }

    @Override
    public Secret.StringSecret exportAsString(String o, String n, String p) {
        return stringCache.get(o, n, () -> {
            String value;

            try {
                value = secretService.exportAsString(o, n, p);
            } catch (Exception e) {
                throw new RuntimeException(String.format("Error exporting secret '%s/%s': %s", o, n, e.getMessage()), e);
            }

            return new Secret.StringSecret(value);
        });
    }

    @Override
    public Secret.CredentialsSecret exportCredentials(String o, String n, String p) {
        return credentialCache.get(o, n, () -> {
            try {
                SecretService.UsernamePassword up = secretService.exportCredentials(o, n, p);
                return new Secret.CredentialsSecret(up.username(), up.password());
            } catch (Exception e) {
                log.error("error exporting credentials secret: {}", e.getMessage());
            }

            return new Secret.CredentialsSecret(null, null);
        });
    }
}
