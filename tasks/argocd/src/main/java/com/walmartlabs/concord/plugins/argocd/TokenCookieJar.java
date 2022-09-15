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

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

import java.util.Collections;
import java.util.List;

public class TokenCookieJar implements CookieJar {

    String token = null;

    List<Cookie> cookies = Collections.emptyList();

    @Override
    public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {

        this.cookies = cookies;

        for (Cookie c : cookies) {
            if ("argocd.token".equalsIgnoreCase(c.name())) {
                this.token = c.value();
            }
        }
    }

    @Override
    public List<Cookie> loadForRequest(HttpUrl url) {
        return this.cookies;
    }

    public String token() {
        return token;
    }

};
