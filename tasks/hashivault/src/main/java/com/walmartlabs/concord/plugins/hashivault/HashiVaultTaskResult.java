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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class HashiVaultTaskResult {
    private final Boolean ok;
    private final String error;
    private final Object data;

    private HashiVaultTaskResult(boolean ok, Object data, String error) {
        this.ok = ok;
        this.error = error;
        this.data = data;
    }

    public static HashiVaultTaskResult of(boolean ok, Map<String, String> data, String error, TaskParams p) {
        if (Optional.ofNullable(p).filter(TaskParams::hasKeyField).isPresent()) {
            return new HashiVaultTaskResult(ok, data.get(p.key()), error);
        }

        return new HashiVaultTaskResult(ok, data, error);
    }

    @SuppressWarnings("unchecked")
    public <T> T data() {
        return (T) this.data;
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
