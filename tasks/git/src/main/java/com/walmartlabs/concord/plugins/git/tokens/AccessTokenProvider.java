package com.walmartlabs.concord.plugins.git.tokens;

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
import com.walmartlabs.concord.plugins.git.Utils;
import com.walmartlabs.concord.plugins.git.model.Auth;

import java.nio.file.Files;
import java.util.Objects;
import java.util.stream.Stream;

public interface AccessTokenProvider {

    String getToken();

    static AccessTokenProvider fromAuth(Auth auth,
                                        String baseUrl,
                                        String installationRepo,
                                        GitSecretService secretService) {

        Stream.of(auth.accessToken(), auth.appInstallation(), auth.appInstallationSecret())
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid 'auth' input."));

        if (auth.accessToken() != null) {
            return new BasicTokenProvider(auth.accessToken());
        } else if (auth.appInstallation() != null) {
            var appInstallation = auth.appInstallation();
            return new AppInstallationTokenProvider(appInstallation, baseUrl, installationRepo);
        } else if (auth.appInstallationSecret() != null) {
            var sAuth = Objects.requireNonNull(auth.appInstallationSecret());
            try (var is = Files.newInputStream(secretService.exportFile(sAuth.org(), sAuth.name(), sAuth.password()))) {
                var a = Utils.getObjectMapper().readValue(is, Auth.AppInstallationAuth.class);
                return new AppInstallationTokenProvider(a, baseUrl, installationRepo);
            } catch (Exception e) {
                throw new RuntimeException("Failed to read the app installation token from secret '" + sAuth.name() + "'", e);
            }
        } else {
            throw new IllegalArgumentException("Unknown auth type");
        }
    }

}
