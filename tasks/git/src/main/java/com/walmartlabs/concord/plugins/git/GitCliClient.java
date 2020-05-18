package com.walmartlabs.concord.plugins.git;

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

import com.walmartlabs.concord.repository.GitClientConfiguration;
import com.walmartlabs.concord.repository.ImmutableGitClientConfiguration;
import com.walmartlabs.concord.sdk.Secret;

import java.nio.file.Path;

public class GitCliClient implements GitClient {

    private final boolean shallowClone;

    public GitCliClient(boolean shallowClone) {
        this.shallowClone = shallowClone;
    }

    @Override
    public void cloneRepo(String uri, String branchName, Secret secret, Path dst) {
        ImmutableGitClientConfiguration.Builder cfg = GitClientConfiguration.builder()
                .httpLowSpeedLimit(0)
                .sshTimeout(600)
                .shallowClone(shallowClone);

        if (secret instanceof TokenSecret) {
            cfg.oauthToken(((TokenSecret) secret).getToken());
            secret = null;
        }

        new com.walmartlabs.concord.repository.GitClient(cfg.build())
                .fetch(uri, branchName, null, false, secret, dst);
    }
}
