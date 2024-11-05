package com.walmartlabs.concord.plugins.jsonpath.v2;

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

import com.walmartlabs.concord.plugins.jsonpath.JsonPathTaskCommon;
import com.walmartlabs.concord.plugins.jsonpath.TaskParams;
import com.walmartlabs.concord.runtime.v2.sdk.*;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;

@Named("jsonPath")
@DryRunReady
@SuppressWarnings("unused")
public class JsonPathTaskV2 implements Task {

    private final JsonPathTaskCommon delegate;

    @Inject
    public JsonPathTaskV2(Context context) {
        this.delegate = new JsonPathTaskCommon(context.workingDirectory());
    }

    @Override
    public TaskResult execute(Variables input) throws Exception {
        Object result = delegate.execute(new TaskParams(input));
        return TaskResult.success()
                .value("result", result);
    }

    public Object read(Object v, String jsonPath) {
        return delegate.read(v, jsonPath);
    }

    public Object readJson(String s, String jsonPath) {
        return delegate.readJson(s, jsonPath);
    }

    public Object readFile(Object v, String jsonPath) throws IOException {
        return delegate.readFile(v, jsonPath);
    }
}
