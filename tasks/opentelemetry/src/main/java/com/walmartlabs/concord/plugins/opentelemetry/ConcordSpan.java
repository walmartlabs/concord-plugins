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
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.sdk.common.InstrumentationLibraryInfo;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.StatusData;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.Collections;

@Value.Immutable
@Value.Style(jdkOnly = true)
public abstract class ConcordSpan implements ReadableSpan {

    public abstract Resource resource();

    public abstract Attributes attributes();

    public abstract long startEpochNanos();
    public abstract long endEpochNanos();

    public abstract StatusData status();

    @Override
    public SpanData toSpanData() {
        return ConcordSpanData.builder()
                .name(getName())
                .kind(getKind())
                .spanContext(getSpanContext())
                .parentSpanContext(getParentSpanContext())
                .status(status())
                .startEpochNanos(startEpochNanos())
                .endEpochNanos(endEpochNanos())
                .attributes(attributes())
                .events(Collections.emptyList())
                .links(Collections.emptyList())
                .hasEnded(hasEnded())
                .totalRecordedEvents(0)
                .totalRecordedLinks(0)
                .totalAttributeCount(attributes().size())
                .resource(resource())
                .instrumentationLibraryInfo(getInstrumentationLibraryInfo())
                .build();
    }

    @Override
    public InstrumentationLibraryInfo getInstrumentationLibraryInfo() {
        return InstrumentationLibraryInfo.empty();
    }

    @Override
    public boolean hasEnded() {
        return true;
    }

    @Override
    public long getLatencyNanos() {
        return 0;
    }

    @Override
    public <T> T getAttribute(AttributeKey<T> key) {
        return attributes() == null ? null : attributes().get(key);
    }

    @Override
    @Nullable
    public abstract SpanContext getParentSpanContext();
}
