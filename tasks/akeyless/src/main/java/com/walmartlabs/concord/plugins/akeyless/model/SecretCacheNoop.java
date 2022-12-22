package com.walmartlabs.concord.plugins.akeyless.model;

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

import java.util.function.Supplier;

public class SecretCacheNoop<T extends Secret> implements SecretCache<T> {

    private static final SecretCacheNoop<Secret.StringSecret> stringCache = new SecretCacheNoop<>();
    private static final SecretCacheNoop<Secret.CredentialsSecret> credentialCache = new SecretCacheNoop<>();

    public static SecretCache<Secret.StringSecret> getStringCache() {
        return stringCache;
    }

    public static SecretCache<Secret.CredentialsSecret> getCredentialCache() {
        return credentialCache;
    }

    @Override
    public T get(String org, String name, Supplier<T> lookup) {
        return lookup.get();
    }

    @Override
    public void put(String org, String name, Secret value) {
        // no cache in noop
    }
}
