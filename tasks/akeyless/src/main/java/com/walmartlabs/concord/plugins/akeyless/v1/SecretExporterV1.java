package com.walmartlabs.concord.plugins.akeyless.v1;

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
import com.walmartlabs.concord.sdk.Context;
import com.walmartlabs.concord.sdk.SecretService;

import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

public class SecretExporterV1 implements SecretExporter {
    private final Context ctx;
    private final String txId;
    private final String workDir;
    private final SecretService secretService;

    public SecretExporterV1(Context ctx, UUID txId, Path workDir, SecretService secretService) {
        this.ctx = ctx;
        this.txId = txId.toString();
        this.workDir = workDir.toString();
        this.secretService = secretService;
    }

    @Override
    public Secret.StringSecret exportAsString(String orgName, String secretName, String password) throws Exception {
        return new Secret.StringSecret(secretService.exportAsString(ctx, txId, orgName, secretName, password));
    }

    @Override
    public Secret.CredentialsSecret exportCredentials(String orgName, String secretName, String password) throws Exception {
        Map<String, String> up =  secretService.exportCredentials(ctx, txId, workDir, orgName, secretName, password);

        return new Secret.CredentialsSecret(up.get("username"), up.get("password"));
    }
}
