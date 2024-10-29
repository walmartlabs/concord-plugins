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

import com.google.inject.Inject;
import com.walmartlabs.concord.runtime.common.injector.InstanceId;
import com.walmartlabs.concord.svm.Runtime;
import com.walmartlabs.concord.svm.*;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

public class TelemetryCollector2 implements ExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(TelemetryCollector2.class);

    private final InstanceId instanceId;
    private final Map<ThreadId, Deque<Span>> openSpans = new HashMap<>();
    private final Map<ScopeKey, Deque<Scope>> openScopes = new HashMap<>();

    private OtlpGrpcSpanExporter spanExporter;
    private OpenTelemetrySdk openTelemetry;
    private Tracer tracer;
    private Span processSpan;
    private Scope processScope;

    @Inject
    public TelemetryCollector2(InstanceId instanceId) {
        this.instanceId = instanceId;
    }

    @Override
    public void beforeProcessStart(Runtime runtime, State state) {
        var resource = Resource.getDefault()
                .merge(Resource.create(Attributes.of(
                        AttributeKey.stringKey("service.name"), "Concord"
                )));

        this.spanExporter = OtlpGrpcSpanExporter.builder()
                .setEndpoint("http://localhost:4317")
                .build();

        var spanProcessor = BatchSpanProcessor.builder(spanExporter).build();

        var tracerProvider = SdkTracerProvider.builder()
                .setResource(resource)
                .addSpanProcessor(spanProcessor)
                .build();

        this.openTelemetry = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .build();

        this.tracer = tracerProvider.tracerBuilder(instanceId.getValue().toString()).build();

        this.processSpan = tracer.spanBuilder("process")
                .setAttribute("instanceId", instanceId.getValue().toString())
                .startSpan();

        this.processScope = processSpan.makeCurrent();
    }

    @Override
    public Result beforeCommand(Runtime runtime, VM vm, State state, ThreadId threadId, Command cmd) {
        Span currentSpan = null;
        synchronized (openSpans) {
            var spans = openSpans.get(threadId);
            if (spans != null && !spans.isEmpty()) {
                currentSpan = spans.peek();
            }
        }

        if (currentSpan == null) {
            currentSpan = processSpan;
        }

        var currentScope = currentSpan.makeCurrent();


        // TODO attrs
        synchronized (openSpans) {
            var span = tracer.spanBuilder("command")
                    .setAttribute("command", cmd.toString())
                    .setAttribute("scopeDebug", currentScope.toString())
                    .startSpan();

            var scope = span.makeCurrent();
            synchronized (openScopes) {
                openScopes.computeIfAbsent(new ScopeKey(threadId, cmd), k -> new ArrayDeque<>()).push(scope);
            }

            openSpans.computeIfAbsent(threadId, k -> new ArrayDeque<>()).push(span);
        }

        return Result.CONTINUE;
    }

    @Override
    public Result afterCommand(Runtime runtime, VM vm, State state, ThreadId threadId, Command cmd) {
        synchronized (openSpans) {
            var spans = openSpans.get(threadId);
            if (spans != null && !spans.isEmpty()) {
                var span = spans.pop();
                span.end();

                if (spans.isEmpty()) {
                    openSpans.remove(threadId);
                }

                synchronized (openScopes) {
                    var scopes = openScopes.get(new ScopeKey(threadId, cmd));
                    if (scopes != null && !scopes.isEmpty()) {
                        var scope = scopes.pop();
                        scope.close();
                    }
                    if (scopes.isEmpty()) {
                        openScopes.remove(new ScopeKey(threadId, cmd));
                    }
                }
            }
        }

        return Result.CONTINUE;
    }

    @Override
    public void afterProcessEnds(Runtime runtime, State state, Frame lastFrame) {
        processScope.close();
        processSpan.end();
        if (!spanExporter.flush().isSuccess()) {
            log.warn("SpanExporter failed to flush");
        }
        openTelemetry.close();

        if (!openSpans.isEmpty()) {
            log.warn("Unclosed spans: {}", openSpans);
        }

        if (!openScopes.isEmpty()) {
            log.warn("Unclosed scopes: {}", openScopes);
        }
    }

    record ScopeKey(ThreadId threadId, Command cmd) {
    }
}
