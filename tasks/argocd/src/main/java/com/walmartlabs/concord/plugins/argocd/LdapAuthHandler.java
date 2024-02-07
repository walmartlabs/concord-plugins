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

import com.walmartlabs.concord.client2.ApiException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class LdapAuthHandler {

    public static String auth(HttpClientBuilder builder, String baseUrl, TaskParams.LdapAuth in) throws IOException, ApiException, URISyntaxException {
        CookieStore httpCookieStore = new BasicCookieStore();
        CloseableHttpClient httpClient = builder.setDefaultRequestConfig(RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build())
                .setDefaultCookieStore(httpCookieStore).build();

        URI url =  new URIBuilder(URI.create(baseUrl)).setPath("auth/login").addParameter("connector_id", in.connectorId()).build();
        RequestBuilder requestBuilder = RequestBuilder.get(url);
        HttpClientContext context = HttpClientContext.create();
        HttpResponse response = httpClient.execute(requestBuilder.build(), context);
        if(!isSuccess(response)) {
            throw new ApiException(response.getStatusLine().getStatusCode(), response.getStatusLine().getReasonPhrase());
        }
        EntityUtils.consumeQuietly(response.getEntity());

        String requestUri = context.getRequest().getRequestLine().getUri();
        requestUri = URLDecoder.decode(requestUri, StandardCharsets.UTF_8.name());
        URI loginUrl = new URIBuilder(URI.create(baseUrl + requestUri)).build();
        final MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
        entityBuilder.addTextBody("login", in.username());
        entityBuilder.addTextBody("password", in.password());
        final HttpEntity multipart = entityBuilder.build();
        HttpUriRequest request = RequestBuilder.post(loginUrl)
                .setEntity(multipart).build();
        response = httpClient.execute(request);

        String token = null;
        for (Cookie cookie: httpCookieStore.getCookies() ) {
            if(cookie.getName().equals("argocd.token") ) {
                 token = cookie.getValue();
            }
        }

        EntityUtils.consumeQuietly(response.getEntity());

        return token;
    }

    private static boolean isSuccess(HttpResponse response) {
        int code = response.getStatusLine().getStatusCode();
        return code >= 200 && code < 300;
    }
}
