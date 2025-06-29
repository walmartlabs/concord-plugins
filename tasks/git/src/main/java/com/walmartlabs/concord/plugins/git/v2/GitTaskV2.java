package com.walmartlabs.concord.plugins.git.v2;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2020 Walmart Inc.
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

import com.walmartlabs.concord.plugins.git.GitTask;
import com.walmartlabs.concord.runtime.v2.sdk.*;
import com.walmartlabs.concord.sdk.MapUtils;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Map;

@Named("git")
@DryRunReady
public class GitTaskV2 implements Task {

    private final Context context;
    private final GitTask delegate;

    @Inject
    public GitTaskV2(Context context) {
        this.context = context;
        this.delegate = new GitTask(new SecretServiceV2(context.secretService()), context.workingDirectory(), context.processConfiguration().dryRun());
    }

    @Override
    public TaskResult.SimpleResult execute(Variables input) throws Exception {
        Map<String, Object> result = delegate.execute(input.toMap(), context.defaultVariables().toMap());

        // we can't change the delegate's return type w/o breaking compatibility with Concord < 1.62.0
        // and we can't break that right now as we don't have a way to properly communicate this breakage
        // (i.e. there's no support for multiple versions of docs per plugin)
        // so, we have to parse the map back...

        return TaskResult.of(MapUtils.getBoolean(result, "ok", true), MapUtils.getString(result, "error"))
                .values(result);
    }

}
