package com.walmartlabs.concord.plugins.jenkins;

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

import com.walmartlabs.concord.sdk.MockContext;
import org.junit.Ignore;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

@Ignore
public class JenkinsTaskTest {

    @Test
    public void testSimple() throws Exception {
        Map<String, Object> cfg = buildCfg();
        cfg.put(Constants.JOB_NAME_KEY, "test");

        MockContext ctx = new MockContext(cfg);
        JenkinsTask task = new JenkinsTask();

        task.execute(ctx);
    }

    @Test
    public void testBuildWithParams() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("stringParam", "iddqd");
        params.put("booleanParam", true);
        params.put("choiceParam", "one");
        params.put("mlineParam", "multi-line");
        params.put("file.txt", "@/tmp/jenkins-file-1");

        Map<String, Object> cfg = buildCfg();
        cfg.put(Constants.JOB_NAME_KEY, "test-with-params");
        cfg.put(Constants.PARAMETERS_KEY, params);

        MockContext ctx = new MockContext(cfg);
        JenkinsTask task = new JenkinsTask();

        task.execute(ctx);
    }

    private static Map<String, Object> buildCfg() {
        Map<String, Object> cfg = new HashMap<>();
        cfg.put(Constants.BASE_URL_KEY, "http://localhost:8181/");
        cfg.put(Constants.USERNAME_KEY, "admin");
        cfg.put(Constants.API_TOKEN_KEY, "114d8ade9646a0758438773fdd96dbbb67");
        return cfg;
    }
}
