package com.walmartlabs.concord.plugins.puppet.model.token;

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
import com.walmartlabs.concord.plugins.puppet.model.cfg.RbacCfg;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TokenPayload {

    private String login;
    private String password;
    private String lifetime; // optional
    private String description; // optional
    private String label; // optional

    public TokenPayload(RbacCfg rbacCfg) {

        this.login = rbacCfg.getUsername();
        this.password = rbacCfg.getPassword();
        this.lifetime = rbacCfg.getTokenLifetime();
        this.label = rbacCfg.getLabel();
        this.description = rbacCfg.getDescription();
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    public String getLifetime() {
        return lifetime;
    }

    public String getDescription() {
        return description;
    }

    public String getLabel() {
        return label;
    }
}
