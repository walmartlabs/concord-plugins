package com.walmartlabs.concord.plugins.akeyless;

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

import java.util.Map;

public class AkeylessTaskResult {
    private final Boolean ok;
    private final String error;
    private final Map<String, String> data;

    public AkeylessTaskResult(Boolean ok, Map<String, String> data, String error) {
        this.ok = ok;
        this.error = error;
        this.data = data;
    }

    public static AkeylessTaskResult of(boolean ok, Map<String, String> data, String error) {
        return new AkeylessTaskResult(ok, data, error);
    }

    public Boolean getOk() {
        return ok;
    }

    public String getError() {
        return error;
    }

    public Map<String, String> getData() {
        return data;
    }
}
