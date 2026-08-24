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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.ldap.LLdapContainer;
import org.testcontainers.utility.DockerImageName;

import javax.naming.Context;
import javax.naming.directory.*;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
public class LdapTaskCommonTest {

    private static final String BASE_DN = "dc=example,dc=com";
    private static final String ADMIN_PASSWORD = "adminpassword";
    // jdoe is created via LDAP add — used by getUser / searchByDn / getGroup / isMemberOf tests
    private static final String TEST_USER_UID = "jdoe";
    private static final String TEST_USER_MAIL = "john.doe@example.com";
    private static final String TEST_GROUP_CN = "testgroup";
    // alice is created via LLDAP REST API so she can be added to a group
    private static final String ALICE_UID = "alice";
    private static final String ALICE_MAIL = "alice@example.com";
    private static final String MEMBER_GROUP_CN = "devgroup";

    private static final DockerImageName LLDAP_IMAGE = DockerImageName.parse("lldap/lldap:v0.6.1-alpine")
            .asCompatibleSubstituteFor("lldap/lldap");

    @Container
    static LLdapContainer lldap = new LLdapContainer(LLDAP_IMAGE)
            .withBaseDn(BASE_DN)
            .withUserPass(ADMIN_PASSWORD)
            .withStartupTimeout(Duration.ofSeconds(120));

    @BeforeAll
    static void setup() throws Exception {
        createTestUser(TEST_USER_UID, "John", "Doe", TEST_USER_MAIL);

        // alice must be created via the LLDAP REST API so LLDAP tracks her internally
        // and can associate her with a group
        String token = getLldapToken();
        createLldapUser(token, ALICE_UID, ALICE_MAIL, "Alice", "Smith");
        int groupId = createLldapGroup(token, MEMBER_GROUP_CN);
        addLldapUserToGroup(token, ALICE_UID, groupId);
    }

    @Test
    void testGetUser() {
        var common = new LdapTaskCommon();

        var vars = new MapBackedVariables(Map.of(
                "action", "getUser",
                "ldapAdServer", lldap.getLdapUrl(),
                "bindUserDn", lldap.getUser(),
                "bindPassword", lldap.getPassword(),
                "searchBase", BASE_DN,
                "user", TEST_USER_MAIL
        ));

        var commonResult = common.execute(TaskParams.of(vars, Map.of(), Map.of()));

        assertTrue((boolean) commonResult.get("success"), "getUser should succeed");
        assertNotNull(commonResult.get("result"), "result should be non-null");

        var result = Assertions.assertInstanceOf(Map.class, commonResult.get("result"));
        var attrs = Assertions.assertInstanceOf(Map.class, result.get("attributes"));
        assertNotNull(attrs, "attributes should be present");
        assertEquals(TEST_USER_MAIL, attrs.get("mail"), "mail attribute should match");
    }

    /**
     * LLDAP exposes {@code distinguishedName} as an attribute on search results, so
     * {@code searchByDn} can locate entries by their full DN.
     */
    @Test
    void testSearchByDn() {
        var common = new LdapTaskCommon();

        var vars = new MapBackedVariables(Map.of(
                "action", "searchByDn",
                "ldapAdServer", lldap.getLdapUrl(),
                "bindUserDn", lldap.getUser(),
                "bindPassword", lldap.getPassword(),
                "searchBase", BASE_DN,
                "dn", "uid=" + TEST_USER_UID + ",ou=people," + BASE_DN
        ));

        var result = common.execute(TaskParams.of(vars, Map.of(), Map.of()));

        assertTrue((Boolean) result.get("success"), "searchByDn should find the user by DN");
        assertNotNull(result.get("result"));

        @SuppressWarnings("unchecked")
        var attrs = (Map<String, Object>) ((Map<String, Object>) result.get("result")).get("attributes");
        assertNotNull(attrs);
        assertEquals(TEST_USER_MAIL, attrs.get("mail"));
    }

