package com.walmartlabs.concord.plugins.argocd.model;

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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Value.Immutable
@Value.Style(jdkOnly = true)
@JsonSerialize(as = ImmutableApplication.class)
@JsonDeserialize(as = ImmutableApplication.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public interface Application {

    @Value.Immutable
    @Value.Style(jdkOnly = true)
    @JsonSerialize(as = ImmutableOperation.class)
    @JsonDeserialize(as = ImmutableOperation.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    interface Operation {

        @Value.Default
        default List<Object> info() {
            return Collections.emptyList();
        }

        @Value.Default
        default Map<String, Object> initiatedBy() {
            return Collections.emptyMap();
        }

        @Value.Default
        default Map<String, Object> retry() {
            return Collections.emptyMap();
        }

        @Value.Default
        default Map<String, Object> sync() {
            return Collections.emptyMap();
        }

        @Value.Default
        @JsonIgnore
        default boolean dryRun() {
            Object v = sync().get("dryRun");
            return Boolean.TRUE.equals(v);
        }
    }

    @Value.Immutable
    @Value.Style(jdkOnly = true)
    @JsonSerialize(as = ImmutableStatus.class)
    @JsonDeserialize(as = ImmutableStatus.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    interface Status {

        @Value.Default
        default List<Object> conditions() {
            return Collections.emptyList();
        }

        Health health();

        @Value.Default
        default List<Object> history() {
            return Collections.emptyList();
        }

        @Nullable
        OffsetDateTime observedAt();

        @Nullable
        OperationState operationState();

        @Nullable
        OffsetDateTime reconciledAt();

        @Value.Default
        default List<Object> resources() {
            return Collections.emptyList();
        }

        @Nullable
        String sourceType();

        @Value.Default
        default Map<String, Object> summary() {
            return Collections.emptyMap();
        }

        Sync sync();
    }

    @Value.Immutable
    @Value.Style(jdkOnly = true)
    @JsonSerialize(as = ImmutableHealth.class)
    @JsonDeserialize(as = ImmutableHealth.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    interface Health {

        @Nullable
        String message();

        @Nullable
        String status();
    }

    @Value.Immutable
    @Value.Style(jdkOnly = true)
    @JsonSerialize(as = ImmutableSync.class)
    @JsonDeserialize(as = ImmutableSync.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    interface Sync {

        @Value.Default
        default Map<String, Object> compareTo() {
            return Collections.emptyMap();
        }

        @Nullable
        String revision();

        String status();
    }

    @Value.Immutable
    @Value.Style(jdkOnly = true)
    @JsonSerialize(as = ImmutableOperationState.class)
    @JsonDeserialize(as = ImmutableOperationState.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    interface OperationState {

        @Nullable
        OffsetDateTime finishedAt();

        @Nullable
        String message();

        @Nullable
        Operation operation();

        @Nullable
        String phase();

        @Nullable
        String retryCount();

        @Nullable
        OffsetDateTime startedAt();

        @Value.Default
        default Map<String, Object> syncResult() {
            return Collections.emptyMap();
        }
    }

    @Value.Default
    default Map<String, Object> metadata() {
        return Collections.emptyMap();
    }

    @Value.Default
    default  Map<String, Object> spec() {
        return Collections.emptyMap();
    }

    @Nullable
    Status status();

    @Nullable
    Operation operation();

    @JsonIgnore
    default String resourceVersion() {
        return (String) metadata().get("resourceVersion");
    }
}
