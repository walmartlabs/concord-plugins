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

import static com.walmartlabs.concord.plugins.msteams.TeamsTaskParams.Action;
import static com.walmartlabs.concord.plugins.msteams.TeamsTaskParams.SendMessageParams;

public class TeamsTaskCommon {

    private static final Logger log = LoggerFactory.getLogger(TeamsTaskCommon.class);

    public Result execute(TeamsTaskParams in) {
        Action action = in.action();

        log.info("Starting '{}' action...", action);

        switch (action) {
            case SENDMESSAGE: {
                return sendMessage((SendMessageParams)in);
            }
            default:
                throw new IllegalArgumentException("Unsupported action type: " + action);
        }
    }

    private Result sendMessage(SendMessageParams in) {
        try (TeamsClient client = new TeamsClient(in)) {
            Result r = client.message(in, in.title(), in.text(), in.themeColor(), in.sections(), in.potentialAction());

            if (!r.isOk()) {
                log.warn("Error sending message to msteams channel: {}", r.getError());
            } else {
                log.info("Message sent to msteams channel");
            }
            return r;
        } catch (Exception e) {
            if (in.ignoreErrors()) {
                log.warn("Finished with a generic error (networking or internal 'msteams' webhook error). For details check for the 'ERROR': {}", e.getMessage());
                return Result.error(e.getMessage());
            } else {
                throw new RuntimeException("'msteams' task error: " + e.getMessage());
            }
        }
    }
}
