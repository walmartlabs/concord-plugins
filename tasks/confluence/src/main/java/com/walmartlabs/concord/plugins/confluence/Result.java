package com.walmartlabs.concord.plugins.confluence;

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

public class Result implements Serializable {

    public static Result ok(Long pageId, Long childId, String data) {
        return new Result(true, pageId, childId, data, null);
    }

    public static Result error(String error) {
        return new Result(false, null, null, null, error);
    }

    public final boolean ok;
    public final Long pageId;
    public final Long childId;
    public final String data;
    public final String error;

    public Result(boolean ok, Long pageId, Long childId, String data, String error) {
        this.ok = ok;
        this.pageId = pageId;
        this.childId = childId;
        this.data = data;
        this.error = error;
    }

    public boolean isOk() {
        return ok;
    }

    public Long getpageId() {
        return pageId;
    }

    public Long getChildId() {
        return childId;
    }

    public String getData() {
        return data;
    }

    public String getError() {
        return error;
    }
}
