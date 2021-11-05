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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class TaskParams implements LdapSearchParams {
    private static final Logger log = LoggerFactory.getLogger(TaskParams.class);

    public static TaskParams of(Variables input, Map<String, Object> defaults) {
        Variables variables = merge(input, defaults);

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

    public static LdapSearchParams searchParams(Variables input, Map<String, Object> defaults) {
        Variables variables = merge(input, defaults);
        return new TaskParams(variables);
    }

    private static final String ACTION_KEY = "action";

    private static final String LDAP_AD_SERVER = "ldapAdServer";
    private static final String LDAP_BIND_USER_DN = "bindUserDn";
    private static final String LDAP_BIND_PASSWORD = "bindPassword";
    private static final String LDAP_SEARCH_BASE = "searchBase";
    private static final String LDAP_CERTIFICATE = "certificate";

    protected final Variables variables;

    public TaskParams(Variables variables) {
        this.variables = variables;
    }

    public Action action() {
        String action = variables.assertString(ACTION_KEY);
        try {
            return Action.valueOf(action.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown action: '" + action + "'. Available actions: " + Arrays.toString(Action.values()));
        }
    }

    @Override
    public String ldapAdServer() {
        return variables.assertString(LDAP_AD_SERVER);
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

    @Override
    public boolean certificate() {
        Map<String, Object> certificate = variables.getMap(LDAP_CERTIFICATE, null);

        if (certificate == null || certificate.isEmpty()) {
            return false;
        }

        Path certPath = Paths.get(CustomSocketFactory.CERT_PATH);

        cleanExistingCerts(certPath);
        copyCertFile((String) certificate.get("file"), certPath);
        writeCertString((String) certificate.get("text"), certPath);
        // TODO support secret source?

        return true;
    }

    private static void cleanExistingCerts(Path certs) {
        try {
            Files.deleteIfExists(certs);
        } catch (IOException e) {
            log.error("Error cleaning existing LDAP CA certs", e);
        }
    }

    private static void copyCertFile(String src, Path dest) {
        if (src == null || src.trim().isEmpty()) {
            return;
        }

        try {
            Files.copy(Paths.get(src), dest);
        } catch (Exception e) {
            log.error("Error copying custom cert file", e);
        }
    }

    private static void writeCertString(String certString, Path dest) {
        if (certString == null || certString.trim().isEmpty()) {
            return;
        }

        try (FileOutputStream fos = new FileOutputStream(dest.toString(), true)) {
            fos.write(certString.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Error writing CA cert file", e);
        }
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

    private static Variables merge(Variables variables, Map<String, Object> defaults) {
        Map<String, Object> variablesMap = new HashMap<>(defaults != null ? defaults : Collections.emptyMap());
        variablesMap.putAll(variables.toMap());
        return new MapBackedVariables(variablesMap);
    }
}
