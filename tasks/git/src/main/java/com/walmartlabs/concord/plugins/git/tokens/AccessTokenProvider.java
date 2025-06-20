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

public interface AccessTokenProvider {

    String getToken();

    static AccessTokenProvider fromAuth(Auth auth, String baseUrl, String installationRepo, GitSecretService secretService) {

        if (auth instanceof Auth.AccessTokenAuth tokenAuth) {
            return new BasicTokenProvider(tokenAuth.token());
        } else if (auth instanceof Auth.AppInstallationAuth appInstallAuth) {
            return new AppInstallationTokenProvider(appInstallAuth, baseUrl, installationRepo);
        } else if (auth instanceof Auth.AppInstallationSecretAuth sAuth) {
            try (var is = Files.newInputStream(secretService.exportFile(sAuth.org(), sAuth.name(), sAuth.password()))) {
                var a = Utils.getObjectMapper().readValue(is, Auth.AppInstallationAuth.class);
                return new AppInstallationTokenProvider(a, baseUrl, installationRepo);
            } catch (Exception e) {
                throw new RuntimeException("Failed to read the app installation token from secret '" + sAuth.name() + "'", e);
            }
        } else {
            throw new IllegalArgumentException("Unsupported auth type: " + auth.getClass().getName());
        }
    }

}
