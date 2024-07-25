package com.walmartlabs.concord.plugins.msteams.v2;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2020 Walmart Inc., Concord Authors
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
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.Task;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import com.walmartlabs.concord.runtime.v2.sdk.Variables;

import javax.inject.Inject;
import javax.inject.Named;

@Named("msteamsV2")
public class TeamsV2TaskV2 implements Task {

    private final Context context;

    private final TeamsV2TaskCommon delegate;

    @Inject
    public TeamsV2TaskV2(Context context, TeamsV2TaskCommon delegate) {
        this.context = context;
        this.delegate = delegate;
    }

    @Override
    public TaskResult execute(Variables input) {
        Result r = delegate.execute(TeamsV2TaskParams.of(input, context.defaultVariables().toMap()));

        TaskResult.SimpleResult result = TaskResult.of(r.isOk(), r.getError())
                .value("data", r.getData());

        if (r.getActivityId() != null && r.getConversationId().contains(r.getActivityId())) {
            result.value("conversationId", r.getConversationId())
                    .value("activityId", r.getActivityId());
        }

        return result;
    }
}
