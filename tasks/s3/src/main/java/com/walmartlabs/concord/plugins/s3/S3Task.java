package com.walmartlabs.concord.plugins.s3;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2019 Walmart Inc.
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
import com.walmartlabs.concord.runtime.v2.sdk.Variables;
import com.walmartlabs.concord.sdk.Context;
import com.walmartlabs.concord.sdk.ContextUtils;
import com.walmartlabs.concord.sdk.InjectVariable;
import com.walmartlabs.concord.sdk.Task;

import javax.inject.Named;
import java.util.Map;

@Named("s3")
@SuppressWarnings("unused")
public class S3Task implements Task {

    @InjectVariable("s3Params")
    private Map<String, Object> defaults;

    @Override
    public void execute(Context ctx) throws Exception {
        Result r = delegate(ctx).execute(TaskParams.of(new ContextVariables(ctx), defaults));

        ObjectMapper om = new ObjectMapper();
        ctx.setVariable("result", om.convertValue(r, Map.class));
    }

    private static S3TaskCommon delegate(Context ctx) {
        return new S3TaskCommon(ContextUtils.getWorkDir(ctx));
    }

    private static class ContextVariables implements Variables {

        private final Context context;

        public ContextVariables(Context context) {
            this.context = context;
        }

        @Override
        public Object get(String key) {
            return context.getVariable(key);
        }

        @Override
        public void set(String key, Object value) {
            throw new IllegalStateException("Unsupported");
        }

        @Override
        public boolean has(String key) {
            return context.getVariable(key) != null;
        }

        @Override
        public Map<String, Object> toMap() {
            return context.toMap();
        }
    }
}
