package com.walmartlabs.concord.plugins.jira;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2024 Walmart Inc., Concord Authors
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JiraHttpClientFactory {

    private static final Logger log = LoggerFactory.getLogger(JiraHttpClientFactory.class);

    private JiraHttpClientFactory() {
        throw new IllegalStateException("instantiation is not allowed");
    }

    public static JiraHttpClient create(JiraClientCfg cfg) {
        try {
            return new NativeJiraHttpClient(cfg);
        } catch (NoClassDefFoundError e) {
            // client2 may not exist
            log.info("Falling back to okhttp client");
        }

        try {
            return new JiraClient(cfg);
        } catch (Exception e) {
            // that's very unexpected as long as okhttp is still allowed
            throw new IllegalStateException("No jira http client found");
        }
    }

}
