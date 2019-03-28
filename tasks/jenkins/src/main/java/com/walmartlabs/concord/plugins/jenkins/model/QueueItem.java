package com.walmartlabs.concord.plugins.jenkins.model;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2019 Walmart Inc.
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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class QueueItem {

    private final String why;

    private final boolean cancelled;

    private final Executable executable;

    @JsonCreator
    public QueueItem(@JsonProperty("why") String why,
                     @JsonProperty("cancelled") boolean cancelled,
                     @JsonProperty("executable") Executable executable) {
        this.why = why;
        this.cancelled = cancelled;
        this.executable = executable;
    }

    public String getWhy() {
        return why;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public Executable getExecutable() {
        return executable;
    }
}
