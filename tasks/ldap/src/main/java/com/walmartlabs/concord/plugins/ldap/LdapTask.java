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

import com.walmartlabs.concord.sdk.Context;
import com.walmartlabs.concord.sdk.ContextUtils;
import com.walmartlabs.concord.sdk.InjectVariable;
import com.walmartlabs.concord.sdk.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import java.util.*;

import static com.walmartlabs.concord.sdk.ContextUtils.assertString;

/**
 * Created by ppendha on 6/18/18.
 */
@Named("ldap")
public class LdapTask implements Task {

    private static final Logger log = LoggerFactory.getLogger(LdapTask.class);

    private static final String ACTION_KEY = "action";

    // in params
    private static final String LDAP_AD_SERVER = "ldapAdServer";
    private static final String LDAP_BIND_USER_DN = "bindUserDn";
    private static final String LDAP_BIND_PASSWORD = "bindPassword";
    private static final String LDAP_SEARCH_BASE = "searchBase";
    private static final String LDAP_USER = "user";
    private static final String LDAP_GROUP = "group";
    private static final String LDAP_SECURITY_ENABLED = "securityEnabled";
    private static final String LDAP_DN = "dn";

    // out params
    private static final String LDAP_OUT = "out";
    private static final String LDAP_DEFAULT_OUT = "ldapResult";

    @InjectVariable("ldapParams")
    private Map<String, Object> defaults;

    @Override
    public void execute(Context ctx) {
        Action action = getAction(ctx);

        switch (action) {
            case SEARCHBYDN: {
                log.info("Starting 'SearchByDn' Action");
                searchByDn(ctx, null);
                break;
            }
            case GETUSER: {
                log.info("Starting 'GetUser' Action");
                getUser(ctx);
                break;
            }
            case GETGROUP: {
                log.info("Starting 'GetGroup' Action");
                getGroup(ctx);
                break;
            }
            case ISMEMBEROF: {
                log.info("Starting 'IsMemberOf' Action");
                isMemberOf(ctx);
                break;
            }
            default:
                throw new IllegalArgumentException("Unsupported action type: " + action);
        }
    }

    public boolean isMemberOf(@InjectVariable("context") Context ctx, String userDn, String groupDn) {
        SearchResult result = searchByDn(ctx, groupDn);
        if (result == null) {
            return false;
        }

        NamingEnumeration<String> members = getAttrValues(result, "member");
        if (members != null) {
            while (members.hasMoreElements()) {
                String member = members.nextElement();
                if (Objects.equals(userDn, member) || isMemberOf(ctx, userDn, member)) {
                    return true;
                }
            }
        }
        return false;
    }

    private SearchResult searchByDn(Context ctx, String dn) {
        if (dn == null) {
            dn = assertString(ctx, LDAP_DN);
        }

        boolean success = false;
        SearchResult result = null;

        try {
            // create custom filter for dn
            String searchFilter = "(distinguishedName=" + dn + ")";

            // use private method search
            NamingEnumeration<SearchResult> results = search(ctx, searchFilter);

            if (results.hasMoreElements()) {
                result = results.nextElement();
                success = true;
            }

            setOutVariable(ctx, success, searchResultToMap(result));
            return result;
        } catch (Exception e) {
            throw new IllegalArgumentException("Error occurred while searching " + e);
        }
    }

    private SearchResult getUser(Context ctx) {
        String user = assertString(ctx, LDAP_USER);

        boolean success = false;
        SearchResult result = null;

        try {
            // create custom filter for user
            String searchFilter = "(|"
                    + "(userPrincipalName=" + user + ")"
                    + "(sAMAccountName=" + user + ")"
                    + "(mailNickname=" + user + ")"
                    + "(proxyAddresses=smtp:" + user + ")"
                    + "(mail=" + user + ")"
                    + ")";

            // use private method search
            NamingEnumeration<SearchResult> results = search(ctx, searchFilter);

            if (results.hasMoreElements()) {
                result = results.nextElement();
                success = true;
            }

            setOutVariable(ctx, success, searchResultToMap(result));
            return result;
        } catch (Exception e) {
            throw new IllegalArgumentException("Error occurred while searching " + e);
        }
    }

    private SearchResult getGroup(Context ctx) {
        String group = assertString(ctx, LDAP_GROUP);
        boolean securityEnabled = ContextUtils.assertVariable(ctx, LDAP_SECURITY_ENABLED, Boolean.class);

        boolean success = false;
        SearchResult result = null;

        try {
            // create custom filter for group
            String searchFilter = "(name=" + group + ")";

            // use private method search
            NamingEnumeration<SearchResult> results = search(ctx, searchFilter);

            while (results.hasMoreElements()) {
                result = results.nextElement();
                String dn = getAttrValue(result, "distinguishedName");

                if (dn != null && dn.toLowerCase().contains("ou=security") == securityEnabled) {
                    success = true;
                    break;
                }
            }

            setOutVariable(ctx, success, searchResultToMap(result));
            return result;
        } catch (Exception e) {
            throw new IllegalArgumentException("Error occurred while searching " + e);
        }
    }

