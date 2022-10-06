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

public enum HealthStatus {

    // Indicates that health assessment failed and actual health status is unknown
    UNKNOWN("Unknown"),
    // Progressing health status means that resource is not healthy but still have a chance to reach healthy state
    PROGRESSING("Progressing"),
    // Resource is 100% healthy
    HEALTHY("Healthy"),
    // Assigned to resources that are suspended or paused. The typical example is a
    // [suspended](https://kubernetes.io/docs/tasks/job/automated-tasks-with-cron-jobs/#suspend) CronJob.
    SUSPENDED("Suspended"),
    // Degrade status is used if resource status indicates failure or resource could not reach healthy state
    // within some timeout.
    DEGRADED("Degraded"),
    // Indicates that resource is missing in the cluster.
    MISSING("Missing");

    private final String value;

    HealthStatus(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
