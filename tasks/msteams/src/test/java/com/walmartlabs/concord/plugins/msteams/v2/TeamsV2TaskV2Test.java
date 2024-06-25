package com.walmartlabs.concord.plugins.msteams.v2;

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

import com.walmartlabs.concord.plugins.msteams.Result;
import com.walmartlabs.concord.plugins.msteams.TeamsV2TaskCommon;
import com.walmartlabs.concord.plugins.msteams.TeamsV2TaskParams;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.MapBackedVariables;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamsV2TaskV2Test {

    @Mock
    Context context;

    @Mock
    TeamsV2TaskCommon delegate;

    @Test
    void testTask() {
        when(context.defaultVariables())
                .thenReturn(new MapBackedVariables(Map.of()));

        when(delegate.execute(any(TeamsV2TaskParams.CreateConversationParams.class)))
                .thenReturn(new Result(true, null, "mock-data", "mock-conversation-id?mock-activity-id", "mock-activity-id"));

        var input = Map.<String, Object>of(
                "action", "createConversation"
        );

        var task = new TeamsV2TaskV2(context, delegate);

        var result = Assertions.assertInstanceOf(TaskResult.SimpleResult.class, task.execute(new MapBackedVariables(input)));

        assertTrue(result.ok());
        assertEquals("mock-data", result.values().get("data"));
        assertEquals("mock-conversation-id?mock-activity-id", result.values().get("conversationId"));
        assertEquals("mock-activity-id", result.values().get("activityId"));
    }
}
