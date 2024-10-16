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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class FlowSteps implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final UUID instanceId;
    private final String flowName;
    private final long startedAt;
    private final List<StepInfo> steps;

    @JsonIgnore
    private final Map<StepId, Integer> stepIndex;

    @JsonCreator
    public FlowSteps(@JsonProperty("instanceId") UUID instanceId,
                     @JsonProperty("flowName") String flowName,
                     @JsonProperty("startedAt") long startedAt,
                     @JsonProperty("steps") List<StepInfo> steps) {

        this.instanceId = instanceId;
        this.flowName = flowName;
        this.startedAt = startedAt;
        this.steps = new ArrayList<>(steps != null ? steps : List.of());
        this.stepIndex = IntStream.range(0, this.steps.size())
                .boxed()
                .collect(Collectors.toMap(
                        i -> this.steps.get(i).id(),
                        i -> i
                ));
    }

    public void onStepStart(StepId stepId, String name, StepId parentId,
                            String filename, int lineNum, String flowName) {
        StepInfo step = StepInfo.builder()
                .id(stepId)
                .name(name)
                .parentId(parentId)
                .startedAt(System.currentTimeMillis())
                .filename(filename)
                .lineNum(lineNum)
                .flowName(flowName)
                .build();

        synchronized (this) {
            steps.add(step);
            stepIndex.put(stepId, steps.size() - 1);
        }
    }

    public boolean onStepEnd(StepId stepId, boolean success) {
        synchronized (this) {
            Integer index = stepIndex.get(stepId);
            if (index == null) {
                return false;
            }

            steps.set(index, StepInfo.builder().from(steps.get(index))
                    .endedAt(System.currentTimeMillis())
                    .success(success)
                    .build());
        }

        return true;
    }

    @JsonProperty("steps")
    public List<StepInfo> steps() {
        return steps;
    }

    @JsonProperty("instanceId")
    public UUID instanceId() {
        return instanceId;
    }

    @JsonProperty("startedAt")
    public long startedAt() {
        return startedAt;
    }

    @JsonProperty("flowName")
    public String flowName() {
        return flowName;
    }
}
