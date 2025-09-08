package com.walmartlabs.concord.plugins.git.v1;

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

import com.walmartlabs.concord.plugins.git.GitSecretService;
import com.walmartlabs.concord.sdk.Context;
import com.walmartlabs.concord.sdk.ContextUtils;
import com.walmartlabs.concord.sdk.SecretService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

class SecretServiceV1 implements GitSecretService {

    private final SecretService delegate;
    private final Context context;

    SecretServiceV1(SecretService delegate, Context context) {
        this.delegate = delegate;
        this.context = context;
    }

    @Override
    public Path exportPrivateKeyAsFile(String orgName, String secretName, String pwd) throws Exception {
        UUID instanceId = ContextUtils.getTxId(context);
        Path workDir = ContextUtils.getWorkDir(context);

        Map<String, String> result = delegate.exportKeyAsFile(context, instanceId.toString(), workDir.toString(), orgName, secretName, pwd);
        Files.deleteIfExists(Paths.get(result.get("public")));
        return Paths.get(result.get("private"));
    }

    @Override
    public Path exportFile(String orgName, String secretName, String pwd) throws Exception {
        UUID instanceId = ContextUtils.getTxId(context);
        Path workDir = ContextUtils.getWorkDir(context);

        return Paths.get(delegate.exportAsFile(context, instanceId.toString(), workDir.toString(), orgName, secretName, pwd));
    }
}
