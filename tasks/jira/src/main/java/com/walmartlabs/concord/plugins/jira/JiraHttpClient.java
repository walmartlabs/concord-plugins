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

import java.io.File;
import java.io.IOException;
import java.util.Map;

public interface JiraHttpClient {
    JiraHttpClient url(String url);

    JiraHttpClient successCode(int successCode);

    JiraHttpClient jiraAuth(String auth);

    Map<String, Object> get() throws IOException;

    Map<String, Object> post(Map<String, Object> data) throws IOException;

    void post(File file) throws IOException;

    void put(Map<String, Object> data) throws IOException;

    void delete() throws IOException;

    static void assertResponseCode(int code, String result, int successCode) {
        if (code == successCode) {
            return;
        }

        if (code == 400) {
            throw new IllegalStateException("input is invalid (e.g. missing required fields, invalid values). Here are the full error details: " + result);
        } else if (code == 401) {
            throw new IllegalStateException("User is not authenticated. Here are the full error details: " + result);
        } else if (code == 403) {
            throw new IllegalStateException("User does not have permission to perform request. Here are the full error details: " + result);
        } else if (code == 404) {
            throw new IllegalStateException("Issue does not exist. Here are the full error details: " + result);
        } else if (code == 500) {
            throw new IllegalStateException("Internal Server Error. Here are the full error details" + result);
        } else {
            throw new IllegalStateException("Error: " + result);
        }
    }
}
