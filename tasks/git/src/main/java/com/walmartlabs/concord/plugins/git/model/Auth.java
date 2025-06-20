package com.walmartlabs.concord.plugins.git.model;

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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;

public sealed interface Auth
        permits Auth.AppInstallationAuth, Auth.AccessTokenAuth, Auth.AppInstallationSecretAuth {

    @Value.Immutable
    @Value.Style(jdkOnly = true)
    @JsonDeserialize(as = ImmutableAccessTokenAuth.class)
    non-sealed interface AccessTokenAuth extends Auth {
        String token();
    }

    @Value.Immutable
    @Value.Style(jdkOnly = true)
    @JsonDeserialize(as = ImmutableAppInstallationAuth.class)
    non-sealed interface AppInstallationAuth extends Auth {
        String privateKey();

        String clientId();

        @Value.Default
        default long refreshBufferSeconds() {
            return 60;
        }
    }

    @Value.Immutable
    @Value.Style(jdkOnly = true)
    @JsonDeserialize(as = ImmutableAppInstallationSecretAuth.class)
    non-sealed interface AppInstallationSecretAuth extends Auth {
        String org();

        String name();

        @Nullable
        String password();
    }

}
