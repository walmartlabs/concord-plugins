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

import com.walmartlabs.concord.sdk.Context;
import com.walmartlabs.concord.sdk.InjectVariable;
import com.walmartlabs.concord.sdk.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import java.util.HashMap;
import java.util.Map;

import static com.walmartlabs.concord.sdk.ContextUtils.*;

@Named("zoom")
public class ZoomTask implements Task {

    private static final Logger log = LoggerFactory.getLogger(ZoomTask.class);
    private static final String ACTION_KEY = "action";
    private static final String ZOOM_CHANNEL_JID = "channelId";
    private static final String ZOOM_HEAD_TEXT = "headText";
    private static final String ZOOM_BODY_TEXT = "bodyText";
    private static final String IGNORE_ERRORS_KEY = "ignoreErrors";

    @Override
    public void execute(Context ctx) {
        Action action = getAction(ctx);

        String channelId = assertString(ctx, ZOOM_CHANNEL_JID);
        String headText = assertString(ctx, ZOOM_HEAD_TEXT);
        String bodyText = getString(ctx, ZOOM_BODY_TEXT, null);
        boolean ignoreErrors = getBoolean(ctx, IGNORE_ERRORS_KEY, false);

        log.info("Starting '{}' action...", action);

        switch (action) {
            case SENDMESSAGE: {
                sendMessage(ctx, channelId, headText, bodyText, ignoreErrors);
                break;
            }
            default:
                throw new IllegalArgumentException("Unsupported action type: " + action);
        }
    }

    private void sendMessage(@InjectVariable("context") Context ctx, String channelId, String headText, String bodyText, boolean ignoreErrors) {
        ZoomConfiguration cfg = ZoomConfiguration.from(ctx);

        try (ZoomClient client = new ZoomClient(cfg)) {
            Result r = client.message(cfg.getRobotJid(), headText, bodyText, channelId, cfg.getAccountId(), cfg.getRootApi());

            if (!r.isOk()) {
                log.warn("Error sending a zoom message: {}", r.getError());
            } else {
                log.info("Zoom message sent into '{}' channel", channelId);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("ok", r.isOk());
            result.put("error", r.getError());
            result.put("data", r.getData());
            ctx.setVariable("result", result);
        } catch (Exception e) {
            if (ignoreErrors) {
                log.warn("Finished with a generic error (networking or internal zoom api errors). For details check for the 'ERROR' in logs: {}", e.getMessage());
            } else {
                log.error("call ['{}', '{}', '{}'] -> error",
                        channelId, headText, e);
                throw new RuntimeException("zoom task error: ", e);
            }
        }
    }

    private static Action getAction(Context ctx) {
        return Action.valueOf(assertString(ctx, ACTION_KEY).trim().toUpperCase());
    }

    private enum Action {
        SENDMESSAGE
    }
}
