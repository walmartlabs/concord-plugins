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
import com.walmartlabs.concord.plugins.puppet.Constants.Keys;

import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class RbacCfg extends PuppetConfiguration {

    /** PuppetDB API Url */
    @JsonProperty(value = Keys.RBAC_URL_KEY, required = true)
    private String rbacUrl;
    @JsonProperty(value = Keys.TOKEN_LIFETIME_KEY, defaultValue = "1h", required = true)
    private String tokenLifetime;
    @JsonProperty(value = Keys.USERNAME_KEY, required = true)
    private String username;
    @JsonProperty(value = Keys.PASSWORD_KEY, required = true)
    private String password;
    @JsonProperty(value = Keys.TOKEN_LABEL_KEY)
    private String label;
    @JsonProperty(value = Keys.TOKEN_DESCRIPTION_KEY)
    private String description;


    public RbacCfg() { }

    public Map<String,String> getHeaders() {
        // no headers for rbac call
        return new HashMap<>();
    }

    @Override
    public String getBaseUrl() {
        return rbacUrl;
    }

    public String getTokenLifetime() {
        return tokenLifetime;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }
}
