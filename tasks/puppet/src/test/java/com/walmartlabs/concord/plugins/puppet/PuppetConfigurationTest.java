package com.walmartlabs.concord.plugins.puppet;

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

import com.walmartlabs.concord.plugins.puppet.model.cfg.RbacCfg;
import com.walmartlabs.concord.plugins.puppet.model.exception.InvalidValueException;
import com.walmartlabs.concord.plugins.puppet.model.exception.MissingParameterException;
import com.walmartlabs.concord.sdk.MockContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class PuppetConfigurationTest {

    MockContext ctx;

    @BeforeEach
    public void setup() {
        ctx = new MockContext(buildCfg());
    }

    @Test
    public void testBadMap() {
        // set map parameter to a non-map value
        ctx.setVariable("certificate", "not a map");

        try {
            Utils.createCfg(ctx, null, ctx.toMap(), RbacCfg.class);
            fail("Exception should be thrown when expected map is not a map.");
        } catch (InvalidValueException expected) {
            assertTrue(expected.getMessage().contains("Unable to convert to Map"));
        } catch (Exception ex) {
            fail("Expected an InvalidValueException, not this one: " + ex.getMessage());
        }
    }

    @Test
    public void testNullRequiredValue() {
        //
        ctx.setVariable("rbacUrl", null);

        try {
            Utils.createCfg(ctx, null, ctx.toMap(), RbacCfg.class);
            fail("Exception should be thrown when required value is null.");
        } catch (MissingParameterException expected) {
            assertTrue(expected.getMessage().contains("Cannot find value for rbacUrl"));
        } catch (Exception ex) {
            fail("Expected an MissingParameterException, not this one: " + ex.getMessage());
        }

    }

    private Map<String, Object> buildCfg() {
        Map<String, Object> cfg = new HashMap<>();
        cfg.put(Constants.Keys.RBAC_URL_KEY, "https://example.com");
        cfg.put(Constants.Keys.USERNAME_KEY, "fake-username");
        cfg.put(Constants.Keys.PASSWORD_KEY, "fake-password");
        return cfg;
    }
}
