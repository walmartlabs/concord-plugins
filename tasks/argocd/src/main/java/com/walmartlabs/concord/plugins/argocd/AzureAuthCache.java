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

import com.microsoft.aad.msal4j.IAccount;
import com.microsoft.aad.msal4j.PublicClientApplication;

import java.util.HashSet;
import java.util.Set;

public class AzureAuthCache {

    private final Set<IAccount> accountsInCache;
    private PublicClientApplication pca;

    private static final AzureAuthCache azureAuthCache = new AzureAuthCache();


    public static AzureAuthCache getInstance() {
        return azureAuthCache;
    }

    private AzureAuthCache() {
        accountsInCache = new HashSet<>();
        pca = null;
    }

    public void putIAccounts(Set<IAccount> accounts) {
        accountsInCache.addAll(accounts);
    }

    public Set<IAccount> getIAccounts() {
        return accountsInCache;
    }

    public void setPca(PublicClientApplication pca) {
        this.pca = pca;
    }

    public PublicClientApplication getPca() {
        return this.pca;
    }

    public void clear() {
        accountsInCache.clear();
        pca = null;
    }
}