    /**
     * LLDAP groups use the {@code cn} attribute; the task's filter {@code (name=...)} is
     * AD-specific and will not match any entry. This test verifies the action completes without
     * error and correctly signals not-found.
     */
    @Test
    void testGetGroup() {
        var common = new LdapTaskCommon();

        var vars = new MapBackedVariables(Map.of(
                "action", "getGroup",
                "ldapAdServer", lldap.getLdapUrl(),
                "bindUserDn", lldap.getUser(),
                "bindPassword", lldap.getPassword(),
                "searchBase", BASE_DN,
                "group", TEST_GROUP_CN,
                "securityEnabled", false,
                "securityGroupTypes", List.of()
        ));

        var result = common.execute(TaskParams.of(vars, Map.of(), Map.of()));

        assertFalse((Boolean) result.get("success"), "getGroup should return not-found against LLDAP");
        assertNull(result.get("result"));
    }

    /**
     * Affirmative test: {@code getGroup} returns a group when it exists in LDAP.
     * Uses {@code devgroup}, which was created via the LLDAP REST API in setup.
     * The task's {@code cn}-based fallback filter matches LLDAP's standard {@code cn} attribute,
     * and the non-AD group is returned since security filtering is disabled.
     */
    @Test
    void testGetGroupFound() {
        var common = new LdapTaskCommon();

        var vars = new MapBackedVariables(Map.of(
                "action", "getGroup",
                "ldapAdServer", lldap.getLdapUrl(),
                "bindUserDn", lldap.getUser(),
                "bindPassword", lldap.getPassword(),
                "searchBase", BASE_DN,
                "group", MEMBER_GROUP_CN,
                "securityEnabled", false,
                "securityGroupTypes", List.of()
        ));

        var result = common.execute(TaskParams.of(vars, Map.of(), Map.of()));

        assertTrue((Boolean) result.get("success"), "getGroup should find " + MEMBER_GROUP_CN);
        assertNotNull(result.get("result"));

        @SuppressWarnings("unchecked")
        var attrs = (Map<String, Object>) ((Map<String, Object>) result.get("result")).get("attributes");
        assertNotNull(attrs);
        assertEquals(MEMBER_GROUP_CN, attrs.get("cn"), "cn attribute should match the group name");
        assertNotNull(attrs.get("member"), "member attribute should list group members");
    }

    /**
     * {@code isMemberOf} resolves both user and group before checking membership.
     * The user is found via {@code mail}, but the group lookup uses the AD-specific
     * {@code (name=...)} filter which yields nothing in LLDAP — so membership is {@code false}.
     * This test verifies the action always returns {@code success: true} with a boolean result.
     */
    @Test
    void testIsMemberOf() {
        var common = new LdapTaskCommon();

        var vars = new MapBackedVariables(Map.of(
                "action", "isMemberOf",
                "ldapAdServer", lldap.getLdapUrl(),
                "bindUserDn", lldap.getUser(),
                "bindPassword", lldap.getPassword(),
                "searchBase", BASE_DN,
                "user", TEST_USER_MAIL,
                "group", TEST_GROUP_CN,
                "securityEnabled", false,
                "securityGroupTypes", List.of()
        ));

        var result = common.execute(TaskParams.of(vars, Map.of(), Map.of()));

        assertTrue((Boolean) result.get("success"), "isMemberOf should always report success");
        assertFalse((Boolean) result.get("result"), "membership should be false when group cannot be resolved");
    }

    /**
     * {@code isMemberOf} resolved via the public method with explicitly supplied DNs.
     * <p>
     * LLDAP's {@code getGroup} filter {@code (name=...)} is AD-specific and never matches
     * LLDAP groups, so the {@code execute(ISMEMBEROF)} path always returns {@code false}.
     * This test bypasses that gap by calling the public
     * {@link LdapTaskCommon#isMemberOf(LdapSearchParams, String, String)} method directly with
     * known DNs, exercising the real membership check against live LLDAP data:
     * <ol>
     *   <li>{@code searchByDn} locates the group using LLDAP's virtual
     *       {@code distinguishedName} filter.</li>
     *   <li>The {@code member} attribute on the group entry is compared to the user DN.</li>
     * </ol>
     */
    @Test
    void testIsMemberOfTrue() {
        var common = new LdapTaskCommon();

        // Build a TaskParams that supplies connection config and searchBase
        var vars = new MapBackedVariables(Map.of(
                "action", "isMemberOf",
                "ldapAdServer", lldap.getLdapUrl(),
                "bindUserDn", lldap.getUser(),
                "bindPassword", lldap.getPassword(),
                "searchBase", BASE_DN,
                "user", ALICE_MAIL,
                "group", MEMBER_GROUP_CN,
                "securityEnabled", false,
                "securityGroupTypes", List.of()
        ));
        var searchParams = (LdapSearchParams) TaskParams.of(vars, Map.of(), Map.of());

        String aliceDn = "uid=" + ALICE_UID + ",ou=people," + BASE_DN;
        String groupDn = "cn=" + MEMBER_GROUP_CN + ",ou=groups," + BASE_DN;

        assertTrue(common.isMemberOf(searchParams, aliceDn, groupDn),
                "alice should be a member of " + MEMBER_GROUP_CN);
    }

