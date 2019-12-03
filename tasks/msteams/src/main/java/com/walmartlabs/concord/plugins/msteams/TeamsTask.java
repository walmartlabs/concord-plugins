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

import com.walmartlabs.concord.sdk.Context;
import com.walmartlabs.concord.sdk.InjectVariable;
import com.walmartlabs.concord.sdk.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.walmartlabs.concord.sdk.ContextUtils.*;

@Named("msteams")
public class TeamsTask implements Task {

    private static final Logger log = LoggerFactory.getLogger(TeamsTask.class);
    private static final String ACTION_KEY = "action";
    private static final String TEAMS_MESSAGE_TITLE = "title";
    private static final String TEAMS_MESSAGE_TEXT = "text";
    private static final String TEAMS_MESSAGE_THEME_COLOR = "themeColor";
    private static final String TEAMS_MESSAGE_SECTIONS = "sections";
    private static final String TEAMS_MESSAGE_POTENTIAL_ACTION = "potentialAction";
    private static final String IGNORE_ERRORS_KEY = "ignoreErrors";

    @Override
    public void execute(Context ctx) {
        Action action = getAction(ctx);

        String title = getString(ctx, TEAMS_MESSAGE_TITLE, null);
        String text = assertString(ctx, TEAMS_MESSAGE_TEXT);
        String themeColor = getString(ctx, TEAMS_MESSAGE_THEME_COLOR, Constants.VARS_DEFAULT_THEME_COLOR);
        List<Object> sections = getList(ctx, TEAMS_MESSAGE_SECTIONS, null);
        List<Object> potentialAction = getList(ctx, TEAMS_MESSAGE_POTENTIAL_ACTION, null);
        boolean ignoreErrors = getBoolean(ctx, IGNORE_ERRORS_KEY, false);

        log.info("Starting '{}' action...", action);

        switch (action) {
            case SENDMESSAGE: {
                sendMessage(ctx, title, text, themeColor, sections, potentialAction, ignoreErrors);
                break;
            }
            default:
                throw new IllegalArgumentException("Unsupported action type: " + action);
        }
    }

    private void sendMessage(@InjectVariable("context") Context ctx, String title, String text, String themeColor, List<Object> sections,
                             List<Object> potentialAction, boolean ignoreErrors) {
        TeamsConfiguration cfg = TeamsConfiguration.from(ctx);
        Map<String, Object> result = new HashMap<>();
        Result r;

        try (TeamsClient client = new TeamsClient(cfg)) {
            r = client.message(ctx, title, text, themeColor, sections, potentialAction);

            if (!r.isOk()) {
                log.warn("Error sending message to msteams channel: {}", r.getError());
            } else {
                log.info("Message sent to msteams channel");
            }
            setResult(ctx, result, r);
        } catch (Exception e) {
            r = Result.error(e.getMessage());
            if (ignoreErrors) {
                setResult(ctx, result, r);
                log.warn("Finished with a generic error (networking or internal 'msteams' webhook error). For details check for the 'ERROR': {}", e.getMessage());
            } else {
                setResult(ctx, result, r);
                throw new RuntimeException("'msteams' task error: " + e.getMessage());
            }
        }
    }

    private static Action getAction(Context ctx) {
        return Action.valueOf(assertString(ctx, ACTION_KEY).trim().toUpperCase());
    }

    private void setResult(Context ctx, Map<String, Object> result, Result r) {
        result.put("ok", r.isOk());
        result.put("error", r.getError());
        result.put("data", r.getData());
        ctx.setVariable("result", result);
    }

    private enum Action {
        SENDMESSAGE
    }
}
