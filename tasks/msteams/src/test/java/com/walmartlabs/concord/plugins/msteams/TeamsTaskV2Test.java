package com.walmartlabs.concord.plugins.msteams;

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

import com.walmartlabs.concord.sdk.Context;
import com.walmartlabs.concord.sdk.MockContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamsTaskV2Test {

    @Mock
    TeamsV2TaskCommon delegate;

    Context context;
    Map<String, Object> ctxVars;

    @BeforeEach
    void setUp() {
        ctxVars = new HashMap<>();
        context = new MockContext(ctxVars);
    }

    @Test
    void testTask() {
        when(delegate.execute(any(TeamsV2TaskParams.CreateConversationParams.class)))
                .thenReturn(new Result(true, null, "mock-data", "mock-conversation-id?mock-activity-id", "mock-activity-id"));

        var task = new TeamsTaskV2(delegate);

        ctxVars.put("action", "createConversation");

        task.execute(context);

        Map<?, ?> result = assertInstanceOf(Map.class, ctxVars.get("result"));
        assertEquals("mock-data", result.get("data"));
        assertEquals("mock-conversation-id?mock-activity-id", result.get("conversationId"));
        assertEquals("mock-activity-id", result.get("activityId"));
    }
}
