package com.walmartlabs.concord.plugins.puppet;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2020 Walmart Inc.
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

import com.walmartlabs.concord.plugins.puppet.model.cfg.PuppetConfiguration;
import com.walmartlabs.concord.runtime.v2.sdk.SecretService;

import java.util.HashMap;
import java.util.Map;


public class UtilsV2 {

    private UtilsV2() {
        // private constructor to prevent instantiation
    }

    static <T extends PuppetConfiguration> T
    createCfg(Map<String, Object> mergedParams, SecretService ss, Class<T> clazz) {

        // Instantiate a PuppetConfig obj
        T cfg = PuppetConfiguration.parseFromMap(mergedParams, clazz);
        cfg.initializeCertificates(ss::exportAsFile);

        return cfg;
    }


    /**
     * Merges input, default, and method parameters.
     *
     * @param iParams Task input parameters (or public method parameters)
     * @param dParams Default/Global parameters
     * @return Map of merged parameters
     */
    public static Map<String, Object> mergeParams(
            Map<String, Object> iParams,
            Map<String, Object> dParams) {

        // start with default params or an empty map
        Map<String, Object> merged = dParams != null ? dParams : new HashMap<>();

        // add values from input parameters, overriding defaults if value is non-null
        // then add values from public function parameters, overriding existing value
        // finally,
        for (String k : Constants.Keys.getAllInParams()) {
            if (iParams != null) {
                putIntoMap(merged, k, iParams.get(k));
            }
        }

        return merged;
    }

    /**
     * Puts a key-value pair into a given Map if the value is not null
     *
     * @param m Map to store the key-value pair
     * @param k Key to store
     * @param v value to put into the map
     */
    public static void putIntoMap(Map<String, Object> m, String k, Object v) {
        if (v == null) {
            return;
        }

        m.put(k, v);
    }
}
