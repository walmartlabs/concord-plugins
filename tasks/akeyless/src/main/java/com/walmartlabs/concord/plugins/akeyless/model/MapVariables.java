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

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MapVariables {

    private final Map<String, Object> values;

    public MapVariables(Map<String, Object> values) {
        this.values = values != null ? values : Collections.emptyMap();
    }

    public boolean has(String key) {
        return values.containsKey(key);
    }

    public Object get(String key) {
        return values.get(key);
    }

    public String getString(String key) {
        Object value = values.get(key);
        return value != null ? value.toString() : null;
    }

    public String getString(String key, String defaultValue) {
        String value = getString(key);
        return value != null ? value : defaultValue;
    }

    public String assertString(String key) {
        String value = getString(key);
        if (value == null) {
            throw new IllegalArgumentException("Required string parameter '" + key + "' is missing");
        }
        return value;
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        Object value = values.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return Boolean.parseBoolean(value.toString());
    }

    public boolean assertBoolean(String key) {
        if (!values.containsKey(key)) {
            throw new IllegalArgumentException("Required boolean parameter '" + key + "' is missing");
        }
        return getBoolean(key, false);
    }

    public Long assertLong(String key) {
        Object value = values.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Required long parameter '" + key + "' is missing");
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.parseLong(value.toString());
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> assertMap(String key) {
        Object value = values.get(key);
        if (!(value instanceof Map)) {
            throw new IllegalArgumentException("Required map parameter '" + key + "' is missing");
        }
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    public List<String> assertList(String key) {
        Object value = values.get(key);
        if (!(value instanceof List)) {
            throw new IllegalArgumentException("Required list parameter '" + key + "' is missing");
        }
        return (List<String>) value;
    }

    @SuppressWarnings("unchecked")
    public List<String> getList(String key, List<String> defaultValue) {
        Object value = values.get(key);
        return value != null ? (List<String>) value : defaultValue;
    }

    public <T> T get(String key, T defaultValue, Class<T> type) {
        Object value = values.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (!type.isInstance(value)) {
            throw new IllegalArgumentException("Parameter '" + key + "' must be a " + type.getSimpleName());
        }
        return type.cast(value);
    }
}
