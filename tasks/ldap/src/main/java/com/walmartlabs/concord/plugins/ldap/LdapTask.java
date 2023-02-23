package com.walmartlabs.concord.plugins.ldap;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2018 Wal-Mart Store, Inc.
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

import javax.inject.Named;
import java.util.Map;

@Named("ldap")
@SuppressWarnings("unused")
public class LdapTask implements Task {

    // out params
    private static final String LDAP_OUT = "out";
    private static final String LDAP_DEFAULT_OUT = "ldapResult";

    @InjectVariable(TaskParams.DEFAULT_PARAMS_KEY)
    private Map<String, Object> defaults;

    @Override
    public void execute(Context ctx) {
        Map<String, Object> result = new LdapTaskCommon()
                .execute(TaskParams.of(new ContextVariables(ctx), defaults, null));

        String key = getString(null, ctx, LDAP_OUT, LDAP_DEFAULT_OUT);

        ctx.setVariable(key, result);
    }

    public boolean isMemberOf(@InjectVariable("context") Context ctx, String userDn, String groupDn) {
        return new LdapTaskCommon().isMemberOf(TaskParams.searchParams(new ContextVariables(ctx), defaults, null), userDn, groupDn);
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

    private static String getString(Map<String, Object> defaults, Context ctx, String k, String defaultValue) {
        Object v = getValue(defaults, ctx, k, defaultValue);
        if (!(v instanceof String)) {
            throw new IllegalArgumentException("'" + k + "': expected a string value, got " + v);
        }
        return (String) v;
    }

    private static Object getValue(Map<String, Object> defaults, Context ctx, String k, Object defaultValue) {
        Object v = ctx.getVariable(k);

        if (v == null && defaults != null) {
            v = defaults.get(k);
        }

        if (v == null) {
            v = defaultValue;
        }

        if (v == null) {
            throw new IllegalArgumentException("Mandatory parameter '" + k + "' is required");
        }

        return v;
    }
}
