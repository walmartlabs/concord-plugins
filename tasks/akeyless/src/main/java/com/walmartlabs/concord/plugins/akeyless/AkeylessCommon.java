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
import java.util.List;
import java.util.Map;

public class AkeylessCommon {
    private static final Logger log = LoggerFactory.getLogger(AkeylessCommon.class);
    private TaskParams params;

    public AkeylessCommon() {
        // empty default constructor
    }

    public AkeylessTaskResult execute(TaskParams params) {
        this.params = params;

        log.info("Action: {}", params.action());

        switch (params.action()) {
            case GETSECRET: {
                return getSecret((TaskParams.GetSecretParams) params);
            }
            case GETSECRETS: {
                return getSecrets((TaskParams.GetSecretsParams) params);
            }
            case UPDATESECRET: {
                return updateSecretVal((TaskParams.UpdateSecretParams) params);
            }
            default:
                throw new IllegalArgumentException("Invalid action: " + params.action());
        }
    }

    private static V2Api getApi(TaskParams params) {
        return new V2Api(Configuration.getDefaultApiClient()
                .setBasePath(params.apiBasePath())
                .setConnectTimeout(params.connectTimeout()));
    }

    private Map<String, String> getSecrets(TaskParams params, List<String> paths) {
        V2Api api = getApi(params);

        try {
            AuthOutput authResult = auth(api); // get auth token

            GetSecretValue body = new GetSecretValue()
                    .token(authResult.getToken());

            for (String path : paths) {
                body.addNamesItem(path);
            }
            return api.getSecretValue(body);

        } catch (Exception e) {
            log.error("Error fetching secret data", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets data for a single secret
     * @param params
     * @return task result containing data for a single secret
     */
    private AkeylessTaskResult getSecret(TaskParams.GetSecretParams params) {
        Map<String, String> secretData = getSecrets(params, Collections.singletonList(params.path()));

        if (secretData.size() != 1) {
            // very odd...we only asked for one secret
            log.warn("Multiple ({}) secret data returned.", secretData.size());
        }

        return new AkeylessTaskResult(true, secretData, null);
    }

    /**
     * Gets data for one or more secret paths
     * @param params
     * @return task result containing a map of paths to secret data
     */
    private AkeylessTaskResult getSecrets(TaskParams.GetSecretsParams params) {
        Map<String, String> secretData = getSecrets(params, params.paths());

        return new AkeylessTaskResult(true, secretData, null);
    }

    private AkeylessTaskResult updateSecretVal(TaskParams.UpdateSecretParams params) {

        try {
            V2Api api = getApi(params);
            AuthOutput auth = auth(api);

            UpdateSecretVal body = new UpdateSecretVal()
                    .token(auth.getToken())
                    .value(params.value())
                    .name(params.path())
                    .multiline(params.multiline())
                    .key(params.protectionKey()) // may be null
                    .keepPrevVersion(Boolean.toString(params.keepPreviousVersion()));

            UpdateSecretValOutput result = api.updateSecretVal(body);

            log.info("result {}", result);

            return AkeylessTaskResult.of(true, null, null);
        } catch (Exception e) {
            log.error("Error updating secret", e);
            throw new RuntimeException(e);
        }
    }

    private AuthOutput auth(V2Api api) throws ApiException {
        return api.auth(params.auth());
    }
}
