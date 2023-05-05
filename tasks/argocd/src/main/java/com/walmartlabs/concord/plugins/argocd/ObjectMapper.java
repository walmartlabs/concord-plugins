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
import com.walmartlabs.concord.plugins.argocd.openapi.model.V1alpha1Application;
import com.walmartlabs.concord.plugins.argocd.openapi.model.V1alpha1ApplicationSet;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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

    public Map<String, Object> toMap(Object app) {
        return delegate.convertValue(app, ObjectMapper.MAP_TYPE);
    }

    public <T> T mapToModel(Map<String, Object> map, Class<T> model) throws IOException {
        return (T) readValue(writeValueAsString(map), model);
    }


    public V1alpha1Application buildApplicationObject(TaskParams.CreateUpdateParams in) throws IOException {
        Map<String, Object> metadata = new HashMap<>();
        Map<String, Object> spec = new HashMap<>();
        Map<String, Object> source = new HashMap<>();
        Map<String, Object> helm = new HashMap<>();
        Map<String, Object> destination = new HashMap<>();
        Map<String, Object> body = new HashMap<>();

        metadata.put("name", in.app());
        metadata.put("namespace", ArgoCdConstants.ARGOCD_NAMESPACE);
        metadata.put("finalizers", ArgoCdConstants.FINALIZERS);

        if (in.annotations() != null) {
            metadata.put("annotations", in.annotations());
        }

        destination.put("namespace", in.namespace());
        destination.put("name", in.cluster());

        if (in.gitRepo() != null) {
            source.put("repoURL", Objects.requireNonNull(in.gitRepo()).repoUrl());
            source.put("path", Objects.requireNonNull(in.gitRepo()).path());
            source.put("targetRevision", Objects.requireNonNull(in.gitRepo()).targetRevision());
        } else if (in.helmRepo() != null) {
            source.put("repoUrl", Objects.requireNonNull(in.helmRepo()).repoUrl());
            source.put("chart", Objects.requireNonNull(in.helmRepo()).chart());
            source.put("targetRevision", Objects.requireNonNull(in.helmRepo()).targetRevision());
        } else {
            throw new RuntimeException("Source information not provided for " + in.app() + "." +
                    "Provide either `gitRepo` or `helmRepo` details for the application to be created." +
                    "Cannot proceed further. Refer docs (https://concord.walmartlabs.com/docs/plugins-v2/argocd.html#usage) for usage");
        }

        if (in.helm() != null) {
            if (Objects.requireNonNull(in.helm()).parameters() != null)
                helm.put("parameters", Objects.requireNonNull(in.helm()).parameters());

            helm.put("values", Objects.requireNonNull(in.helm()).values());
            source.put("helm", helm);
        }
        spec.put("project", in.project());
        spec.put("destination", destination);
        spec.put("source", source);

        if (in.createNamespace()) {
            Map<String, Object> syncPolicy = new HashMap<>(ArgoCdConstants.SYNC_POLICY);
            syncPolicy.put("syncOptions", ArgoCdConstants.CREATE_NAMESPACE_OPTION);
            spec.put("syncPolicy", syncPolicy);
        } else {
            spec.put("syncPolicy", ArgoCdConstants.SYNC_POLICY);
        }

        body.put("metadata", metadata);
        body.put("spec", spec);

        return mapToModel(body, V1alpha1Application.class);
    }

    public V1alpha1ApplicationSet buildApplicationSetObject(TaskParams.CreateUpdateApplicationSetParams in) throws IOException {
        V1alpha1Application application = buildApplicationObject(in);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("name", in.applicationSet());
        metadata.put("namespace", in.applicationSetNamespace());
        Map<String, Object> applicationSetMap = new HashMap<>();
        applicationSetMap.put("metadata", metadata);
        Map<String, Object> spec = new HashMap<>();
        spec.put("generators", in.generators());
        Map<String,Object> syncPolicy = new HashMap<>();
        syncPolicy.put("preserveResourcesOnDeletion", in.preserveResourcesOnDeletion());
        spec.put("syncPolicy",syncPolicy);
        spec.put("strategy", in.strategy());
        spec.put("template", application);
        applicationSetMap.put("spec", spec);
        return mapToModel(applicationSetMap, V1alpha1ApplicationSet.class);
    }
}
