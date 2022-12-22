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

import com.walmartlabs.concord.plugins.akeyless.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class SecretCacheImpl<T extends Secret> implements SecretCache<T> {
    private static final Logger log = LoggerFactory.getLogger(SecretCacheImpl.class);
    private static SecretCacheImpl<Secret.StringSecret> stringCache;
    private static SecretCacheImpl<Secret.CredentialsSecret> credentialCache;

    private final String salt;
    private final Map<String, T> data;
    private final boolean debug;

    public static synchronized SecretCache<Secret.StringSecret> getStringCache(String salt, boolean debug) {
        if (stringCache == null) {
            stringCache = new SecretCacheImpl<>(salt, debug);
        }

        if (stringCache.isDirty(salt)) {
            log.warn("String secret cache is dirty. Re-initializing");
            stringCache = new SecretCacheImpl<>(salt, debug);
        }

        return stringCache;
    }

    public static synchronized SecretCache<Secret.CredentialsSecret> getCredentialCache(String salt, boolean debug) {
        if (credentialCache == null) {
            credentialCache = new SecretCacheImpl<>(salt, debug);
        }

        if (credentialCache.isDirty(salt)) {
            log.warn("String secret cache is dirty. Re-initializing");
            credentialCache = new SecretCacheImpl<>(salt, debug);
        }

        return credentialCache;
    }

    private SecretCacheImpl(String s, boolean debug) {
        this.salt = s;
        this.data = new ConcurrentHashMap<>();
        this.debug = debug;
    }

    public boolean isDirty(String s) {
        return this.salt == null || !(this.salt.equals(s));
    }

    @Override
    public T get(String org, String name, Supplier<T> lookup) {
        final String cacheKey = buildKey(org, name, salt);
        final String hash = Util.hash(cacheKey);

        return data.computeIfAbsent(hash, k -> {
            Util.debug(debug, log, String.format("secret cache miss: %s/%s", org, name));
            return lookup.get();
        });
    }

    @Override
    public void put(String org, String name, T value) {
        final String cacheKey = buildKey(org, name, salt);

        data.put(Util.hash(cacheKey), value);
    }

    private static String buildKey(String org, String name, String salt) {
        return String.format("%s/%s/%s", org, salt, name);
    }
}
