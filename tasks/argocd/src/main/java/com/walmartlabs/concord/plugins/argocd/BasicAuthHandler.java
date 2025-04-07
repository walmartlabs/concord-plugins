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

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

public class BasicAuthHandler {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private BasicAuthHandler() { }

    public static String auth(HttpClient.Builder httpBuilder, String baseUrl, TaskParams.BasicAuth auth) throws IOException {
        Map<String, Object> body = new HashMap<>();
        body.put("username", auth.username());
        body.put("password", auth.password());

        var req = HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/session"))
                .POST(BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .header("Content-Type", "application/json")
                .build();

        try {
            var resp = httpBuilder.build().send(req, HttpResponse.BodyHandlers.ofInputStream());
            var m = objectMapper.readMap(resp.body());
            return (String) m.get("token");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        throw new IllegalStateException("Unexpected error retrieving token with basic auth");
    }
}
