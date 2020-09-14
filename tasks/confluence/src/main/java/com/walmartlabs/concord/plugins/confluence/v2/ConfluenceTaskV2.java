package com.walmartlabs.concord.plugins.confluence.v2;

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

import com.walmartlabs.concord.plugins.confluence.ConfluenceTaskCommon;
import com.walmartlabs.concord.plugins.confluence.Result;
import com.walmartlabs.concord.plugins.confluence.TaskParams;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.Task;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import com.walmartlabs.concord.runtime.v2.sdk.Variables;

import javax.inject.Inject;
import javax.inject.Named;

@Named("confluence")
public class ConfluenceTaskV2 implements Task {

    private final Context context;

    @Inject
    public ConfluenceTaskV2(Context context) {
        this.context = context;
    }

    @Override
    public TaskResult execute(Variables input) {
        Result result = new ConfluenceTaskCommon(context.workingDirectory(), context.variables().toMap())
                .execute(TaskParams.of(input, context.defaultVariables().toMap()));

        TaskResult taskResult = new TaskResult(result.ok, result.error);
        if (result.pageId != null) {
            taskResult.value("pageId", result.pageId);
        }
        if (result.childId != null) {
            taskResult.value("childId", result.childId);
        }
        if (result.data != null) {
            taskResult.value("data", result.data);
        }
        return taskResult;
    }
}
