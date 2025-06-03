package com.walmartlabs.concord.plugins.hashivault;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2025 Walmart Inc., Concord Authors
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

import com.walmartlabs.concord.runtime.v2.sdk.MapBackedVariables;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class TaskParamsTest {

    @Mock
    TaskParams.SecretExporter secretExporter;

    @Test
    void testBaseUrl() {
        var input = Map.<String, Object>of("baseUrl", "https://mock-api.local");
        var params = TaskParams.of(new MapBackedVariables(input), defaultParams(), secretExporter);

        assertEquals("https://mock-api.local", params.baseUrl());
    }

    @Test
    void testBaseUrlTrailingSlash() {
        var input = Map.<String, Object>of("baseUrl", "https://mock-api.local/");
        var params = TaskParams.of(new MapBackedVariables(input), defaultParams(), secretExporter);

        assertEquals("https://mock-api.local", params.baseUrl());
    }

    private static Map<String, Object> defaultParams() {
        return Map.of(
                "apiToken", "mock-key",
                "path", TestConstants.SECRET_PATH
            );
    }
}
