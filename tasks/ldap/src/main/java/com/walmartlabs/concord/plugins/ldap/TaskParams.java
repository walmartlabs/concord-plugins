package com.walmartlabs.concord.plugins.ldap;

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

import com.walmartlabs.concord.runtime.v2.sdk.MapBackedVariables;
import com.walmartlabs.concord.runtime.v2.sdk.Variables;

import java.util.*;

public class TaskParams implements LdapSearchParams {

    public static TaskParams of(Variables input, Map<String, Object> defaults, Map<String, Object> policyDefaults) {

        Variables variables = merge(input, defaults, policyDefaults);

        Action action = new TaskParams(variables).action();
        switch (action) {
            case SEARCHBYDN: {
                return new SearchByDnParams(variables);
            }
            case GETUSER: {
                return new GetUserParams(variables);
            }
            case GETGROUP: {
                return new GetGroupParams(variables);
            }
            case ISMEMBEROF: {
                return new MemberOfParams(variables);
            }
            default:
                throw new IllegalArgumentException("Unsupported action type: " + action);
        }
    }

    public static LdapSearchParams searchParams(Variables input, Map<String, Object> defaults, Map<String, Object> policyDefaults) {
        Variables variables = merge(input, defaults, policyDefaults);
        return new TaskParams(variables);
    }

    private static final String ACTION_KEY = "action";

    public static final String DEFAULT_PARAMS_KEY = "ldapParams";
    public static final String LDAP_AD_SERVER = "ldapAdServer";
    private static final String LDAP_BIND_USER_DN = "bindUserDn";
    private static final String LDAP_BIND_PASSWORD = "bindPassword";
    private static final String LDAP_SEARCH_BASE = "searchBase";
    public static final String LDAP_DNS_SRV_RR = "dnsSrvRr";

    protected final Variables variables;

    public TaskParams(Variables variables) {
        this.variables = variables;
    }

    public Action action() {
        String action = variables.assertString(ACTION_KEY);
        try {
            return Action.valueOf(action.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Unknown action: '" + action + "'. Available actions: " + Arrays.toString(Action.values()));
        }
    }

    @Override
    public String ldapAdServer() {
        return variables.getString(LDAP_AD_SERVER);
    }

    @Override
    public Map<String, Object> dnsSrvRr() {
        return variables.getMap(LDAP_DNS_SRV_RR, null);
    }

    @Override
    public String bindUserDn() {
        return variables.getString(LDAP_BIND_USER_DN);
    }

    @Override
    public String bindPassword() {
        return variables.getString(LDAP_BIND_PASSWORD);
    }

    @Override
    public String searchBase() {
        return variables.assertString(LDAP_SEARCH_BASE);
    }

    public static class SearchByDnParams extends TaskParams {

        private static final String LDAP_DN = "dn";

        public SearchByDnParams(Variables variables) {
            super(variables);
        }

        public String dn() {
            return variables.assertString(LDAP_DN);
        }
    }

    public static class GetUserParams extends TaskParams {

        private static final String LDAP_USER = "user";

        public GetUserParams(Variables variables) {
            super(variables);
        }

        public String user() {
            return variables.assertString(LDAP_USER);
        }
    }

    public static class GetGroupParams extends TaskParams {

        private static final String LDAP_GROUP = "group";
        private static final String LDAP_SECURITY_ENABLED = "securityEnabled";
        private static final String LDAP_SECURITY_GROUP_TYPES = "securityGroupTypes";

        public GetGroupParams(Variables variables) {
            super(variables);
        }

        public String group() {
            return variables.assertString(LDAP_GROUP);
        }

        public boolean securityEnabled() {
            return variables.assertBoolean(LDAP_SECURITY_ENABLED);
        }

        public List<String> securityGroupTypes() {
            return variables.getList(LDAP_SECURITY_GROUP_TYPES, Collections.emptyList());
        }
    }

    public static class MemberOfParams extends TaskParams {

        private static final String LDAP_USER = "user";
        private static final String LDAP_GROUP = "group";
        private static final String LDAP_SECURITY_ENABLED = "securityEnabled";
        private static final String LDAP_SECURITY_GROUP_TYPES = "securityGroupTypes";

        public MemberOfParams(Variables variables) {
            super(variables);
        }

        public String user() {
            return variables.assertString(LDAP_USER);
        }

        public String group() {
            return variables.assertString(LDAP_GROUP);
        }

        public boolean securityEnabled() {
            return variables.assertBoolean(LDAP_SECURITY_ENABLED);
        }

        public List<String> securityGroupTypes() {
            return variables.getList(LDAP_SECURITY_GROUP_TYPES, Collections.emptyList());
        }
    }

    public enum Action {
        SEARCHBYDN,
        GETUSER,
        GETGROUP,
        ISMEMBEROF
    }

    private static Variables merge(Variables variables, Map<String, Object> defaults, Map<String, Object> policyDefaults) {
        Map<String, Object> mergedVars = new HashMap<>(policyDefaults != null ? policyDefaults : Collections.emptyMap());
        mergedVars.putAll(defaults);
        mergedVars.putAll(variables.toMap());
        return new MapBackedVariables(mergedVars);
    }
}
