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

public class SecretCacheImpl implements SecretCache {
    private static final Logger log = LoggerFactory.getLogger(SecretCacheImpl.class);
    private static SecretCacheImpl instance;

    private final String salt;
    private final Map<String, String> data;
    private final boolean debug;

    public static synchronized SecretCache getInstance(String salt, boolean debug) {
        if (instance == null) {
            instance = new SecretCacheImpl(salt, debug);
        }

        if (instance.isDirty(salt)) {
            log.warn("Secret cache is dirty. Re-initializing");
            instance = new SecretCacheImpl(salt, debug);
        }

        return instance;
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
    public String get(String org, String name, Supplier<String> lookup) {
        final String cacheKey = buildKey(org, name, salt);
        final String hash = Util.hash(cacheKey);

        return data.computeIfAbsent(hash, k -> {
            Util.debug(debug, log, String.format("secret cache miss: %s/%s", org, name));
            return lookup.get();
        });
    }

    @Override
    public void put(String org, String name, String value) {
        final String cacheKey = buildKey(org, name, salt);

        data.put(Util.hash(cacheKey), value);
    }

    private static String buildKey(String org, String name, String salt) {
        return String.format("%s/%s/%s", org, salt, name);
    }
}
