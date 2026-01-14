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
import com.walmartlabs.concord.sdk.Context;
import com.walmartlabs.concord.sdk.MockContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@WireMockTest
@ExtendWith(WireMockExtension.class)
@ExtendWith(MockitoExtension.class)
public class TaskV1Test extends AbstractTaskTest {

    Context context;

    @TempDir
    Path workDir;

    @BeforeEach
    void setup() {
        context = new MockContext(new HashMap<>());

        context.setVariable("workDir", workDir.toString());
    }

    @Test
    void testGetContentBasic(WireMockRuntimeInfo wiremock) {
        stubGetContentWithBasicAuth();

        var task = new ConfluenceTask();

        context.setVariable("apiUrl", wiremock.getHttpBaseUrl() + "/rest/api/");
        context.setVariable("action", "getPageContent");
        context.setVariable("pageId", 12345L);
        context.setVariable("auth", Map.of(
                "basic", Map.of(
                        "username", MOCK_USERNAME,
                        "password", MOCK_PASSWORD
                )));

        task.execute(context);

        var result = assertInstanceOf(Map.class, context.getVariable("result"));
        assertEquals("<table></table>", result.get("data"));
    }

    @Test
    void testGetContentPat(WireMockRuntimeInfo wiremock) throws Exception {
        stubGetContentWithPat();

        var task = new ConfluenceTask();

        context.setVariable("apiUrl", wiremock.getHttpBaseUrl() + "/rest/api/");
        context.setVariable("action", "getPageContent");
        context.setVariable("pageId", 12345L);
        context.setVariable("auth", Map.of("accessToken", MOCK_PAT));

        task.execute(context);

        var result = assertInstanceOf(Map.class, context.getVariable("result"));
        assertEquals("<table></table>", result.get("data"));
    }

}
