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

@Value.Immutable
@Value.Style(jdkOnly = true)
@JsonDeserialize(as = ImmutableAuth.class)
public interface Auth {

    @Nullable
    String accessToken();
    @Nullable
    AppInstallationAuth appInstallation();
    @Nullable
    AppInstallationSecretAuth appInstallationSecret();

    static ImmutableAuth.Builder builder() {
        return ImmutableAuth.builder();
    }

    @Value.Immutable
    @Value.Style(jdkOnly = true)
    @JsonDeserialize(as = ImmutableAppInstallationAuth.class)
    interface AppInstallationAuth {
        String privateKey();

        String clientId();

        @Value.Default
        default long refreshBufferSeconds() {
            return 60;
        }

        static ImmutableAppInstallationAuth.Builder builder() {
            return ImmutableAppInstallationAuth.builder();
        }
    }

    @Value.Immutable
    @Value.Style(jdkOnly = true)
    @JsonDeserialize(as = ImmutableAppInstallationSecretAuth.class)
    interface AppInstallationSecretAuth {
        String org();

        String name();

        @Nullable
        String password();

        static ImmutableAppInstallationSecretAuth.Builder builder() {
            return ImmutableAppInstallationSecretAuth.builder();
        }
    }

}
