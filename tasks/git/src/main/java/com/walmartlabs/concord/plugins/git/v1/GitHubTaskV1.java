package com.walmartlabs.concord.plugins.git.v1;

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
import com.walmartlabs.concord.sdk.Context;
import com.walmartlabs.concord.sdk.InjectVariable;
import com.walmartlabs.concord.sdk.Task;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Map;

@Named("github")
public class GitHubTaskV1 implements Task {

    private final GitHubTask delegate;

    @InjectVariable("githubParams")
    private Map<String, Object> defaults;

    @Inject
    public GitHubTaskV1() {
        this.delegate = new GitHubTask();
    }

    @Override
    public void execute(Context ctx) {
        Map<String, Object> result = delegate.execute(ctx.toMap(), defaults);
        result.forEach(ctx::setVariable);
    }
}
