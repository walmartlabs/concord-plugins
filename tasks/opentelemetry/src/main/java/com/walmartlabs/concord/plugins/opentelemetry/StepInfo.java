package com.walmartlabs.concord.plugins.opentelemetry;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2024 Walmart Inc., Concord Authors
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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.io.Serial;
import java.io.Serializable;

@Value.Immutable
@Value.Style(jdkOnly = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonSerialize(as = ImmutableStepInfo.class)
@JsonDeserialize(as = ImmutableStepInfo.class)
public interface StepInfo extends Serializable {

    @Serial
    long serialVersionUID = 1L;

    StepId id();

    String name();

    @Nullable
    StepId parentId();

    long startedAt();

    @Nullable
    Long endedAt();

    @Nullable
    String filename();

    int lineNum();

    @Nullable
    String flowName();

    @Value.Default
    default boolean success() {
        return false;
    }

    static ImmutableStepInfo.Builder builder() {
        return ImmutableStepInfo.builder();
    }
}
