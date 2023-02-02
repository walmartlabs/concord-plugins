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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.CommunicationException;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.net.ssl.SSLHandshakeException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.walmartlabs.concord.plugins.ldap.TaskParams.*;
import static com.walmartlabs.concord.sdk.MapUtils.assertString;

public class LdapTaskCommon {

    private static final Logger log = LoggerFactory.getLogger(LdapTaskCommon.class);

    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY = 3000;

    public Map<String, Object> execute(TaskParams in) {
        switch (in.action()) {
            case SEARCHBYDN: {
                log.info("Starting 'SearchByDn' Action");

                SearchByDnParams p = (SearchByDnParams) in;
                SearchResult searchResult = searchByDn(p, p.searchBase(), p.dn());
                return toResult(searchResult);
            }
            case GETUSER: {
                log.info("Starting 'GetUser' Action");

                GetUserParams p = (GetUserParams) in;
                SearchResult searchResult = getUser(p, p.searchBase(), p.user());
                return toResult(searchResult);
            }
            case GETGROUP: {
                log.info("Starting 'GetGroup' Action");

                GetGroupParams p = (GetGroupParams) in;
                SearchResult searchResult = getGroup(p, p.searchBase(), p.group(), p.securityGroupTypes(), p.securityEnabled());
                return toResult(searchResult);
            }
            case ISMEMBEROF: {
                log.info("Starting 'IsMemberOf' Action");
                boolean member = isMemberOf((MemberOfParams) in);
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("result", member);
                return result;
            }
            default:
                throw new IllegalArgumentException("Unsupported action type: " + in.action());
        }
    }

    public boolean isMemberOf(LdapSearchParams searchParams, String userDn, String groupDn) {
        SearchResult result = searchByDn(searchParams, searchParams.searchBase(), groupDn);
        if (result == null) {
            return false;
        }

        NamingEnumeration<String> members = getAttrValues(result, "member");
        if (members != null) {
            while (members.hasMoreElements()) {
                String member = members.nextElement();
                if (Objects.equals(userDn, member) || isMemberOf(searchParams, userDn, member)) {
                    return true;
                }
            }
        }
        return false;
    }

    private SearchResult searchByDn(LdapConnectionCfg cfg, String searchBase, String dn) {
        try {
            // create custom filter for dn
            String searchFilter = "(distinguishedName=" + dn + ")";

            // use private method search
            NamingEnumeration<SearchResult> results = withRetry(MAX_RETRIES, RETRY_DELAY, () -> search(cfg, searchBase, searchFilter));

            if (results.hasMoreElements()) {
                return results.nextElement();
            }

            return null;
        } catch (Exception e) {
            throw new IllegalArgumentException("Error occurred while searching " + e);
        }
    }

