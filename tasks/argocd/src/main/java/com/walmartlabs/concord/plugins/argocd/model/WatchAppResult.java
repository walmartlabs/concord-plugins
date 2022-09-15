/**
 * Concord
 * -----
 * Copyright (C) 2017 - 2021 Walmart Inc.
 * -----
 */
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
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;

@Value.Immutable
@Value.Style(jdkOnly = true)
@JsonSerialize(as = ImmutableWatchAppResult.class)
@JsonDeserialize(as = ImmutableWatchAppResult.class)
public interface WatchAppResult {

    @Value.Default
    default Map<String, Object> error() {
        return Collections.emptyMap();
    }

    @JsonIgnore
    default Application application() {
        Result result = result();
        if (result == null) {
            return null;
        }
        return result.application();
    }

    @Nullable
    Result result();

    @Value.Immutable
    @Value.Style(jdkOnly = true)
    @JsonSerialize(as = ImmutableResult.class)
    @JsonDeserialize(as = ImmutableResult.class)
    interface Result {

        @Nullable
        Application application();
    }
}
