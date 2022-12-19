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
import com.walmartlabs.concord.runtime.v2.sdk.SecretService;

public class SecretExporterV2 implements SecretExporter {
    private final SecretService secretService;

    public SecretExporterV2(SecretService secretService) {
        this.secretService = secretService;
    }

    @Override
    public Secret.StringSecret exportAsString(String orgName, String secretName, String password) throws Exception {
        return new Secret.StringSecret(secretService.exportAsString(orgName, secretName, password));
    }

    @Override
    public Secret.CredentialsSecret exportCredentials(String orgName, String secretName, String password) throws Exception {
        SecretService.UsernamePassword up = secretService.exportCredentials(orgName, secretName, password);
        return new Secret.CredentialsSecret(up.username(), up.password());
    }
}
