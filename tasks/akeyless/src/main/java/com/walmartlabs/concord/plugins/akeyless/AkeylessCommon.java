package com.walmartlabs.concord.plugins.akeyless;

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

import com.walmartlabs.concord.plugins.akeyless.api.V2Api;
import com.walmartlabs.concord.plugins.akeyless.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;

public class AkeylessCommon {
    private static final Logger log = LoggerFactory.getLogger(AkeylessCommon.class);

    public AkeylessCommon() {
        // empty default constructor
    }

    public AkeylessTaskResult execute(TaskParams params) {

        switch (params.action()) {
            case GETSECRET: {
                return getSecret(params);
            }
            default:
                throw new IllegalArgumentException("Invalid action: " + params.action());
        }


    }

    public static AkeylessTaskResult getSecret(TaskParams params) {
        ApiClient client = Configuration.getDefaultApiClient();
        client.setBasePath(params.apiBasePath());
        V2Api api = new V2Api(client);

        Auth authBody = new Auth();
        authBody.setAccessId(params.accessId());
        authBody.setAccessKey(params.accessKey());

        AkeylessTaskResult result;

        try {
            AuthOutput authResult = api.auth(authBody);

            GetSecretValue getBody = new GetSecretValue();
            getBody.setToken(authResult.getToken());
            getBody.addNamesItem(params.secretPath());
            Map<String, String> secretData =  api.getSecretValue(getBody);

            result = AkeylessTaskResult.of(true, secretData, null);
        } catch (Exception e) {
            log.error("Error executing api call", e);
            result = AkeylessTaskResult.of(false, Collections.emptyMap(), e.getMessage());
        }

        return result;
    }
}
