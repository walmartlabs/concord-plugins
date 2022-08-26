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

import com.walmartlabs.concord.plugins.puppet.model.cfg.DbQueryCfg;
import com.walmartlabs.concord.plugins.puppet.model.dbquery.DbQueryPayload;
import com.walmartlabs.concord.plugins.puppet.model.exception.ConfigException;
import com.walmartlabs.concord.sdk.MockContext;
import com.walmartlabs.concord.sdk.SecretService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.*;

public class ConfigurationTest {
    private static final Logger log = LoggerFactory.getLogger(ConfigurationTest.class);

    private MockContext ctx;

    @BeforeEach
    public void setup() {
        Map<String, Object> cfg = new HashMap<>();
        cfg.put(Constants.Keys.DATABASE_URL_KEY, "https://example.com");
        cfg.put(Constants.Keys.API_TOKEN_KEY, "23w4der5f6tg7yh8uj9iko");
        cfg.put("txId", "cd729813-e93f-4e0a-9856-dc8e65bdd9df");
        cfg.put("workDir", System.getProperty("user.dir"));

        ctx = new MockContext(cfg);
    }

    @Test
    public void testReadCertFileError() {

        Map<String, Object> certificate = new HashMap<>();
        certificate.put("path", "does/not/exit");

        ctx.setVariable("queryString", "test query");
        ctx.setVariable("certificate", certificate);

        try {
            Utils.createCfg(ctx, null, ctx.toMap(), DbQueryCfg.class);
            fail("Missing certificate file should cause an exception.");
        } catch (ConfigException expected) {
            log.info("Hit expected exception with missing cert file.");
        } catch (Exception e) {
            fail("Missing certificate file should cause an exception, not this one: " + e.getMessage());
        }
    }

    @Test
    public void testBadCertText() {
        Map<String, Object> certificate = new HashMap<>();
        certificate.put("text", "not really a cert");

        ctx.setVariable("queryString", "test query");
        ctx.setVariable("certificate", certificate);

        try {
            Utils.createCfg(ctx, null, ctx.toMap(), DbQueryCfg.class);
            fail("Bad certificate text should cause an exception.");
        } catch (ConfigException expected) {
            log.info("Hit expected exception with missing cert file.");
        } catch (Exception e) {
            fail("Bad certificate text should cause an exception, but not this one: " + e.getMessage());
        }

    }

    @Test
    public void testCertFromSecretError() throws Exception {
        Map<String, Object> secret = new HashMap<>();
        secret.put("org", "o");
        secret.put("name", "n");
        secret.put("password", null);

        SecretService sService = Mockito.mock(SecretService.class);
        Mockito.doAnswer(invocationOnMock -> "not/a/good/path")
                .when(sService)
                .exportAsFile(any(), anyString(), anyString(), anyString(), anyString(), nullable(String.class));


        Map<String, Object> certificate = new HashMap<>();
        certificate.put("secret", secret);

        ctx.setVariable("queryString", "test query");
        ctx.setVariable("certificate", certificate);

        try {
            Utils.createCfg(ctx, sService, ctx.toMap(), DbQueryCfg.class);
            fail("Bad certificate text should cause an exception.");
        } catch (ConfigException expected) {
            log.info("Hit expected exception with missing cert file.");
        } catch (Exception e) {
            fail("Bad certificate text should cause an exception, but not this one: " + e.getMessage());
        }
    }

    @Test
    public void payloadTest() {
        ctx.setVariable("queryString", "test query");

        DbQueryCfg cfg = Utils.createCfg(ctx, null, ctx.toMap(), DbQueryCfg.class);
        DbQueryPayload payload = new DbQueryPayload(cfg);

        assertEquals("test query", payload.getQuery());

        payload.setQuery("new query");

        assertEquals("new query", payload.getQuery());
    }
}
