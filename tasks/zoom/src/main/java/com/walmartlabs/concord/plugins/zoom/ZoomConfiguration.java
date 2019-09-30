package com.walmartlabs.concord.plugins.zoom;

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

public class ZoomConfiguration {

    @SuppressWarnings("unchecked")
    public static ZoomConfiguration from(Context ctx) {
        Map<String, Object> zoomParams = (Map<String, Object>) ctx.getVariable(Constants.VAR_ZOOM_PARAMS);


        return from(getString(zoomParams, Constants.VAR_CLIENT_ID), getString(zoomParams, Constants.VAR_CLIENT_SECRET),
                getString(zoomParams, Constants.VAR_ACCOUNT_ID), getString(zoomParams, Constants.VAR_ROBOT_ID),
                getString(zoomParams, Constants.VAR_ROOT_API), getString(zoomParams, Constants.VAR_ACCESS_TOKEN_API),
                getString(zoomParams, Constants.VAR_PROXY_ADDRESS), getInt(zoomParams, Constants.VAR_PROXY_PORT, Constants.DEFAULT_PROXY_PORT),
                getInt(zoomParams, Constants.VAR_CONNECTION_TIMEOUT, Constants.DEFAULT_CONNECT_TIMEOUT),
                getInt(zoomParams, Constants.VAR_SO_TIMEOUT, Constants.DEFAULT_SO_TIMEOUT),
                getInt(zoomParams, Constants.VAR_RETRY_COUNT, Constants.DEFAULT_RETRY_COUNT));
    }


    public static ZoomConfiguration from(String clientId, String clientSecret, String accountId,
                                         String robotJid, String rootApi, String accessTokenApi,
                                         String proxyAddress, int proxyPort, int connectTimeout, int soTimeout, int retryCount) {

        return new ZoomConfiguration(clientId, clientSecret, accountId,
                robotJid, rootApi, accessTokenApi, proxyAddress, proxyPort, connectTimeout, soTimeout, retryCount);
    }

    private final String clientId;
    private final String clientSecret;
    private final String accountId;
    private final String robotJid;
    private final String rootApi;
    private final String accessTokenApi;

    private final String proxyAddress;
    private final int proxyPort;
    private final int connectTimeout;
    private final int soTimeout;
    private final int retryCount;

    public ZoomConfiguration(String clientId, String clientSecret, String accountId, String robotJid,
                             String rootApi, String accessTokenApi, String proxyAddress, int proxyPort,
                             int connectTimeout, int soTimeout, int retryCount) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.accountId = accountId;
        this.robotJid = robotJid;
        this.rootApi = rootApi;
        this.accessTokenApi = accessTokenApi;
        this.proxyAddress = proxyAddress;
        this.proxyPort = proxyPort;
        this.connectTimeout = connectTimeout;
        this.soTimeout = soTimeout;
        this.retryCount = retryCount;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getRobotJid() {
        return robotJid;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getRootApi() {
        return rootApi;
    }

    public String getAccessTokenApi() {
        return accessTokenApi;
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
