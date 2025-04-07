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

import com.walmartlabs.concord.plugins.argocd.openapi.ApiException;

import java.io.IOException;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

public class LdapAuthHandler {

    private LdapAuthHandler() { }

    public static String auth(HttpClient.Builder clientBuilder, String baseUrl, TaskParams.LdapAuth in) throws IOException, ApiException, URISyntaxException {
        var cookieManager = new CookieManager();
        clientBuilder.cookieHandler(cookieManager);

        var loginUri = URI.create(baseUrl + "/auth/login?connector_id=ldap");

        var req = HttpRequest.newBuilder(loginUri)
                .GET()
                .build();

        try {
            // this will redirect, hopefully client is configured to do so
            var resp = clientBuilder.build().send(req, HttpResponse.BodyHandlers.ofString());
            if (!isSuccess(resp.statusCode())) {
                throw new ApiException(resp.statusCode(), resp.body());
            }

            // capture the redirect uri
            loginUri = resp.uri();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        var body = ArgoCdClient.toParameterString(Map.of("login", in.username(), "password", in.password()));

        req = HttpRequest.newBuilder(loginUri)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        try {
            var resp = clientBuilder.build().send(req, HttpResponse.BodyHandlers.ofString());
            if (!isSuccess(resp.statusCode())) {
                throw new ApiException(resp.statusCode(), resp.body());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String token = null;

        for (HttpCookie cookie : cookieManager.getCookieStore().getCookies()) {
            if (cookie.getName().equals("argocd.token")) {
                token = cookie.getValue();
            }
        }

//        for (Cookie cookie: httpCookieStore.getCookies() ) {
//            if(cookie.getName().equals("argocd.token") ) {
//                token = cookie.getValue();
//            }
//        }
//
//        EntityUtils.consumeQuietly(response.getEntity());

        return token;
    }

    private static boolean isSuccess(int code) {
        return code >= 200 && code < 300;
    }


}
