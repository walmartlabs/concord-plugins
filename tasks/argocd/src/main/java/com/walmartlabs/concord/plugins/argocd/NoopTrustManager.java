package com.walmartlabs.concord.plugins.argocd;

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


import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;
import java.net.Socket;
import java.security.cert.X509Certificate;

/**
 * Trust manager which trusts everything. Not recommended for production usage,
 * but helpful with test environments.
 */
public class NoopTrustManager extends X509ExtendedTrustManager {
    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) {
        // no validation
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) {
        // no validation
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) {
        // no validation
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) {
        // no validation
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {
        // no validation
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {
        // no validation
    }

    public static TrustManager[] getManagers() {
        return new TrustManager[] { new NoopTrustManager() };
    }
}
