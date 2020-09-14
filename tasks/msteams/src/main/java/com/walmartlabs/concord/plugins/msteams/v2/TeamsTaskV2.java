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
import com.walmartlabs.concord.plugins.msteams.TeamsTaskCommon;
import com.walmartlabs.concord.plugins.msteams.TeamsTaskParams;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.Task;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import com.walmartlabs.concord.runtime.v2.sdk.Variables;

import javax.inject.Inject;
import javax.inject.Named;

@Named("msteams")
public class TeamsTaskV2 implements Task {

    private final Context context;

    private final TeamsTaskCommon delegate = new TeamsTaskCommon();

    @Inject
    public TeamsTaskV2(Context context) {
        this.context = context;
    }

    @Override
    public TaskResult execute(Variables input) {
        Result r = delegate.execute(TeamsTaskParams.of(input, context.defaultVariables().toMap()));

        return new TaskResult(r.isOk(), r.getError())
                .value("data", r.getData());
    }
}
