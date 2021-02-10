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
import java.util.HashMap;
import java.util.Map;

public class HashiVaultTaskResult {
    private final Map<String, String> data;
    private final Boolean ok;
    private final String error;

    public HashiVaultTaskResult(boolean ok, Map<String, String> result, String error) {
        this.ok = ok;
        this.data = result;
        this.error = error;
    }

    public Map<String, Object> data() {
        if (data == null) {
            return Collections.emptyMap();
        }

        HashMap<String, Object> d = new HashMap<>(data.size());
        d.putAll(data);

        return d;
    }

    public boolean ok() {
        return ok;
    }

    public String error() {
        return error;
    }

    /**
     * @return Entire object as a Map
     */
    public Map<String, Object> toMap() {
        Map<String, Object> m = new HashMap<>(3);
        m.put("ok", ok);
        m.put("data", data);
        m.put("error", error);
        return m;
    }
}
