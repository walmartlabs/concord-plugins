package com.walmartlabs.concord.plugins.argocd;

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

import okhttp3.*;

import java.io.IOException;

import static com.walmartlabs.concord.plugins.argocd.ArgoCdClient.exception;

public class LdapAuthHandler {

    public static TokenCookieJar auth(ArgoCdClient apiClient, TaskParams.LdapAuth in) throws IOException {
        HttpUrl url = apiClient.urlBuilder("auth/login")
                .addQueryParameter("connector_id", in.connectorId())
                .build();

        Request initRequest = new Request.Builder()
                .url(url)
                .get()
                .build();

        TokenCookieJar cookieJar = new TokenCookieJar();
        OkHttpClient client = apiClient.newBuilder()
                .cookieJar(cookieJar)
                .build();

        try (Response response = client.newCall(initRequest).execute()) {
            if (!response.isSuccessful()) {
                throw exception(initRequest, response.code(), response.message());
            }

            RequestBody authRequestBody = new FormBody.Builder()
                    .add("login", in.username())
                    .add("password", in.password())
                    .build();

            Request authRequest = new Request.Builder()
                    .url(response.request().url())
                    .post(authRequestBody)
                    .build();

            try (Response authResponse = client.newCall(authRequest).execute()) {
                if (!authResponse.isSuccessful()) {
                    throw exception(initRequest, authResponse.code(), authResponse.message());
                }
            }
        }

        if (cookieJar.token() == null) {
            throw new RuntimeException("Invalid username or password");
        }

        return cookieJar;
    }
}
