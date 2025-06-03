package com.walmartlabs.concord.plugins.msteams.it;

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
import com.walmartlabs.concord.runtime.v2.sdk.MapBackedVariables;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Disabled("requires sensitive vars file")
class MSTeamsV2IT extends AbstractIT {
    @Test
    void testV2() {
        Map<String, Object> defaults = defaultVars();

        // Create a conversation
        Map<String, Object> input = new HashMap<>();
        input.put("action", "createConversation");
        input.put("activity", Map.of(
                "type", "message",
                "text", "Integration test message: " + ZonedDateTime.now()));
        input.put("ignoreErrors", true);

        Result createResult = new TeamsV2TaskCommon().execute(TeamsV2TaskParams.of(new MapBackedVariables(input), defaults));
        assertTrue(createResult.isOk());
        String conversationId = createResult.getConversationId();

        // Reply in a thread or whatever teams calls it
        input.put("action", "replyToConversation");
        input.put("activity", Map.of(
                "type", "message",
                "text", "Integration test reply: " + ZonedDateTime.now()));
        input.put("conversationId", conversationId);
        input.put("ignoreErrors", true);

        Result replyResult = new TeamsV2TaskCommon().execute(TeamsV2TaskParams.of(new MapBackedVariables(input), defaults));
        assertTrue(replyResult.isOk());
    }

}
