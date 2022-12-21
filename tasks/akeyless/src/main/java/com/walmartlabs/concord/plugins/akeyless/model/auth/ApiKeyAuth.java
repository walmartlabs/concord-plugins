package com.walmartlabs.concord.plugins.akeyless.model.auth;

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
import com.walmartlabs.concord.plugins.akeyless.model.Auth;
import com.walmartlabs.concord.runtime.v2.sdk.Variables;

import static com.walmartlabs.concord.plugins.akeyless.Util.stringOrSecret;

public class ApiKeyAuth {
    private static final String ACCESS_ID_KEY = "accessId";
    private static final String ACCESS_KEY_KEY = "accessKey";

    public static Auth of(Variables vars, SecretExporter secretExporter) {
        return new Auth()
                .accessId(stringOrSecret(vars.get(ACCESS_ID_KEY), secretExporter))
                .accessKey(stringOrSecret(vars.get(ACCESS_KEY_KEY), secretExporter));
    }

    private ApiKeyAuth() {
    }
}
