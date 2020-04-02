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
import java.util.Map;

import static com.walmartlabs.concord.sdk.ContextUtils.assertString;

@Named("msteamsV2")
public class TeamsTaskV2 implements Task {

    private static final Logger log = LoggerFactory.getLogger(TeamsTask.class);

    @InjectVariable("msteamsParams")
    private static Map<String, Object> defaults;

    @Override
    public void execute(Context ctx) {
        Action action = getAction(ctx);
        Map<String, Object> cfg = createCfg(ctx);

        Map<String, Object> activity = MapUtils.assertMap(cfg, Constants.VAR_ACTIVITY);

        if (activity.containsKey("text") && activity.containsKey("attachments")) {
            throw new IllegalArgumentException("'activity' object cannot have 'text' " +
                    "and 'attachments' at the same time.");
        }

        boolean ignoreErrors = MapUtils.getBoolean(cfg, Constants.IGNORE_ERRORS_KEY, false);
        boolean useProxy = MapUtils.getBoolean(cfg, Constants.USE_PROXY_KEY, false);

        log.info("Starting '{}' action....", action);

        switch (action) {
            case CREATECONVERSATION: {
                createConversation(ctx, cfg, activity, ignoreErrors, useProxy);
                break;
            }
            case REPLYTOCONVERSATION: {
                replyToConversation(ctx, cfg, activity, ignoreErrors, useProxy);
                break;
            }
            default:
                throw new IllegalArgumentException("Unsupported action type: " + action);
        }
    }

    private void createConversation(@InjectVariable("context") Context ctx, Map<String, Object> cfg,
                                    Map<String, Object> activity, boolean ignoreErrors, boolean useProxy) {

        TeamsConfiguration teamsConfiguration = TeamsConfiguration.from(ctx);
        String channelId = MapUtils.assertString(cfg, Constants.VAR_CHANNEL_ID);

        Map<String, Object> result = new HashMap<>();
        Result r;

        try (TeamsClientV2 client = new TeamsClientV2(teamsConfiguration, useProxy)) {
            r = client.createConversation(cfg, activity, channelId, teamsConfiguration.getRootApi());

            if (!r.isOk()) {
                log.warn("Error sending message to msteams channel: {}", r.getError());
            } else {
                log.info("Message sent to msteams channel.");
            }
            setResult(ctx, result, r);
        } catch (Exception e) {
            r = Result.error(e.getMessage());
            if (ignoreErrors) {
                setResult(ctx, result, r);
                log.warn("Finished with a generic error (networking or internal 'msteams' error). " +
                        "For details check for the 'ERROR': {}", e.getMessage());
            } else {
                setResult(ctx, result, r);
                throw new RuntimeException("'msteams' task error: " + e.getMessage());
            }
        }
    }

    private void replyToConversation(@InjectVariable("context") Context ctx, Map<String, Object> cfg,
                                     Map<String, Object> activity, boolean ignoreErrors, boolean useProxy) {

        TeamsConfiguration teamsConfiguration = TeamsConfiguration.from(ctx);

        String conversationId = MapUtils.assertString(cfg, Constants.VAR_CONVERSATION_ID);
        Map<String, Object> result = new HashMap<>();
        Result r;

        try (TeamsClientV2 client = new TeamsClientV2(teamsConfiguration, useProxy)) {
            r = client.replyToConversation(activity, teamsConfiguration.getRootApi(), conversationId);

            if (!r.isOk()) {
                log.warn("Error sending message to conversation: {}", r.getError());
            } else {
                log.info("Message sent to conversation.");
            }
            setResult(ctx, result, r);
        } catch (Exception e) {
            r = Result.error(e.getMessage());
            if (ignoreErrors) {
                setResult(ctx, result, r);
                log.warn("Finished with generic error (networking or internal 'msteams' error). " +
                        "For details check for the 'ERROR': {}", e.getMessage());
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
        if (r.getActivityId() != null && r.getConversationId().contains(r.getActivityId())) {
            result.put("conversationId", r.getConversationId());
            result.put("activityId", r.getActivityId());
        }
        ctx.setVariable("result", result);
    }

    private enum Action {
        CREATECONVERSATION,
        REPLYTOCONVERSATION
    }
}
