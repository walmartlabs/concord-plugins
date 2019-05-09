package com.walmartlabs.concord.plugins.taurus;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2019 Walmart Inc.
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

import com.walmartlabs.concord.sdk.Context;
import org.junit.Ignore;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Ignore
public class TaurusTaskTest {

    @Test
    public void test() throws Exception {
        Path workDir = Files.createTempDirectory("test");

        Path scenarioFile = Paths.get(TaurusTaskTest.class.getResource("test.yml").toURI());
        Files.copy(scenarioFile, workDir.resolve("test.yml"));

        Map<String, Object> args = new HashMap<>();
        args.put("useFakeHome", false);
        args.put("action", "run");
        args.put(com.walmartlabs.concord.sdk.Constants.Context.WORK_DIR_KEY, workDir.toString());
        args.put("configs", Arrays.asList(
                "test.yml",
                Collections.singletonMap("scenarios",
                        Collections.singletonMap("quick-test",
                                Collections.singletonMap("variables",
                                        Collections.singletonMap("endpoint", "/api/v1/server/ping"))))
        ));

        Context ctx = mock(Context.class);
        when(ctx.getVariable(anyString())).then(i -> args.get(i.getArgument(0)));
        when(ctx.interpolate(any())).then(i -> i.getArgument(0));

        TaurusTask t = new TaurusTask();
        t.execute(ctx);
    }
}
