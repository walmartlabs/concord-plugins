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
import com.walmartlabs.concord.sdk.*;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Map;

@Named("github")
public class GitHubTaskV1 implements Task {

    @InjectVariable("githubParams")
    private Map<String, Object> defaults;

    private final SecretService secretService;

    @Inject
    public GitHubTaskV1(SecretService secretService) {
        this.secretService = secretService;
    }

    @Override
    public void execute(Context ctx) {
        Map<String, Object> result = getDelegate(ctx).execute(ctx.toMap(), getDefaults(), new SecretServiceV1(secretService, ctx));
        result.forEach(ctx::setVariable);
    }

    public String createAppAccessToken(@InjectVariable("context") Context ctx, Map<String, Object> in) {
        return getDelegate(ctx).createAppToken(in, getDefaults(), new SecretServiceV1(secretService, ctx));
    }

    GitHubTask getDelegate(Context ctx) {
        return new GitHubTask(ContextUtils.getTxId(ctx));
    }

    Map<String, Object> getDefaults() {
        return defaults == null ? Map.of() : defaults;
    }
}
