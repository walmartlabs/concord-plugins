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

@JsonInclude(Include.NON_NULL)
public class PlanResult implements Serializable {

    public static PlanResult noChanges(String output, String planPath) {
        return new PlanResult(true, false, output, planPath, null);
    }

    public static PlanResult hasChanges(String output, String planPath) {
        return new PlanResult(true, true, output, planPath, null);
    }

    public static PlanResult error(String error) {
        return new PlanResult(false, false, null, null, error);
    }

    private final boolean ok;
    private final boolean hasChanges;
    private final String output;
    private final String planPath;
    private final String error;

    public PlanResult(boolean ok, boolean hasChanges, String output, String planPath, String error) {
        this.ok = ok;
        this.hasChanges = hasChanges;
        this.output = output;
        this.planPath = planPath;
        this.error = error;
    }

    public boolean isOk() {
        return ok;
    }

    public boolean isHasChanges() {
        return hasChanges;
    }

    public String getOutput() {
        return output;
    }

    public String getPlanPath() {
        return planPath;
    }

    public String getError() {
        return error;
    }
}
