package com.walmartlabs.concord.plugins.argocd.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.Collections;
import java.util.Map;

@Value.Immutable
@Value.Style(jdkOnly = true)
@JsonSerialize(as = ImmutableProject.class)
@JsonDeserialize(as = ImmutableProject.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public interface Project {

    @Value.Default
    default Map<String, Object> metadata() {
        return Collections.emptyMap();
    }

    @Value.Default
    default  Map<String, Object> spec() {
        return Collections.emptyMap();
    }

    @Value.Default
    default Map<String, Object> status() {
        return Collections.emptyMap();
    }


}