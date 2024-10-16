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
import io.opentelemetry.api.trace.*;
import io.opentelemetry.sdk.internal.AttributesMap;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.IdGenerator;
import io.opentelemetry.sdk.trace.data.StatusData;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static io.opentelemetry.api.common.AttributeKey.longKey;
import static io.opentelemetry.api.common.AttributeKey.stringKey;

public class ConcordSpanBuilder {

    private final String traceId;
    private final Resource resource;
    private final String spanId;
    private final String spanName;
    private String parentSpanId;
    private final SpanKind spanKind = SpanKind.INTERNAL;
    private final Map<AttributeKey<?>, Object> attributes = new HashMap<>();
    private StatusData status;
    private long startEpochNanos;
    private long endEpochNanos;

    public static ConcordSpanBuilder builder(String traceId, Resource resource, String spanName) {
        return builder(traceId, resource, IdGenerator.random().generateSpanId(), spanName);
    }

    public static ConcordSpanBuilder builder(String traceId, Resource resource, String spanId, String spanName) {
        return new ConcordSpanBuilder(traceId, resource, spanId, spanName);
    }

    public ConcordSpanBuilder(String traceId, Resource resource, String spanId, String spanName) {
        this.traceId = traceId;
        this.resource = resource;
        this.spanId = spanId;
        this.spanName = spanName;
    }

    public ConcordSpanBuilder setAttribute(String key, String value) {
        return setAttribute(stringKey(key), value);
    }

    public ConcordSpanBuilder setAttribute(String key, long value) {
        return setAttribute(longKey(key), value);
    }

    public <T> ConcordSpanBuilder setAttribute(AttributeKey<T> key, T value) {
        if (key == null || key.getKey().isEmpty() || value == null) {
            return this;
        }
        attributes.put(key, value);
        return this;
    }

    public ConcordSpanBuilder setStartTimestamp(long startTimestamp, TimeUnit unit) {
        startEpochNanos = unit.toNanos(startTimestamp);
        return this;
    }

    public ConcordSpanBuilder setParentSpanId(String parentSpanId) {
        this.parentSpanId = parentSpanId;
        return this;
    }

    public ConcordSpanBuilder setStatus(StatusCode statusCode) {
        this.status = StatusData.create(statusCode, "");
        return this;
    }

    public ConcordSpanBuilder end(Long millis) {
        if (millis != null) {
            return end(millis, TimeUnit.MILLISECONDS);
        }
        return this;
    }

    public ConcordSpanBuilder end(long timestamp, TimeUnit unit) {
        end(unit.toNanos(timestamp));
        return this;
    }

    private void end(long endEpochNanos) {
        this.endEpochNanos = endEpochNanos;
    }

    public ConcordSpan build() {
        AttributesMap attrs = AttributesMap.create(attributes.size(), Integer.MAX_VALUE);
        attrs.putAll(attributes);
        return ImmutableConcordSpan.builder()
                .resource(resource)
                .name(spanName)
                .kind(spanKind)
                .spanContext(buildContext(traceId, spanId))
                .parentSpanContext(buildContext(traceId, parentSpanId))
                .attributes(attrs)
                .startEpochNanos(startEpochNanos)
                .endEpochNanos(endEpochNanos)
                .status(status)
                .build();
    }

    private static SpanContext buildContext(String traceId, String spanId) {
        return SpanContext.create(
                traceId,
                spanId,
                TraceFlags.getSampled(),
                TraceState.getDefault()
        );
    }
}