    @SuppressWarnings("SameParameterValue")
    private static void createTestUser(String uid, String givenName, String sn, String mail) throws Exception {
        LdapContext ctx = bindAsAdmin();
        try {
            Attributes attrs = new BasicAttributes(true);
            Attribute objectClass = new BasicAttribute("objectClass");
            objectClass.add("inetOrgPerson");
            attrs.put(objectClass);
            attrs.put(new BasicAttribute("uid", uid));
            attrs.put(new BasicAttribute("cn", givenName + " " + sn));
            attrs.put(new BasicAttribute("givenName", givenName));
            attrs.put(new BasicAttribute("sn", sn));
            attrs.put(new BasicAttribute("mail", mail));
            ctx.createSubcontext("uid=" + uid + ",ou=people," + BASE_DN, attrs);
        } finally {
            ctx.close();
        }
    }

    private static LdapContext bindAsAdmin() throws Exception {
        Hashtable<String, Object> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, lldap.getLdapUrl());
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, lldap.getUser());
        env.put(Context.SECURITY_CREDENTIALS, lldap.getPassword());
        env.put("java.naming.ldap.version", "3");
        return new InitialLdapContext(env, null);
    }

    private static String lldapApiUrl() {
        return "http://" + lldap.getHost() + ":" + lldap.getMappedPort(17170);
    }

    private static String getLldapToken() throws Exception {
        var body = "{\"username\":\"admin\",\"password\":\"" + ADMIN_PASSWORD + "\"}";
        var response = httpPost(lldapApiUrl() + "/auth/simple/login", body, null);
        // parse: {"token":"<jwt>", ...}
        int start = response.indexOf("\"token\":\"") + 9;
        int end = response.indexOf("\"", start);
        return response.substring(start, end);
    }

    @SuppressWarnings("SameParameterValue")
    private static void createLldapUser(String token, String uid, String email,
                                        String firstName, String lastName) throws Exception {
        String body = "{\"query\":\"mutation CreateUser($u: CreateUserInput!) { createUser(user: $u) { id } }\","
                + "\"variables\":{\"u\":{\"id\":\"" + uid + "\",\"email\":\"" + email + "\","
                + "\"displayName\":\"" + firstName + " " + lastName + "\","
                + "\"firstName\":\"" + firstName + "\",\"lastName\":\"" + lastName + "\"}}}";
        httpPost(lldapApiUrl() + "/api/graphql", body, token);
    }

    @SuppressWarnings("SameParameterValue")
    private static int createLldapGroup(String token, String name) throws Exception {
        String body = "{\"query\":\"mutation { createGroup(name: \\\"" + name + "\\\") { id } }\"}";
        String response = httpPost(lldapApiUrl() + "/api/graphql", body, token);
        // parse: {"data":{"createGroup":{"id":<N>}}}
        int start = response.lastIndexOf("\"id\":") + 5;
        int end = response.indexOf("}", start);
        return Integer.parseInt(response.substring(start, end).trim());
    }

    private static void addLldapUserToGroup(String token, String userId, int groupId) throws Exception {
        String body = "{\"query\":\"mutation { addUserToGroup(userId: \\\"" + userId
                + "\\\", groupId: " + groupId + ") { ok } }\"}";
        httpPost(lldapApiUrl() + "/api/graphql", body, token);
    }

    private static String httpPost(String url, String body, String bearerToken) throws Exception {
        var builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (bearerToken != null) {
            builder.header("Authorization", "Bearer " + bearerToken);
        }
        var response = HttpClient.newHttpClient().send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return response.body();
    }
}
