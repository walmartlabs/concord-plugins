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

import com.walmartlabs.concord.common.AuthTokenProvider.OauthTokenProvider;
import com.walmartlabs.concord.common.cfg.OauthTokenConfig;
import com.walmartlabs.concord.repository.FetchRequest;
import com.walmartlabs.concord.repository.GitClientConfiguration;
import com.walmartlabs.concord.repository.ImmutableGitClientConfiguration;
import com.walmartlabs.concord.sdk.Secret;
import com.walmartlabs.concord.common.AuthTokenProvider;
import org.immutables.value.Value;

import java.nio.file.Path;
import java.time.Duration;

public class GitCliClient implements GitClient {

    private final boolean shallowClone;

    public GitCliClient(boolean shallowClone) {
        this.shallowClone = shallowClone;
    }

    @Override
    public void cloneRepo(String uri, String branchName, Secret secret, Path dst) {
        ImmutableGitClientConfiguration.Builder cfg = GitClientConfiguration.builder()
                .httpLowSpeedLimit(0)
                .sshTimeout(Duration.ofSeconds(600));

        ImmutableOauthTokenConfigImpl.Builder oauthBuilder = OauthTokenConfigImpl.builder();

        if (secret instanceof TokenSecret tokenSecret) {
            String token = tokenSecret.getToken();
            cfg.oauthToken(token); // obfuscates in logs
            oauthBuilder.oauthToken(token); // actual provider
            secret = null;
        }

        AuthTokenProvider secretTokenProvider = new OauthTokenProvider(oauthBuilder.build());

        new com.walmartlabs.concord.repository.GitClient(cfg.build(), secretTokenProvider)
                .fetch(FetchRequest.builder()
                        .url(uri)
                        .version(FetchRequest.Version.from(branchName))
                        .withCommitInfo(false)
                        .destination(dst)
                        .secret(secret)
                        .shallow(shallowClone)
                        .build());
    }

    @Value.Immutable
    interface OauthTokenConfigImpl extends OauthTokenConfig {
        static ImmutableOauthTokenConfigImpl.Builder builder() {
            return ImmutableOauthTokenConfigImpl.builder();
        }
    }
}
