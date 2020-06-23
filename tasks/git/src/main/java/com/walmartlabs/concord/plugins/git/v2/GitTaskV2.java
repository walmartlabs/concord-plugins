package com.walmartlabs.concord.plugins.git.v2;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2020 Walmart Inc.
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
import com.walmartlabs.concord.plugins.git.GitTask;
import com.walmartlabs.concord.runtime.v2.sdk.SecretService;
import com.walmartlabs.concord.runtime.v2.sdk.Task;
import com.walmartlabs.concord.runtime.v2.sdk.Variables;
import com.walmartlabs.concord.runtime.v2.sdk.WorkingDirectory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@Named("git")
public class GitTaskV2 implements Task {

    private final GitTask delegate;

    @Inject
    public GitTaskV2(SecretService secretService, WorkingDirectory workDir) {
        this.delegate = new GitTask(new SecretServiceV2(secretService), workDir.getValue());
    }

    @Override
    public Serializable execute(Variables input) throws Exception {
        Map<String, Object> result = delegate.execute(input.toMap());
        return new HashMap<>(result);
    }

    static class SecretServiceV2 implements GitSecretService {

        private final SecretService delegate;

        SecretServiceV2(SecretService delegate) {
            this.delegate = delegate;
        }

        @Override
        public Path exportPrivateKeyAsFile(String orgName, String secretName, String pwd) throws Exception {
            SecretService.KeyPair result = delegate.exportKeyAsFile(orgName, secretName, pwd);
            Files.deleteIfExists(result.publicKey());
            return result.privateKey();
        }
    }
}
