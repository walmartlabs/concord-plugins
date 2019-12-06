package com.walmartlabs.concord.plugins.msteams;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2018 Walmart Inc.
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

import com.walmartlabs.concord.sdk.Context;

import java.util.Map;

import static com.walmartlabs.concord.sdk.MapUtils.getInt;
import static com.walmartlabs.concord.sdk.MapUtils.getString;

public class TeamsConfiguration {

    @SuppressWarnings("unchecked")
    public static TeamsConfiguration from(Context ctx) {
        Map<String, Object> msteamsParams = (Map<String, Object>) ctx.getVariable(Constants.MSTEAMS_PARAMS_KEY);


        return from(getString(msteamsParams, Constants.TEAM_ID_KEY), getString(msteamsParams, Constants.TENANT_ID_KEY),
                getString(msteamsParams, Constants.WEBHOOKTYPE_ID_KEY), getString(msteamsParams, Constants.WEBHOOK_ID_KEY), getString(msteamsParams, Constants.ROOT_WEBHOOK_URL_KEY),
                getString(msteamsParams, Constants.PROXY_ADDRESS_KEY), getInt(msteamsParams, Constants.PROXY_PORT_KEY, Constants.DEFAULT_PROXY_PORT),
                getInt(msteamsParams, Constants.CONNECTION_TIMEOUT_KEY, Constants.DEFAULT_CONNECT_TIMEOUT),
                getInt(msteamsParams, Constants.SO_TIMEOUT_KEY, Constants.DEFAULT_SO_TIMEOUT),
                getInt(msteamsParams, Constants.RETRY_COUNT_KEY, Constants.DEFAULT_RETRY_COUNT));
    }


    public static TeamsConfiguration from(String teamId, String tenantId, String webhookTypeId, String webhookId, String rootWebhookUrl,
                                          String proxyAddress, int proxyPort, int connectTimeout, int soTimeout, int retryCount) {

        return new TeamsConfiguration(teamId, tenantId, webhookTypeId, webhookId, rootWebhookUrl, proxyAddress, proxyPort, connectTimeout, soTimeout, retryCount);
    }

    private final String teamId;
    private final String tenantId;
    private final String webhookTypeId;
    private final String webhookId;
    private final String rootWebhookUrl;


    private final String proxyAddress;
    private final int proxyPort;
    private final int connectTimeout;
    private final int soTimeout;
    private final int retryCount;

    public TeamsConfiguration(String teamId, String tenantId, String webhookTypeId, String webhookId, String rootWebhookUrl, String proxyAddress, int proxyPort,
                              int connectTimeout, int soTimeout, int retryCount) {
        this.teamId = teamId;
        this.tenantId = tenantId;
        this.webhookTypeId = webhookTypeId;
        this.webhookId = webhookId;
        this.rootWebhookUrl = rootWebhookUrl;
        this.proxyAddress = proxyAddress;
        this.proxyPort = proxyPort;
        this.connectTimeout = connectTimeout;
        this.soTimeout = soTimeout;
        this.retryCount = retryCount;
    }

    public String getWebhookTypeId() {
        return webhookTypeId;
    }

    public String getWebhookId() {
        return webhookId;
    }

    public String getRootWebhookUrl() {
        return rootWebhookUrl;
    }

    public String getTeamId() {
        return teamId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getProxyAddress() {
        return proxyAddress;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public int getSoTimeout() {
        return soTimeout;
    }

    public int getRetryCount() {
        return retryCount;
    }
}
