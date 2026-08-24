package com.walmartlabs.concord.plugins.ldap;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2026 Walmart Inc., Concord Authors
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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Manual integration tests against a real Active Directory instance.
 * <p>
 * All tests in this class are {@link Disabled} and must be run explicitly.
 * Supply connection details as system properties, e.g.:
 * <pre>
 *   mvn test -pl tasks/ldap -Dtest=LdapActiveDirectoryIT#testGetGroup \
 *       -DadServer=ldap://ad.corp.example.com \
 *       -DbindUserDn="CN=svc_ldap,OU=ServiceAccounts,DC=corp,DC=example,DC=com" \
 *       -DbindPassword=secret \
 *       -DsearchBase="DC=corp,DC=example,DC=com" \
 *       -DgroupName="My Security Group" \
 *       -DsecurityEnabled=true \
 *       -DsecurityGroupTypes="-2147483640,-2147483646"
 * </pre>
 *
 * <table border="1">
 *   <caption>System properties</caption>
 *   <tr><th>Property</th><th>Description</th><th>Required</th></tr>
 *   <tr><td>{@code adServer}</td><td>LDAP URL, e.g. {@code ldap://ad.corp.example.com}</td><td>yes</td></tr>
 *   <tr><td>{@code bindUserDn}</td><td>Full DN of the bind user</td><td>yes</td></tr>
 *   <tr><td>{@code bindPassword}</td><td>Bind user password</td><td>yes</td></tr>
 *   <tr><td>{@code searchBase}</td><td>LDAP search base, e.g. {@code DC=corp,DC=example,DC=com}</td><td>yes</td></tr>
 *   <tr><td>{@code groupName}</td><td>AD group name to look up</td><td>yes</td></tr>
 *   <tr><td>{@code securityEnabled}</td><td>{@code true} for security groups, {@code false} otherwise (default: {@code false})</td><td>no</td></tr>
 *   <tr><td>{@code securityGroupTypes}</td><td>Comma-separated list of AD {@code groupType} values to treat as security groups</td><td>no</td></tr>
 * </table>
 */
@Disabled("Requires a real Active Directory instance — run manually with the system properties documented in the class Javadoc")
class LdapActiveDirectoryIT {

    private static final Logger log = LoggerFactory.getLogger(LdapActiveDirectoryIT.class);

    @Test
    void testGetGroup() {
        String adServer = requireProp("adServer");
        String bindUserDn = requireProp("bindUserDn");
        String bindPassword = requireProp("bindPassword");
        String searchBase = requireProp("searchBase");
        String groupName = requireProp("groupName");
        boolean securityEnabled = Boolean.parseBoolean(System.getProperty("securityEnabled", "false"));
        List<String> securityGroupTypes = parseList(System.getProperty("securityGroupTypes", ""));

        var vars = new MapBackedVariables(Map.of(
                "action", "getGroup",
                "ldapAdServer", adServer,
                "bindUserDn", bindUserDn,
                "bindPassword", bindPassword,
                "searchBase", searchBase,
                "group", groupName,
                "securityEnabled", securityEnabled,
                "securityGroupTypes", securityGroupTypes
        ));

        var result = new LdapTaskCommon().execute(TaskParams.of(vars, Map.of(), Map.of()));

        assertTrue((Boolean) result.get("success"),
                "Expected group '" + groupName + "' to be found in AD");
        assertNotNull(result.get("result"), "Expected a non-null group result");

        @SuppressWarnings("unchecked")
        var attrs = (Map<String, Object>) ((Map<String, Object>) result.get("result")).get("attributes");
        assertNotNull(attrs, "Expected attributes to be present in the result");
        for  (Map.Entry<String, Object> e : attrs.entrySet()) {
            log.info("{} = {}", e.getKey(), e.getValue());
        }
    }

    private static String requireProp(String name) {
        String value = System.getProperty(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Required system property '" + name + "' is not set. "
                    + "See the class Javadoc for usage.");
        }
        return value;
    }

    private static List<String> parseList(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
