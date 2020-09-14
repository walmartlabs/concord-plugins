package com.walmartlabs.concord.plugins.s3.v2;

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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.walmartlabs.concord.plugins.s3.Result;
import com.walmartlabs.concord.plugins.s3.S3TaskCommon;
import com.walmartlabs.concord.plugins.s3.TaskParams;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.Task;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import com.walmartlabs.concord.runtime.v2.sdk.Variables;
import com.walmartlabs.concord.sdk.MapUtils;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Map;

@Named("s3")
public class S3TaskV2 implements Task {

    private final Context context;
    private final S3TaskCommon delegate;

    @Inject
    public S3TaskV2(Context context) {
        this.context = context;
        this.delegate = new S3TaskCommon(context.workingDirectory());
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Variables input) throws Exception {
        Result result = delegate.execute(TaskParams.of(input, context.defaultVariables().toMap()));

        ObjectMapper om = new ObjectMapper();
        Map<String, Object> r = om.convertValue(result, Map.class);

        return new TaskResult(MapUtils.getBoolean(r, "ok", false), MapUtils.getString(r, "error"))
                .values(r);
    }
}
