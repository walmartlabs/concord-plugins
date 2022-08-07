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

public class SecretCacheNoop implements SecretCache {

    private final static SecretCache instance = new SecretCacheNoop();

    public static SecretCache getInstance() {
        return instance;
    }

    @Override
    public String get(String key, Supplier<String> lookup) {
        return lookup.get();
    }

    @Override
    public void put(String key, String value) {
        // no cache in noop
    }
}
