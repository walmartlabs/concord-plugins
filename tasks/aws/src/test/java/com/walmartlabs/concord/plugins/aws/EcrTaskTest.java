package com.walmartlabs.concord.plugins.aws;

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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.walmartlabs.concord.runtime.v2.sdk.MapBackedVariables;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Disabled("requires AWS credentials")
public class EcrTaskTest {

    @Test
    public void testDescribeImages() {
        var task = new EcrTask(new MockContext(), new ObjectMapper());
        var input = new MapBackedVariables(Map.of(
                "action", "describe-images",
                "region", "us-east-1",
                "repositoryName", "foo"
        ));
        var result = task.execute(input);
        assertInstanceOf(TaskResult.SimpleResult.class, result);
        assertNotNull(((TaskResult.SimpleResult) result).toMap().get("data"));
    }
}
