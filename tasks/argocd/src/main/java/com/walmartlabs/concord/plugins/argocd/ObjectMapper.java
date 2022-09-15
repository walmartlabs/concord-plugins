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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.walmartlabs.concord.plugins.argocd.model.Application;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class ObjectMapper {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<Map<String, Object>>() {
    };

    private final com.fasterxml.jackson.databind.ObjectMapper delegate;

    public ObjectMapper() {
        this.delegate = objectMapper();
    }

    private static com.fasterxml.jackson.databind.ObjectMapper objectMapper() {
        com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        om.registerModule(new Jdk8Module());
        om.registerModule(new JavaTimeModule());
        return om;
    }

    public <T> T readValue(String in, Class<T> clazz) throws IOException {
        return delegate.readValue(in, clazz);
    }

    public <T> T readValue(InputStream in, Class<T> clazz) throws IOException {
        return delegate.readValue(in, clazz);
    }

    public String writeValueAsString(Object value) throws JsonProcessingException {
        return delegate.writeValueAsString(value);
    }

    public Map<String, Object> readMap(InputStream in) throws IOException {
        return delegate.readValue(in, MAP_TYPE);
    }

    public Map<String, Object> toMap(Application app) {
        return delegate.convertValue(app, ObjectMapper.MAP_TYPE);
    }
}
