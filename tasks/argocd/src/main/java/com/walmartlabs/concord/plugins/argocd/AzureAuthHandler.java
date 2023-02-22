package com.walmartlabs.concord.plugins.argocd;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2023 Walmart Inc., Concord Authors
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

import com.microsoft.aad.msal4j.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.util.Set;

public class AzureAuthHandler {

    private final static Logger log = LoggerFactory.getLogger(AzureAuthHandler.class);

    public static String auth(TaskParams.AzureAuth auth) throws Exception {
        PublicClientApplication pca = getPca(AzureAuthCache.getInstance().getPca(), auth.clientId(), auth.authority());

        IAccount account = getAccountByUsername(AzureAuthCache.getInstance().getIAccounts(), auth.username());

        /*Attempt to acquire token*/
        IAuthenticationResult result = acquireTokenUsernamePassword(pca, auth.scope(), account, auth.username(), auth.password());

        Set<IAccount> accounts = pca.getAccounts().join();
        AzureAuthCache.getInstance().putIAccounts(accounts);
        AzureAuthCache.getInstance().setPca(pca);

        return result.idToken();
    }

    private static IAuthenticationResult acquireTokenUsernamePassword(PublicClientApplication pca,
                                                                      Set<String> scope,
                                                                      IAccount account,
                                                                      String username,
                                                                      String password) throws Exception {
        IAuthenticationResult result;
        try {
            SilentParameters silentParameters =
                    SilentParameters
                            .builder(scope)
                            .account(account)
                            .build();

        /*  Try to acquire token silently. This will fail on the first acquireTokenUsernamePassword() call
            because the token cache does not have any data for the user */
            result = pca.acquireTokenSilently(silentParameters).join();
            log.info("==acquireTokenSilently call succeeded:- username: {}", result.account().username());
        } catch (Exception ex) {
            if (ex.getCause() instanceof MsalException) {
                UserNamePasswordParameters parameters =
                        UserNamePasswordParameters
                                .builder(scope, username, password.toCharArray())
                                .build();

            /*  Try to acquire a token via username/password */
                result = pca.acquireToken(parameters).join();
                log.info("==username/password flow succeeded:- username: {}", result.account().username());
            } else {
                throw ex;
            }
        }
        return result;
    }

    /**
     * Helper function to return an account from a given set of accounts based on the given username,
     * or return null if no accounts in the set match
     */
    private static IAccount getAccountByUsername(Set<IAccount> accounts, String username) {
        if (accounts == null || accounts.isEmpty()) {
            return null;
        }

        for (IAccount account : accounts) {
            if (account.username().equalsIgnoreCase(username)) {
                return account;
            }
        }

        return null;
    }

    /**
     * Helper function to return pca from cache,
     * or return fresh pca if no pca in the set match the clientId & authority
     */
    private static PublicClientApplication getPca(PublicClientApplication pca, String clientId, String authority) throws MalformedURLException {
        if (pca != null && pca.clientId().equalsIgnoreCase(clientId) && removeLastCharIfSlash(pca.authority()).equalsIgnoreCase(removeLastCharIfSlash(authority))) {
            return pca;
        }
        return PublicClientApplication.builder(clientId)
                .authority(authority)
                .build();
    }

    private static String removeLastCharIfSlash(String s) {
        if (s == null || s.length() == 0 || s.charAt(s.length() - 1) != '/') {
            return s;
        }
        return s.substring(0, s.length() - 1);
    }
}
