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
import com.walmartlabs.concord.plugins.akeyless.model.auth.ApiKeyAuth;
import com.walmartlabs.concord.plugins.akeyless.model.auth.LdapAuth;
import com.walmartlabs.concord.runtime.v2.sdk.MapBackedVariables;
import com.walmartlabs.concord.runtime.v2.sdk.Variables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

public class AkeylessCommon {
    private static final Logger log = LoggerFactory.getLogger(AkeylessCommon.class);
    private TaskParams params;
    private ApiClient apiClient;
    private SecretExporter secretExporter;
    private static final Map<String, BiFunction<Variables, SecretExporter, Auth>> authBuilders = createAuthBuilders();

    public AkeylessCommon() {
        // empty default constructor
    }

    private static Map<String, BiFunction<Variables, SecretExporter, Auth>> createAuthBuilders() {
        Map<String, BiFunction<Variables, SecretExporter, Auth>> result = new HashMap<>();
        result.put("apiKey", ApiKeyAuth::of);
        result.put("ldap", LdapAuth::of);
        return result;
    }

    public AkeylessTaskResult execute(TaskParams params, SecretExporter secretExporter) {
        this.params = params;
        this.secretExporter = secretExporter;

        if (params.enableConcordSecretCache()) {
            secretExporter.initCache(params.sessionId(), params.debug());
        }

        Util.debug(params.debug(), log, String.format("Action: %s", params.action()));

        switch (params.action()) {
            case AUTH: {
                return auth(params);
            }
            case GETSECRET: {
                return getSecret((TaskParams.GetSecretParams) params);
            }
            case GETSECRETS: {
                return getSecrets((TaskParams.GetSecretsParams) params);
            }
            case CREATESECRET: {
                return createSecret((TaskParams.CreateSecretParams) params);
            }
            case UPDATESECRET: {
                return updateSecretVal((TaskParams.UpdateSecretParams) params);
            }
            case DELETEITEM: {
                return deleteItem((TaskParams.DeleteItemParams) params);
            }
            default:
                throw new IllegalArgumentException("Invalid action: " + params.action());
        }
    }

    private V2Api getApi(TaskParams params) {
        if (apiClient == null) {
            apiClient = new ApiClient()
                    .setBasePath(params.apiBasePath())
                    .setConnectTimeout(params.connectTimeout());
        }

        return new V2Api(apiClient);
    }

    private AkeylessTaskResult auth(TaskParams params) {
        V2Api api = getApi(params);

        try {
            AuthOutput authOutput = authenticate(api);
            Map<String, String> data = Collections.singletonMap("accessToken", authOutput.getToken());
            return AkeylessTaskResult.of(true, data, null);
        } catch (ApiException e) {
            log.error("Error authenticating with akeyless server", e);
            throw new RuntimeException(e);
        }
    }

    private Map<String, String> getSecrets(TaskParams params, List<String> paths) {
        V2Api api = getApi(params);

        try {
            String accessToken = getAccessToken(api);
            GetSecretValue body = new GetSecretValue()
                    .ignoreCache(params.ignoreCache() ? "true" : "false")
                    .token(accessToken);

            for (String path : paths) {
                body.addNamesItem(path);
            }

            return api.getSecretValue(body);
        } catch (Exception e) {
            log.error("Error fetching akeyless secret data", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets data for a single secret
     * @param params
     * @return task result containing data for a single secret
     */
    private AkeylessTaskResult getSecret(TaskParams.GetSecretParams params) {
        Util.debug(params.debug(), log, "getting secret data for: " + params.path());

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
        Util.debug(params.debug(), log, "getting akeyless secret data for: " + params.paths());

        Map<String, String> secretData = getSecrets(params, params.paths());

        return new AkeylessTaskResult(true, secretData, null);
    }

    private AkeylessTaskResult createSecret(TaskParams.CreateSecretParams params) {

        try {
            V2Api api = getApi(params);
            String accessToken = getAccessToken(api);

            api.createSecret(new CreateSecret()
                    .token(accessToken)
                    .name(params.path())
                    .value(params.value())
                    .metadata(params.description())
                    .multilineValue(params.multiline())
                    .protectionKey(params.protectionKey())
                    .tags(params.tags()));

            return AkeylessTaskResult.of(true, null, null);
        } catch (Exception e) {
            log.error("Error creating akeyless secret", e);
            throw new RuntimeException(e);
        }
    }

    private AkeylessTaskResult updateSecretVal(TaskParams.UpdateSecretParams params) {

        try {
            V2Api api = getApi(params);
            String accessToken = getAccessToken(api);

            api.updateSecretVal(new UpdateSecretVal()
                    .token(accessToken)
                    .value(params.value())
                    .name(params.path())
                    .multiline(params.multiline())
                    .key(params.protectionKey())
                    .keepPrevVersion(Boolean.toString(params.keepPreviousVersion())));

            return AkeylessTaskResult.of(true, null, null);
        } catch (Exception e) {
            log.error("Error updating akeyless secret", e);
            throw new RuntimeException(e);
        }
    }

    private AkeylessTaskResult deleteItem(TaskParams.DeleteItemParams params) {
        Util.debug(params.debug(), log, "deleting item: " + params.path());

        try {
            V2Api api = getApi(params);
            String accessToken = getAccessToken(api);

            api.deleteItem(new DeleteItem()
                    .token(accessToken)
                    .name(params.path())
                    .version(params.version())
                    .deleteImmediately(params.deleteImmediately())
                    .deleteInDays(params.deleteInDays()));

            return AkeylessTaskResult.of(true, null, null);
        } catch (Exception e) {
            log.error("Error deleting akeyless item", e);
            throw new RuntimeException(e);
        }
    }

    private String getAccessToken(V2Api api) throws ApiException {
        String accessToken = Util.stringOrSecret(params.accessToken(), secretExporter);

        if (accessToken != null) {
            Util.debug(params.debug(), log, "Using provided accessToken: ****");
            return accessToken;
        }

        Util.debug(params.debug(), log, "Using 'auth' param to retrieve an access token.");

        return authenticate(api).getToken();
    }

    private AuthOutput authenticate(V2Api api) throws ApiException {
        Map<String, Object> authParams = params.auth();

        if (authParams.isEmpty()) {
            throw new IllegalArgumentException("Empty auth");
        }

        if (authParams.size() != 1) {
            throw new IllegalArgumentException("Multiple auth types defined. Only one auth definition is allowed.");
        }

        String authType = authParams.keySet().iterator().next();

        BiFunction<Variables, SecretExporter, Auth> builder = authBuilders.get(authType);
        if (builder == null) {
            throw new IllegalArgumentException("Unknown auth type '" + authType + "'. Available: " + authBuilders.keySet());
        }

        Map<String, Object> authTypeParams = new MapBackedVariables(authParams).assertMap(authType);
        Auth auth = builder.apply(new MapBackedVariables(authTypeParams), secretExporter);

        return api.auth(auth);
    }
}
