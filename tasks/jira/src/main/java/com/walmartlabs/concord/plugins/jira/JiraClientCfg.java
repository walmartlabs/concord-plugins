package com.walmartlabs.concord.plugins.jira;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2020 Walmart Inc., Concord Authors
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

public interface JiraClientCfg {

    default long connectTimeout() {
        return 30L;
    }

    default long readTimeout() {
        return 30L;
    }

    default long writeTimeout() {
        return 30L;
    }

    default HttpVersion httpProtocolVersion() {
        return HttpVersion.DEFAULT;
    }

    enum HttpVersion {
        HTTP_1_1("http/1.1"),
        HTTP_2("http/2.0"),
        DEFAULT("default");

        private final String value;

        HttpVersion(String value) {
            this.value = value;
        }

        public static HttpVersion from(String val) {
            for (HttpVersion version : HttpVersion.values()) {
                if (version.value.equals(val)) {
                    return version;
                }
            }

            return DEFAULT;
        }

    }
}
