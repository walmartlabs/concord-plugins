package com.walmartlabs.concord.plugins.zoom;

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

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Result implements Serializable {

    private static final long serialVersionUID = 1L;

    private final boolean ok;
    private final String error;
    private final Map<String, Object> params = new HashMap<>();
    public final String data;

    public Result(boolean ok, String error, String data) {
        this.ok = ok;
        this.error = error;
        this.data = data;
    }

    public boolean isOk() {
        return ok;
    }

    public String getError() {
        return error;
    }

    public String getData() {
        return data;
    }

    @JsonAnyGetter
    public Map<String, Object> getParams() {
        return params;
    }

    @JsonAnySetter
    public void setParams(String name, Object value) {
        params.put(name, value);
    }

    @Override
    public String toString() {
        return "Response{" +
                "ok=" + ok +
                ", error='" + error + '\'' +
                '}';
    }
}