package com.walmartlabs.concord.plugins.zoom;

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

import static com.walmartlabs.concord.plugins.zoom.TaskParams.SendMessageParams;

public class ZoomTaskCommon {

    private static final Logger log = LoggerFactory.getLogger(ZoomTaskCommon.class);

    private final boolean dryRunMode;

    public ZoomTaskCommon() {
        this(false);
    }

    public ZoomTaskCommon(boolean dryRunMode) {
        this.dryRunMode = dryRunMode;
    }

    public Result execute(TaskParams in) {

        log.info("Starting '{}' action...", in.action());

        switch (in.action()) {
            case SENDMESSAGE: {
                return sendMessage((SendMessageParams)in);
            }
            default:
                throw new IllegalArgumentException("Unsupported action type: " + in.action());
        }
    }

    private Result sendMessage(SendMessageParams in) {
        try (ZoomClient client = new ZoomClient(in, dryRunMode)) {
            Result r = client.message(in.robotJid(), in.headText(), in.bodyText(), in.channelId(), in.accountId(), in.rootApi());
            if (dryRunMode) {
                return r;
            }

            if (!r.isOk()) {
                log.warn("Error sending a zoom message: {}", r.getError());
            } else {
                log.info("Zoom message sent into '{}' channel", in.channelId());
            }
            return r;
        } catch (Exception e) {
            if (!in.ignoreErrors()) {
                log.error("call ['{}', '{}'] -> error", in.channelId(), in.headText(), e);
                throw new RuntimeException("zoom task error: ", e);
            }

            log.warn("Finished with a generic error (networking or internal zoom api errors). For details check for the 'ERROR' in logs: {}", e.getMessage());
            return new Result(false, e.getMessage(), null);
        }
    }
}
