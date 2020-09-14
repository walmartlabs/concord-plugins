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

import javax.inject.Named;
import java.util.HashMap;
import java.util.Map;

@Named("msteams")
@SuppressWarnings("unused")
public class TeamsTask implements Task {

    @InjectVariable("msteamsParams")
    private Map<String, Object> defaults;

    private final TeamsTaskCommon delegate = new TeamsTaskCommon();

    @Override
    public void execute(Context ctx) {
        Result r = delegate.execute(TeamsTaskParams.of(new ContextVariables(ctx), defaults));

        Map<String, Object> result = new HashMap<>();
        result.put("ok", r.isOk());
        result.put("error", r.getError());
        result.put("data", r.getData());
        ctx.setVariable("result", result);
    }
}
