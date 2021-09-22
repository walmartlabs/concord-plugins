package com.walmartlabs.concord.plugins.gremlin;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2021 Walmart Inc., Concord Authors
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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class AttackResult {

    protected final Gson gson = new GsonBuilder().create();

    private final String id;

    public AttackResult(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public String details(TaskParams.AttackParams in) {
        try {
            return gson.toJson(new GremlinClient(in)
                    .url("attacks/" + id)
                    .successCode(200)
                    .get());
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while getting attack details", e);
        }
    }
}
