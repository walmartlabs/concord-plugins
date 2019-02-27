package com.walmartlabs.concord.plugins.terraform.actions;

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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import java.io.Serializable;

@JsonInclude(Include.NON_EMPTY)
public class ApplyResult implements Serializable {

    public static ApplyResult ok(String output) {
        return new ApplyResult(true, output, null);
    }

    public static ApplyResult error(String error) {
        return new ApplyResult(false, null, error);
    }

    private final boolean ok;
    private final String output;
    private final String error;

    public ApplyResult(boolean ok, String output, String error) {
        this.ok = ok;
        this.output = output;
        this.error = error;
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
}
