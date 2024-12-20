package com.walmartlabs.concord.plugins.zoom.v2;

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

import com.walmartlabs.concord.plugins.zoom.Result;
import com.walmartlabs.concord.plugins.zoom.TaskParams;
import com.walmartlabs.concord.plugins.zoom.ZoomTaskCommon;
import com.walmartlabs.concord.runtime.v2.sdk.*;

import javax.inject.Inject;
import javax.inject.Named;

@Named("zoom")
@DryRunReady
public class ZoomTaskV2 implements Task {

    private final Context context;
    private final ZoomTaskCommon delegate;

    @Inject
    public ZoomTaskV2(Context context) {
        this.context = context;
        this.delegate = new ZoomTaskCommon(context.processConfiguration().dryRun());
    }

    @Override
    public TaskResult execute(Variables input) {
        Result result = delegate.execute(TaskParams.of(input, context.defaultVariables().toMap()));
        return TaskResult.of(result.isOk(), result.getError())
                .value("data", result.getData());
    }
}