    private SearchResult getUser(LdapConnectionCfg cfg, String searchBase, String user) {
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
            NamingEnumeration<SearchResult> results = withRetry(MAX_RETRIES, RETRY_DELAY, () -> search(cfg, searchBase, searchFilter));

            if (results.hasMoreElements()) {
                return results.nextElement();
            }

            return null;
        } catch (Exception e) {
            throw new IllegalArgumentException("Error occurred while searching " + e);
        }
    }

    private SearchResult getGroup(LdapConnectionCfg cfg, String searchBase, String group, List<String> securityGroupTypes, boolean securityEnabled) {
        try {
            // create custom filter for group
            String searchFilter = "(name=" + group + ")";

            // use private method search
            NamingEnumeration<SearchResult> results = withRetry(MAX_RETRIES, RETRY_DELAY, () -> search(cfg, searchBase, searchFilter));

            while (results.hasMoreElements()) {
                SearchResult result = results.nextElement();

                String dn = getAttrValue(result, "distinguishedName");
                if (dn != null && dn.toLowerCase().contains("ou=security") == securityEnabled) {
                    return result;
                }

                String groupType = getAttrValue(result, "groupType");
                if (groupType != null && securityGroupTypes.stream().anyMatch(groupType::equals) == securityEnabled) {
                    return result;
                }
            }

            return null;
        } catch (Exception e) {
            throw new IllegalArgumentException("Error occurred while searching " + e);
        }
    }

    private boolean isMemberOf(MemberOfParams in) {
        boolean result = false;

        try {
            SearchResult user = getUser(in, in.searchBase(), in.user());
            SearchResult group = getGroup(in, in.searchBase(), in.group(), in.securityGroupTypes(), in.securityEnabled());

            if (user != null && group != null) {
                String userDn = getAttrValue(user, "distinguishedName");
                String groupDn = getAttrValue(group, "distinguishedName");

                result = isMemberOf(in, userDn, groupDn);
            }

            return result;
        } catch (Exception e) {
            throw new IllegalArgumentException("Error occurred while searching " + e);
        }
    }

    private NamingEnumeration<SearchResult> search(LdapConnectionCfg cfg, String searchBase, String searchFilter) {
        LdapContext connection = null;
        try {
            connection = establishConnection(cfg);
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

    private LdapContext establishConnection(LdapConnectionCfg cfg) throws NamingException {
        if (cfg.dnsSrvRr() != null) {
            Map<String, Object> dnsSrvRr = cfg.dnsSrvRr();
            String dnsSrvName = assertString(dnsSrvRr, "name");
            String protocol = assertString(dnsSrvRr, "protocol");
            String port = assertString(dnsSrvRr, "port");

            List<String> servers = getLdapServers(dnsSrvName, protocol, port);

            int index = 0;
            while (index < servers.size()) {
                try {
                    return establishConnection(cfg, servers.get(index));
                } catch (CommunicationException ce) {
                    log.warn("Error while establishing connection with ldap AD server: {}, Exception: {}", servers.get(index), ce.getMessage());
                } catch (Exception e) {
                    throw new IllegalArgumentException("Error while establishing connection " + e);
                }
            }
        }

        if (cfg.ldapAdServer() != null) {
            return establishConnection(cfg, cfg.ldapAdServer());
        }

        throw new IllegalArgumentException("Mandatory variable either '" + LDAP_DNS_SRV_RR + "' or '" + LDAP_AD_SERVER + "' is required");
    }


    private LdapContext establishConnection(LdapConnectionCfg cfg, String ldapServer) throws NamingException {
        Hashtable<String, Object> env = new Hashtable<>();
        env.put(javax.naming.Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(javax.naming.Context.PROVIDER_URL, ldapServer);
        env.put(javax.naming.Context.SECURITY_AUTHENTICATION, "simple");
        env.put(javax.naming.Context.SECURITY_PRINCIPAL, cfg.bindUserDn());
        env.put(javax.naming.Context.SECURITY_CREDENTIALS, cfg.bindPassword());
        env.put("java.naming.ldap.version", "3");

        return new InitialLdapContext(env, null);
    }

    private List<String> getLdapServers(String dnsSrvName, String protocol, String port) throws NamingException {
        CopyOnWriteArrayList<String> servers = new CopyOnWriteArrayList<>();
        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
        env.put(Context.PROVIDER_URL, "dns:");
        DirContext ctx = new InitialDirContext(env);
        Attributes srvs = ctx.getAttributes(dnsSrvName, new String[]{"SRV"});
        NamingEnumeration<? extends Attribute> srv = srvs.getAll();
        while (srv.hasMore()) {
            Attribute srvRecords = (Attribute) srv.next();
            NamingEnumeration<?> srvRecord = srvRecords.getAll();
            while (srvRecord.hasMore()) {
                String attr = (String) srvRecord.next();
                servers.add(protocol + "://" + removeLastCharIfDot(attr.split(" ")[3]) + ":" + port);
            }
        }
        return servers;
    }

    private static String removeLastCharIfDot(String s) {
        if (s == null || s.length() == 0 || s.charAt(s.length() - 1) != '.') {
            return s;
        }
        return s.substring(0, s.length() - 1);
    }

    private static String getAttrValue(SearchResult result, String id) {
        NamingEnumeration<String> values = getAttrValues(result, id);

        if (values != null && values.hasMoreElements()) {
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

    /**
     * Executes a Callable until successful, up to a given number of retries
     *
     * @param retryCount    Number of allowed retry attempts
     * @param retryInterval Milliseconds to wait between retries
     * @param c             Callable to execute
     * @param <T>           Type of Callable
     * @throws RuntimeException when api call can't be made successfully
     */
    private static <T> T withRetry(int retryCount, long retryInterval, Callable<T> c) throws Exception {
        Exception exception = null;
        int tryCount = 0;
        while (!Thread.currentThread().isInterrupted() && tryCount <= retryCount) {
            if (tryCount > 0) {
                log.info("Retry after {} sec", retryInterval / 1000);
                sleep(retryInterval);
                log.info("Retrying...");
            }
            try {
                return c.call(); // execute it
            } catch (SSLHandshakeException e) {
                // probably due to self-signed cert that isn't trusted
                log.error("Error during SSL handshake; possibly due to untrusted self-signed certificate." + e.getMessage());
                throw e;
            } catch (Exception e) {
                exception = e;
                log.error("call error", e);
            }
            tryCount++;
        }

        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException();
        }

        // too many attempts, time to give up
        throw new RuntimeException(exception);
    }

    private static Set<String> getAllAttributesValues(Attribute attribute) throws NamingException {
        Set<String> values = new HashSet<>();

        NamingEnumeration<?> ne = attribute.getAll();
        while (ne.hasMore()) {
            Object o = ne.next();
            values.add(o.toString());
        }

        return values;
    }

    private static Map<String, Object> toResult(SearchResult searchResult) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", searchResult != null);
        result.put("result", searchResultToMap(searchResult));
        return result;
    }

    private static void sleep(long t) {
        try {
            Thread.sleep(t);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
