package com.walmartlabs.concord.plugins.ldap;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2018 Wal-Mart Store, Inc.
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

import com.walmartlabs.concord.runtime.v2.sdk.MapBackedVariables;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class LdapTaskCommonTest {

    @Test
    void escapeFilterValueEscapesRfc4515MetaCharacters() {
        assertEquals("a\\2ab\\28c\\29d\\5ce\\00",
                LdapTaskCommon.escapeFilterValue("a*b(c)d\\e\u0000"));
    }

    @Test
    void taskParamsAllowMissingDefaults() {
        TaskParams params = TaskParams.of(new MapBackedVariables(Map.of(
                "action", "searchByDn",
                "searchBase", "dc=example,dc=com",
                "dn", "cn=user,dc=example,dc=com"
        )), null, null);

        assertNotNull(params);
    }
}
