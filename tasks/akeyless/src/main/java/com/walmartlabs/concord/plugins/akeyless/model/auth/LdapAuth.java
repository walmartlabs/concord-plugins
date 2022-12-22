package com.walmartlabs.concord.plugins.akeyless.model.auth;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2022 Walmart Inc., Concord Authors
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

import com.walmartlabs.concord.plugins.akeyless.SecretExporter;
import com.walmartlabs.concord.plugins.akeyless.Util;
import com.walmartlabs.concord.plugins.akeyless.model.Auth;
import com.walmartlabs.concord.plugins.akeyless.model.Secret;
import com.walmartlabs.concord.runtime.v2.sdk.MapBackedVariables;
import com.walmartlabs.concord.runtime.v2.sdk.Variables;

public class LdapAuth extends Auth {
    private static final String ACCESS_ID_KEY = "accessId";
    private static final String USERNAME_KEY = "username";
    private static final String PASSWORD_KEY = "password";
    private static final String LDAP_CREDS_KEY = "credentials";

    public static Auth of(Variables vars, SecretExporter secretExporter) {
        if (!vars.has(LDAP_CREDS_KEY)) {
            throw new IllegalArgumentException("LDAP auth config is missing " + LDAP_CREDS_KEY + " option");
        }

        Auth auth = new Auth()
                .accessType("ldap")
                .accessId(Util.stringOrSecret(vars.get(ACCESS_ID_KEY), secretExporter));

        Variables authCreds = new MapBackedVariables(vars.assertMap(LDAP_CREDS_KEY));

        if (authCreds.has(USERNAME_KEY) && authCreds.has(PASSWORD_KEY)) {
            return auth.ldapUsername(authCreds.assertString(USERNAME_KEY))
                    .ldapPassword(authCreds.assertString(PASSWORD_KEY));
        }

        if (authCreds.has("org") && authCreds.has("name")) {
            Secret.CredentialsSecret creds = exportUsernamePassword(authCreds, secretExporter);

            return auth.ldapUsername(creds.getUsername())
                    .ldapPassword(creds.getPassword());
        }

        throw new IllegalArgumentException("Invalid LDAP auth parameters given");
    }

    private LdapAuth() {
    }

    private static Secret.CredentialsSecret exportUsernamePassword(Variables secretInfo, SecretExporter secretExporter) {
        final String o = secretInfo.assertString("org");
        final String n = secretInfo.assertString("name");
        final String p = secretInfo.getString("password", null);

        return secretExporter.exportCredentials(o, n, p);
    }
}
