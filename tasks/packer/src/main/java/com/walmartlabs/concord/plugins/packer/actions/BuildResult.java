package com.walmartlabs.concord.plugins.packer.actions;

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

import java.io.Serializable;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_EMPTY)
public class BuildResult implements Serializable {

    public static BuildResult ok(String output, Map<String, Object> data) {
        return new BuildResult(true, output, null, data);
    }

    public static BuildResult error(String error) {
        return new BuildResult(false, null, error, null);
    }

    private final boolean ok;
    private final String output;
    private final String error;
    private final Map<String, Object> data;

    public BuildResult(boolean ok, String output, String error, Map<String, Object> data) {
        this.ok = ok;
        this.output = output;
        this.error = error;
        this.data = data;
    }

    public boolean isOk() {
        return ok;
    }

    public String getOutput() {
        return output;
    }

    public String getError() {
        return error;
    }

    public Map<String, Object> getData() {
        return data;
    }
}
