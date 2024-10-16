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

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.IdGenerator;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class TelemetryExporter {

    private final String endpoint;

    public TelemetryExporter(String endpoint) {
        this.endpoint = endpoint;
    }

    public String sendTelemetry(FlowSteps flowSteps, boolean isProcessFinishedOk) {
        Resource resource = Resource.create(Attributes.of(
                AttributeKey.stringKey("service.name"), "Concord",
                AttributeKey.stringKey("flowName"), flowSteps.flowName(),
                AttributeKey.stringKey("processId"), flowSteps.instanceId().toString())
        );

        OtlpHttpSpanExporter exporter = OtlpHttpSpanExporter.builder()
                .setEndpoint(endpoint)
                .build();

        try (BatchSpanProcessor processor = BatchSpanProcessor.builder(exporter).build()) {

            String traceId = IdGenerator.random().generateTraceId();

            String entryPoint = flowSteps.flowName();
            UUID processId = flowSteps.instanceId();
            long processEnds = System.currentTimeMillis();

            ConcordSpan processSpan = ConcordSpanBuilder.builder(traceId, resource, entryPoint)
                    .setStartTimestamp(flowSteps.startedAt(), TimeUnit.MILLISECONDS)
                    .setAttribute("processId", processId.toString())
                    .setStatus(isProcessFinishedOk ? StatusCode.OK : StatusCode.ERROR)
                    .end(processEnds)
                    .build();

            Map<StepId, String> correlationIdToSpanId = new HashMap<>();

            for (StepInfo s : flowSteps.steps()) {
                correlationIdToSpanId.put(s.id(), IdGenerator.random().generateSpanId());
            }

            for (StepInfo s : flowSteps.steps()) {
                ConcordSpan span = ConcordSpanBuilder.builder(traceId, resource, correlationIdToSpanId.get(s.id()), s.name())
                        .setParentSpanId(s.parentId() == null ? processSpan.getSpanContext().getSpanId() : correlationIdToSpanId.get(s.parentId()))
                        .setStartTimestamp(s.startedAt(), TimeUnit.MILLISECONDS)
                        .setAttribute("filename", s.filename())
                        .setAttribute("lineNum", s.lineNum())
                        .setAttribute("flowName", s.flowName())
                        .setStatus(s.success() ? StatusCode.OK : StatusCode.ERROR)
                        .end(s.endedAt() == null ? processEnds : s.endedAt())
                        .build();

                processor.onEnd(span);
            }

            processor.onEnd(processSpan);

            return traceId;
        }
    }
}