    private void isMemberOf(Context ctx) {
        boolean success = false;
        boolean result = false;

        try {
            SearchResult user = getUser(ctx);
            SearchResult group = getGroup(ctx);

            if (user != null && group != null) {
                String userDn = getAttrValue(user, "distinguishedName");
                String groupDn = getAttrValue(group, "distinguishedName");

                result = isMemberOf(ctx, userDn, groupDn);
                success = true;
            }

            setOutVariable(ctx, success, result);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error occurred while searching " + e);
        }
    }

    private NamingEnumeration<SearchResult> search(Context ctx, String searchFilter) {
        String searchBase = assertString(ctx, LDAP_SEARCH_BASE);
        LdapContext connection = null;
        try {
            connection = establishConnection(ctx);
            SearchControls searchControls = new SearchControls();
            searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);

            // create SearchRequest
            return connection.search(searchBase, searchFilter, searchControls);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error occurred while searching " + e);
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (NamingException e) {
                    throw new IllegalArgumentException("Error occurred while closing connection " + e);
                }
            }
        }
    }

    private LdapContext establishConnection(Context ctx) {
        String ldapAdServer = getString(defaults, ctx, LDAP_AD_SERVER, null);
        String bindUserDn = getString(defaults, ctx, LDAP_BIND_USER_DN, null);
        String bindPassword = getString(defaults, ctx, LDAP_BIND_PASSWORD, null);

        Hashtable<String, Object> env = new Hashtable<String, Object>();
        env.put(javax.naming.Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(javax.naming.Context.PROVIDER_URL, ldapAdServer);
        env.put(javax.naming.Context.SECURITY_AUTHENTICATION, "simple");
        env.put(javax.naming.Context.SECURITY_PRINCIPAL, bindUserDn);
        env.put(javax.naming.Context.SECURITY_CREDENTIALS, bindPassword);
        env.put("java.naming.ldap.version", "3");

        try {
            return new InitialLdapContext(env, null);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error while establishing connection " + e);
        }
    }

    private static String getAttrValue(SearchResult result, String id) {
        NamingEnumeration<String> values = getAttrValues(result, id);

        if (values.hasMoreElements()) {
            return values.nextElement();
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private static NamingEnumeration<String> getAttrValues(SearchResult result, String id) {
        if (result != null) {
            Attributes attributes = result.getAttributes();
            if (attributes != null) {
                Attribute attribute = attributes.get(id);
                if (attribute != null) {
                    try {
                        return (NamingEnumeration<String>) attribute.getAll();
                    } catch (NamingException e) {
                        return null;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Converts a {@link SearchResult} object to a Map
     *
     * @param r object to convert to a Map
     * @return Map representation of the SearchResult object
     */
    private static Map<String, Object> searchResultToMap(SearchResult r) {
        if (r == null) {
            return null;
        }

        Map<String, Object> rMap = new HashMap<>(r.getAttributes().size());
        try {
            Attributes attrs = r.getAttributes();
            NamingEnumeration<String> e = attrs.getIDs();

            Map<String, Object> attrsMap = new HashMap<>(attrs.size());
            while (e.hasMore()) {
                String key = e.next();
                Attribute attribute = attrs.get(key);

                Set<String> values = getAllAttributesValues(attribute);
                if (values.size() == 1) {
                    attrsMap.put(key, values.iterator().next());
                } else {
                    attrsMap.put(key, values);
                }

            }
            rMap.put("attributes", attrsMap);
        } catch (Exception ex) {
            log.error("Error mapping SearchResult attributes: {}", ex.getMessage());
        }

        return rMap;
    }

    private static Set<String> getAllAttributesValues(Attribute attribute) throws NamingException {
        Set<String> values = new HashSet<>();

        NamingEnumeration ne = attribute.getAll();
        while (ne.hasMore()) {
            Object o = ne.next();
            values.add(o.toString());
        }

        return values;
    }

    private static void setOutVariable(Context ctx, Boolean success, Object result) {
        String key = getString(null, ctx, LDAP_OUT, LDAP_DEFAULT_OUT);

        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("result", result);

        ctx.setVariable(key, response);
    }

    private static Action getAction(Context ctx) {
        return Action.valueOf(assertString(ctx, ACTION_KEY).trim().toUpperCase());
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

    private enum Action {
        SEARCHBYDN,
        GETUSER,
        GETGROUP,
        ISMEMBEROF
    }
}
