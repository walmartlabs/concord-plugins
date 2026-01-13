package com.walmartlabs.concord.plugins.confluence;

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

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.walmartlabs.concord.plugins.confluence.v2.ConfluenceTaskV2;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.MapBackedVariables;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult.SimpleResult;
import com.walmartlabs.concord.runtime.v2.sdk.Variables;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@WireMockTest
@ExtendWith(WireMockExtension.class)
@ExtendWith(MockitoExtension.class)
public class TaskV2Test extends AbstractTaskTest {

    @Mock
    Context context;

    @TempDir
    Path workDir;

    @BeforeEach
    void setup() {
        when(context.workingDirectory()).thenReturn(workDir);
        when(context.variables()).thenReturn(new MapBackedVariables(Map.of()));
        when(context.defaultVariables()).thenReturn(new MapBackedVariables(Map.of()));
    }

    @Test
    void testGetContentBasic(WireMockRuntimeInfo wiremock) {
        stubGetContentWithBasicAuth();

        var task = new ConfluenceTaskV2(context);

        var r = task.execute(params(Map.of(
                "apiUrl", wiremock.getHttpBaseUrl() + "/rest/api/",
                "action", "getPageContent",
                "pageId", 12345L,
                "auth", Map.of(
                        "basic", Map.of(
                                "username", MOCK_USERNAME,
                                "password", MOCK_PASSWORD
                        ))
        )));

        var result = assertInstanceOf(SimpleResult.class, r);
        assertTrue(result.ok());
        assertEquals("<table></table>", result.values().get("data"));
    }

    @Test
    void testGetContentPat(WireMockRuntimeInfo wiremock) throws Exception {
        stubGetContentWithPat();

        var task = new ConfluenceTaskV2(context);

        var r = task.execute(params(Map.of(
                "apiUrl", wiremock.getHttpBaseUrl() + "/rest/api/",
                "action", "getPageContent",
                "pageId", 12345L,
                "auth", Map.of("accessToken", MOCK_PAT)
        )));

        var result = assertInstanceOf(SimpleResult.class, r);
        assertTrue(result.ok());
        assertEquals("<table></table>", result.values().get("data"));
    }

    private static Variables params(Map<String, Object> input) {
        return new MapBackedVariables(input);
    }

}
