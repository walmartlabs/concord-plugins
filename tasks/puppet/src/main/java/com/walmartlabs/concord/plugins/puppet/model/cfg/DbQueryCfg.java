package com.walmartlabs.concord.plugins.puppet.model.cfg;

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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.walmartlabs.concord.plugins.puppet.Constants;
import com.walmartlabs.concord.plugins.puppet.Constants.Keys;

import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class DbQueryCfg extends PuppetConfiguration {

    /**
     * PuppetDB API Url
     */
    @JsonProperty(value = Keys.DATABASE_URL_KEY, required = true)
    private String dbUrl;

    /**
     * API token for authorization
     */
    @JsonProperty(value = Keys.API_TOKEN_KEY, required = true)
    private String apiToken;

    /**
     * PQL query string to execute
     */
    @JsonProperty(value = Keys.QUERY_STRING_KEY, required = true)
    private String queryString;

    public DbQueryCfg() {
    }

    public Map<String, String> getHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.ApiConst.AUTHENTICATION_TOKEN, apiToken);

        return headers;
    }

    public String getBaseUrl() {
        return this.dbUrl;
    }

    public String getApiToken() {
        return this.apiToken;
    }

    public String getQueryString() {
        return queryString;
    }
}
