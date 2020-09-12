package com.walmartlabs.concord.plugins.jenkins;

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


import com.walmartlabs.concord.runtime.v2.sdk.Variables;
import com.walmartlabs.concord.sdk.Context;
import com.walmartlabs.concord.sdk.InjectVariable;
import com.walmartlabs.concord.sdk.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import java.util.Map;

@Named("jenkins")
@SuppressWarnings("unused")
public class JenkinsTask implements Task {

    private static final Logger log = LoggerFactory.getLogger(JenkinsTask.class);

    private static final String OUT_VARIABLE_KEY = "jenkinsJob";

    @InjectVariable("jenkinsParams")
    private Map<String, Object> defaults;

    private final JenkinsTaskCommon delegate = new JenkinsTaskCommon();

    public void execute(Context ctx) throws Exception {
        Map<String, Object> result = delegate.execute(JenkinsConfiguration.of(new ContextVariables(ctx), defaults));
        ctx.setVariable(OUT_VARIABLE_KEY, result);
        log.info("Job info is saved as '{}' variable: {}", OUT_VARIABLE_KEY, result);
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
