package com.walmartlabs.concord.plugins.jenkins.v2;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2020 Walmart Inc., Concord Authors
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

import com.walmartlabs.concord.plugins.jenkins.JenkinsConfiguration;
import com.walmartlabs.concord.plugins.jenkins.JenkinsTaskCommon;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.MapBackedVariables;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JenkinsTaskV2Test {

    @Test
    void failedJenkinsBuildReturnsFailedTaskResult() throws Exception {
        Context context = mock(Context.class);
        when(context.defaultVariables()).thenReturn(new MapBackedVariables(Collections.emptyMap()));

        JenkinsTaskCommon delegate = new JenkinsTaskCommon() {
            @Override
            public Map<String, Object> execute(JenkinsConfiguration cfg) {
                return Map.of(
                        "status", "FAILURE",
                        "buildNumber", 42,
                        "isSuccess", false
                );
            }
        };

        JenkinsTaskV2 task = new JenkinsTaskV2(context, delegate);
        TaskResult.SimpleResult result = assertInstanceOf(TaskResult.SimpleResult.class,
                task.execute(new MapBackedVariables(input())));

        assertFalse(result.ok());
        assertEquals("Jenkins job finished with status FAILURE", result.error());
        assertEquals("FAILURE", result.values().get("status"));
    }

    private static Map<String, Object> input() {
        return Map.of(
                "baseUrl", "http://jenkins.example",
                "username", "user",
                "apiToken", "token",
                "jobName", "job"
        );
    }
}
