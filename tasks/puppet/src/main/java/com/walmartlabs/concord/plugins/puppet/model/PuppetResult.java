package com.walmartlabs.concord.plugins.puppet.model;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2019 Walmart Inc.
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

import java.io.Serializable;

public class PuppetResult implements Serializable {
    private final boolean ok;
    private final Object data;
    private final String error;

    public PuppetResult(boolean ok, Object data, String error) {
        this.ok = ok;
        this.data = data;
        this.error = error;
    }

    public boolean isOk() {
        return ok;
    }

    public Object getData() {
        return data;
    }

    public String getError() {
        return error;
    }
}
