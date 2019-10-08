package com.walmartlabs.concord.plugins.s3;

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

public class PutObjectResult implements Result {

    private static final long serialVersionUID = 1L;

    private final boolean ok;
    private final String eTag;
    private final String contentMd5;

    public PutObjectResult(String eTag, String contentMd5) {
        this.ok = true;
        this.eTag = eTag;
        this.contentMd5 = contentMd5;
    }

    public boolean isOk() {
        return ok;
    }

    public String geteTag() {
        return eTag;
    }

    public String getContentMd5() {
        return contentMd5;
    }
}
