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
import java.util.*;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

/**
 * Created by ppendha on 6/18/18.
 */
@Named("ldap")
public class LdapTask implements Task {

    private static final Logger log = LoggerFactory.getLogger(LdapTask.class);

    public static final String ACTION_KEY = "action";

    // in params
    public static final String LDAP_AD_SERVER = "ldapAdServer";
    public static final String LDAP_BIND_USER_DN = "bindUserDn";
    public static final String LDAP_BIND_PASSWORD = "bindPassword";
    public static final String LDAP_SEARCH_BASE = "searchBase";
    public static final String LDAP_USER = "user";
    public static final String LDAP_GROUP = "group";
    public static final String LDAP_SECURITY_ENABLED = "securityEnabled";
    public static final String LDAP_SEARCH_FILTER = "searchFilter";
    public static final String LDAP_DN = "dn";

    // out params
    public static final String LDAP_OUT = "out";
    public static final String LDAP_DEFAULT_OUT = "ldapResult";


    @InjectVariable("ldapParams")
    private Map<String, Object> defaults;

    @Override
    public void execute(Context ctx) throws Exception {
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

    @SuppressWarnings("unchecked")
    public SearchResult searchByDn(Context ctx, String dn) {
        if (dn == null) {
            dn = ContextUtils.assertString(ctx, LDAP_DN);
        }

        Boolean success = false;
        SearchResult result = null;

        try {
            // create custom filter for dn
            String searchFilter = "(distinguishedName=" + dn + ")";

            // use private method search
            NamingEnumeration<SearchResult> results = search(ctx, searchFilter);

            if (results.hasMoreElements()) {
                result = (SearchResult) results.nextElement();
                success = true;
            }

            setOutVariable(ctx, success, searchResultToMap(result));
            return result;
        } catch (Exception e) {
            throw new IllegalArgumentException("Error occurred while searching " + e);
        }
    }

    @SuppressWarnings("unchecked")
    public SearchResult getUser(Context ctx) {
        String user = ContextUtils.assertString(ctx, LDAP_USER);

        Boolean success = false;
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
                result = (SearchResult) results.nextElement();
                success = true;
            }

            setOutVariable(ctx, success, searchResultToMap(result));
            return result;
        } catch (Exception e) {
            throw new IllegalArgumentException("Error occurred while searching " + e);
        }
    }

    @SuppressWarnings("unchecked")
    public SearchResult getGroup(Context ctx) {
        String group = ContextUtils.assertString(ctx, LDAP_GROUP);
        Boolean securityEnabled = ContextUtils.assertVariable(ctx, LDAP_SECURITY_ENABLED, Boolean.class);

        Boolean success = false;
        SearchResult result = null;

        try {
            // create custom filter for group
            String searchFilter = "(name=" + group + ")";
            
            // use private method search
            NamingEnumeration<SearchResult> results = search(ctx, searchFilter);

            while (results.hasMoreElements()) {
                result = (SearchResult) results.nextElement();
                String dn = getAttrValue(result, "distinguishedName");
                if (dn.toLowerCase().contains("ou=security") == securityEnabled) {
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

    @SuppressWarnings("unchecked")
    public Boolean isMemberOf(Context ctx) {
        Boolean success = false;
        Boolean result = false;
        
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
            return result;
        } catch (Exception e) {
            throw new IllegalArgumentException("Error occurred while searching " + e);
        }
    }

    private Boolean isMemberOf(Context ctx, String userDn, String groupDn) {
        SearchResult result = searchByDn(ctx, groupDn);
        if (result != null) {
            NamingEnumeration<String> members = getAttrValues(result, "member");
            if (members != null) {
                while (members.hasMoreElements()) {
                    String member = (String) members.nextElement();
                    if (Objects.equals(userDn, member) || isMemberOf(ctx, userDn, member)) {
                        return true;
                    }
                }    
            }
        }
        return false;
    }

    private NamingEnumeration<SearchResult> search(Context ctx, String searchFilter) {
        String searchBase = ContextUtils.assertString(ctx, LDAP_SEARCH_BASE);
        LdapContext connection = null;
        try {
            connection = establishConnection(ctx);
            SearchControls searchControls = new SearchControls();
            searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);

            // create SearchRequest
            NamingEnumeration<SearchResult> result = connection.search(searchBase, searchFilter, searchControls);
            return result;
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
            Map<String, String> attrsMap = new HashMap<>(attrs.size());
            while (e.hasMore()) {
                String key = e.next();
                attrsMap.put(key, attrs.get(key).get().toString());
            }
            rMap.put("attributes", attrsMap);
        } catch (Exception ex) {
            log.error("Error mapping SearchResult attributes: {}", ex.getMessage());
        }

        return rMap;
    }

    private static void setOutVariable(Context ctx, Boolean success, Object result) {
        String key = getString(null, ctx, LDAP_OUT, LDAP_DEFAULT_OUT);       
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("result", result);

        ctx.setVariable(key, response);
    }

    private static Action getAction(Context ctx) {
        return Action.valueOf(ContextUtils.assertString(ctx, ACTION_KEY).trim().toUpperCase());
    }

    private static Long getLong(Map<String, Object> defaults, Context ctx, String k, Long defaultValue) {
        Object v = getValue(defaults, ctx, k, defaultValue);

        if (v instanceof Integer) {
            v = ((Integer) v).longValue();
        }

        if (!(v instanceof Long)) {
            throw new IllegalArgumentException("'" + k + "': expected a number, got " + v);
        }

        return (Long) v;
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
