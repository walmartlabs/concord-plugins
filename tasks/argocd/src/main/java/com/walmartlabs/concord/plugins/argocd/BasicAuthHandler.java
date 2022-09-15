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

import okhttp3.Request;
import okhttp3.RequestBody;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.walmartlabs.concord.plugins.argocd.ArgoCdClient.APPLICATION_JSON;

public class BasicAuthHandler {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String auth(ArgoCdClient apiClient, TaskParams.BasicAuth auth) throws IOException {
        Map<String, Object> body = new HashMap<>();
        body.put("username", auth.username());
        body.put("password", auth.password());

        Request.Builder rb = new Request.Builder()
                .url(apiClient.urlBuilder("api/v1/session").build())
                .post(RequestBody.create(APPLICATION_JSON, objectMapper.writeValueAsString(body)));

        return apiClient.exec(rb.build(), (response) -> {
            Map<String, Object> m = objectMapper.readMap(response.byteStream());
            return (String)m.get("token");
        });
    }
}
