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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

@Disabled()
public class TelemetryExporterTest {

    @Test
    public void test() {
        TelemetryExporter exporter = new TelemetryExporter("http://localhost:64318/v1/traces");

        FlowSteps steps = new FlowSteps(UUID.randomUUID(), "fake-test-entrypoint", System.currentTimeMillis(), List.of());

        steps.onStepStart(StepId.from(UUID.randomUUID()), "test-step", null, "test.concord.yaml", 1, "my-test-flow");

        String traceId = exporter.sendTelemetry(steps, true);

        System.out.println(traceId);
    }
}
