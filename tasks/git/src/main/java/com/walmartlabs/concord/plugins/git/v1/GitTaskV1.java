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

import com.walmartlabs.concord.plugins.git.GitTask;
import com.walmartlabs.concord.sdk.*;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Map;

@Named("git")
public class GitTaskV1 implements Task {

    private static final String OUT_KEY = "out";
    private static final String DEFAULT_OUT_VAR_KEY = "result";

    @InjectVariable("gitParams")
    private Map<String, Object> defaults;

    private final SecretService secretService;

    @Inject
    public GitTaskV1(SecretService secretService) {
        this.secretService = secretService;
    }

    @Override
    public void execute(Context ctx) throws Exception {
        Map<String, Object> result = new GitTask(new SecretServiceV1(secretService, ctx), ContextUtils.getWorkDir(ctx), false)
                .execute(ctx.toMap(), defaults);

        String out = ContextUtils.getString(ctx, OUT_KEY, DEFAULT_OUT_VAR_KEY);
        ctx.setVariable(out, result);
    }

}
