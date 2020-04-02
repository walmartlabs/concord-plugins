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
import com.walmartlabs.concord.sdk.MapUtils;
import com.walmartlabs.concord.sdk.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.walmartlabs.concord.sdk.ContextUtils.assertString;

@Named("msteams")
public class TeamsTask implements Task {

    private static final Logger log = LoggerFactory.getLogger(TeamsTask.class);

    @InjectVariable("msteamsParams")
    private static Map<String, Object> defaults;

    @Override
    public void execute(Context ctx) {
        Action action = getAction(ctx);
        Map<String, Object> cfg = createCfg(ctx);

        String title = MapUtils.getString(cfg, Constants.MESSAGE_TITLE_KEY, null);
        String text = MapUtils.assertString(cfg, Constants.MESSAGE_TEXT_KEY);
        String themeColor = MapUtils.getString(cfg, Constants.MESSAGE_THEME_COLOR_KEY, Constants.DEFAULT_THEME_COLOR);
        List<Object> sections = MapUtils.getList(cfg, Constants.MESSAGE_SECTIONS_KEY, null);
        List<Object> potentialAction = MapUtils.getList(cfg, Constants.MESSAGE_POTENTIAL_ACTION_KEY, null);
        boolean ignoreErrors = MapUtils.getBoolean(cfg, Constants.IGNORE_ERRORS_KEY, false);

        log.info("Starting '{}' action...", action);

        switch (action) {
            case SENDMESSAGE: {
                sendMessage(ctx, cfg, title, text, themeColor, sections, potentialAction, ignoreErrors);
                break;
            }
            default:
                throw new IllegalArgumentException("Unsupported action type: " + action);
        }
    }

    private void sendMessage(@InjectVariable("context") Context ctx, Map<String, Object> cfg, String title,
                             String text, String themeColor, List<Object> sections,
                             List<Object> potentialAction, boolean ignoreErrors) {
        TeamsConfiguration teamsConfiguration = TeamsConfiguration.from(ctx);
        Map<String, Object> result = new HashMap<>();
        Result r;

        try (TeamsClient client = new TeamsClient(teamsConfiguration)) {
            r = client.message(cfg, title, text, themeColor, sections, potentialAction);

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

    private static Map<String, Object> createCfg(Context ctx) {
        Map<String, Object> m = new HashMap<>(defaults != null ? defaults : Collections.emptyMap());
        for (String k : Constants.ALL_IN_PARAMS) {
            Object v = ctx.getVariable(k);
            if (v != null) {
                m.put(k, v);
            }
        }
        return m;
    }

    private static Action getAction(Context ctx) {
        return Action.valueOf(assertString(ctx, Constants.ACTION_KEY).trim().toUpperCase());
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
