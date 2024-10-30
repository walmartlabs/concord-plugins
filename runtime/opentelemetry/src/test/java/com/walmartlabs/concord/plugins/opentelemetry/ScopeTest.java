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
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled
public class ScopeTest {

    @Test
    public void test() {
        var resource = Resource.getDefault()
                .merge(Resource.create(Attributes.of(
                        AttributeKey.stringKey("service.name"), "Concord"
                )));

        var spanExporter = OtlpGrpcSpanExporter.builder()
                .setEndpoint("http://localhost:4317")
                .build();

        var spanProcessor = BatchSpanProcessor.builder(spanExporter).build();

        var tracerProvider = SdkTracerProvider.builder()
                .setResource(resource)
                .addSpanProcessor(spanProcessor)
                .build();

        var openTelemetry = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .build();

        var tracer = tracerProvider.tracerBuilder("proc").build();

        var processSpan = tracer.spanBuilder("process")
                .setAttribute("instanceId", "foo")
                .startSpan();
        var processScope = processSpan.makeCurrent();

        var nestedSpan = tracer.spanBuilder("nestedSpan")
                .setAttribute("nested", "true")
                .startSpan();
        var nestedScope = nestedSpan.makeCurrent();

        var nestedNestedSpan = tracer.spanBuilder("nestedNestedSpan")
                .setAttribute("nestedNested", "true")
                .startSpan();
        var nestedNestedScope = nestedNestedSpan.makeCurrent();

        nestedNestedScope.close();
        nestedNestedSpan.end();

        nestedScope.close();
        nestedSpan.end();

        processScope.close();
        processSpan.end();

        openTelemetry.close();
    }
}
