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

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class BasicAuthHandler {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String auth(String baseUrl, TaskParams.BasicAuth auth) throws IOException {
        Map<String, Object> body = new HashMap<>();
        body.put("username", auth.username());
        body.put("password", auth.password());

        HttpClient httpClient = HttpClientBuilder.create().build();
        String url = baseUrl + "api/v1/session";
        HttpUriRequest request = RequestBuilder.post(url)
                .setEntity(new StringEntity(objectMapper.writeValueAsString(body), ContentType.APPLICATION_JSON)).build();
        HttpResponse response = httpClient.execute(request);
        Map<String, Object> m = objectMapper.readMap(response.getEntity().getContent());
        return (String)m.get("token");
    }
}
