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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FlowStepsSerializationTest {

    @Test
    public void test() throws Exception {
        FlowSteps flowStepsOrig = new FlowSteps(UUID.randomUUID(), "TestFlow", System.currentTimeMillis(), List.of());
        flowStepsOrig.onStepStart(StepId.from(UUID.randomUUID(), 123), "Step 1", null, "file1", 123, "flow1");

        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(flowStepsOrig);

        FlowSteps flowSteps = objectMapper.readValue(json, FlowSteps.class);

        assertEquals(flowStepsOrig.flowName(), flowSteps.flowName());
        assertEquals(flowStepsOrig.instanceId(), flowSteps.instanceId());
        assertEquals(flowStepsOrig.startedAt(), flowSteps.startedAt());
        assertEquals(1, flowSteps.steps().size());
        assertEquals(flowStepsOrig.steps(), flowSteps.steps());
    }
}
