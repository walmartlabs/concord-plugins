package com.walmartlabs.concord.plugins.taurus;

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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.walmartlabs.concord.runtime.v2.sdk.Variables;
import com.walmartlabs.concord.sdk.*;

import javax.inject.Inject;
import javax.inject.Named;
import java.net.URI;
import java.util.Map;

@Named("taurus")
@SuppressWarnings("unused")
public class TaurusTask implements Task {

    private static final ObjectMapper om = new ObjectMapper();

    @InjectVariable("taurusParams")
    private Map<String, Object> defaults;
    private final DependencyManager dependencyManager;

    @Inject
    public TaurusTask(DependencyManager dependencyManager) {
        this.dependencyManager = dependencyManager;
    }

    @Override
    public void execute(Context ctx) throws Exception {
        BinaryResolver binaryResolver = new BinaryResolver(url -> dependencyManager.resolve(URI.create(url)));

        Taurus.Result result =
                delegate(ctx).execute(TaskParams.of(new ContextVariables(ctx), defaults), binaryResolver);

        ctx.setVariable("result", om.convertValue(result, Map.class));
    }

    private static TaurusTaskCommon delegate(Context ctx) {
        return new TaurusTaskCommon(ContextUtils.getWorkDir(ctx));
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
