/**
 * Concord
 * -----
 * Copyright (C) 2017 - 2021 Walmart Inc.
 * -----
 */
package com.walmartlabs.concord.plugins.argocd;

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

import com.walmartlabs.concord.common.ConfigurationUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class MapUtils {

    @SuppressWarnings("unchecked")
    public static <T> T get(Map<String, Object> m, String path, T defaultValue) {
        String[] paths = path.split("\\.");

        boolean has = ConfigurationUtils.has(m, paths);
        if (!has) {
            return defaultValue;
        }

        T result = (T) ConfigurationUtils.get(m, paths);
        if (result != null) {
            return result;
        }
        return defaultValue;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> set(Map<String, Object> m, String path, Object v) {
        Map<String, Object> result = new HashMap<>(m);
        m = result;

        String[] paths = path.split("\\.");
        for (int i = 0; i < paths.length - 1; i++) {
            String p = paths[i];
            Object mv = m.getOrDefault(p, Collections.emptyMap());
            if (!(mv instanceof Map)) {
                throw new IllegalArgumentException("Value should be a Map: " + String.join("/", Arrays.copyOfRange(paths, 0, i)));
            }

            Map<String, Object> mm = new HashMap<>((Map<String, Object>) mv);
            m.put(p, mm);

            m = mm;
        }
        m.put(paths[paths.length - 1], v);

        return result;
    }

    private MapUtils() {
    }
}
