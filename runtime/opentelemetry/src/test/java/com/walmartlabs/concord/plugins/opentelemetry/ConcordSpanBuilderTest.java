package com.walmartlabs.concord.plugins.opentelemetry;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2026 Walmart Inc., Concord Authors
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
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.sdk.resources.Resource;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConcordSpanBuilderTest {

    @Test
    void buildsSpanAttributesWithPublicApi() {
        AttributeKey<String> key = AttributeKey.stringKey("test.key");

        ConcordSpan span = ConcordSpanBuilder.builder("0123456789abcdef0123456789abcdef", Resource.empty(), "test-span")
                .setStartTimestamp(1, TimeUnit.MILLISECONDS)
                .setAttribute(key, "test-value")
                .setStatus(StatusCode.OK)
                .end(2, TimeUnit.MILLISECONDS)
                .build();

        assertEquals("test-value", span.getAttribute(key));
        assertEquals("test-value", span.toSpanData().getAttributes().get(key));
    }
}
