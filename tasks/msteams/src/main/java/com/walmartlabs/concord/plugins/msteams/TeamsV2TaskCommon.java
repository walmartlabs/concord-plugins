package com.walmartlabs.concord.plugins.msteams;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2018 Walmart Inc.
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static com.walmartlabs.concord.plugins.msteams.TeamsV2TaskParams.CreateConversationParams;
import static com.walmartlabs.concord.plugins.msteams.TeamsV2TaskParams.ReplayToConversationParams;

public class TeamsV2TaskCommon {

    private static final Logger log = LoggerFactory.getLogger(TeamsTask.class);

    public Result execute(TeamsV2TaskParams in) {
        Map<String, Object> activity = in.activity();

        if (activity.containsKey("text") && activity.containsKey("attachments")) {
            throw new IllegalArgumentException("'activity' object cannot have 'text' " +
                    "and 'attachments' at the same time.");
        }

        log.info("Starting '{}' action....", in.action());

        switch (in.action()) {
            case CREATECONVERSATION: {
                return createConversation((CreateConversationParams)in);
            }
            case REPLYTOCONVERSATION: {
                return replyToConversation((ReplayToConversationParams)in);
            }
            default:
                throw new IllegalArgumentException("Unsupported action type: " + in.action());
        }
    }

    private Result createConversation(CreateConversationParams in) {
        try (TeamsClientV2 client = new TeamsClientV2(in)) {
            Result r = client.createConversation(in.tenantId(), in.activity(), in.channelId(), in.rootApi());

            if (!r.isOk()) {
                log.warn("Error sending message to msteams channel: {}", r.getError());
            } else {
                log.info("Message sent to msteams channel.");
            }

            return r;
        } catch (Exception e) {
            if (in.ignoreErrors()) {
                log.warn("Finished with a generic error (networking or internal 'msteams' error). " +
                        "For details check for the 'ERROR': {}", e.getMessage());
                return Result.error(e.getMessage());
            } else {
                throw new RuntimeException("'msteams' task error: " + e.getMessage());
            }
        }
    }

    private Result replyToConversation(ReplayToConversationParams in) {
        String conversationId = in.conversationId();

        try (TeamsClientV2 client = new TeamsClientV2(in)) {
            Result r = client.replyToConversation(in.activity(), in.rootApi(), conversationId);

            if (!r.isOk()) {
                log.warn("Error sending message to conversation: {}", r.getError());
            } else {
                log.info("Message sent to conversation.");
            }
            return r;
        } catch (Exception e) {
            if (in.ignoreErrors()) {
                log.warn("Finished with generic error (networking or internal 'msteams' error). " +
                        "For details check for the 'ERROR': {}", e.getMessage());
                return Result.error(e.getMessage());
            } else {
                throw new RuntimeException("'msteams' task error: " + e.getMessage());
            }
        }
    }
}
