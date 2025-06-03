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

import javax.inject.Inject;
import javax.inject.Named;
import java.util.HashMap;
import java.util.Map;

@Named("msteamsV2")
@SuppressWarnings("unused")
public class TeamsTaskV2 implements Task {

    private static final Logger log = LoggerFactory.getLogger(TeamsTaskV2.class);

    @InjectVariable("msteamsParams")
    private Map<String, Object> defaults;

    private final TeamsV2TaskCommon delegate;

    @Inject
    public TeamsTaskV2(TeamsV2TaskCommon delegate) {
        this.delegate = delegate;
    }

    @Override
    public void execute(Context ctx) {
        Result r = delegate.execute(TeamsV2TaskParams.of(new ContextVariables(ctx), defaults));

        Map<String, Object> result = new HashMap<>();
        result.put("ok", r.isOk());
        result.put("error", r.getError());
        result.put("data", r.getData());
        if (r.getActivityId() != null && r.getConversationId().contains(r.getActivityId())) {
            result.put("conversationId", r.getConversationId());
            result.put("activityId", r.getActivityId());
        }
        ctx.setVariable("result", result);
    }
}
