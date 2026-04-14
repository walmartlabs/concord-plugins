package com.walmartlabs.concord.plugins.hashivault;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2021 Walmart Inc., Concord Authors
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

import java.util.Collections;
import java.util.Map;

class MapVariables {

    private final Map<String, Object> values;

    MapVariables(Map<String, Object> values) {
        this.values = values != null ? values : Collections.emptyMap();
    }

    boolean has(String key) {
        return values.containsKey(key);
    }

    String getString(String key) {
        Object value = values.get(key);
        return value != null ? value.toString() : null;
    }

    String getString(String key, String defaultValue) {
        String value = getString(key);
        return value != null ? value : defaultValue;
    }

    String assertString(String key) {
        String value = getString(key);
        if (value == null) {
            throw new IllegalArgumentException("'" + key + "' is required");
        }
        return value;
    }

    boolean getBoolean(String key, boolean defaultValue) {
        Object value = values.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return Boolean.parseBoolean(value.toString());
    }

    Number getNumber(String key, Number defaultValue) {
        Object value = values.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return (Number) value;
        }
        return Long.parseLong(value.toString());
    }

    int getInt(String key, int defaultValue) {
        return getNumber(key, defaultValue).intValue();
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> assertMap(String key) {
        Object value = values.get(key);
        if (!(value instanceof Map)) {
            throw new IllegalArgumentException("'" + key + "' is required");
        }
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> getMap(String key, Map<String, Object> defaultValue) {
        Object value = values.get(key);
        return value != null ? (Map<String, Object>) value : defaultValue;
    }
}
