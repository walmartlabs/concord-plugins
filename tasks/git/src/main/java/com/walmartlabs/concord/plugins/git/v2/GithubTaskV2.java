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

import com.walmartlabs.concord.plugins.git.GitHubTask;
import com.walmartlabs.concord.runtime.v2.sdk.*;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Map;

@Named("github")
@SuppressWarnings("unused")
@DryRunReady
public class GithubTaskV2 implements Task {

    private final GitHubTask delegate;

    private final Map<String, Object> defaults;

    @Inject
    public GithubTaskV2(Context ctx) {
        this.defaults = ctx.defaultVariables().toMap();
        this.delegate = new GitHubTask(ctx.processConfiguration().dryRun());
    }

    @Override
    public TaskResult execute(Variables input) {
        Map<String, Object> result = delegate.execute(input.toMap(), defaults);
        return TaskResult.success().values(result);
    }
}
