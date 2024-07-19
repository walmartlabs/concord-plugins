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
import com.walmartlabs.concord.plugins.puppet.model.exception.MissingParameterException;
import com.walmartlabs.concord.plugins.puppet.model.token.TokenPayload;
import com.walmartlabs.concord.sdk.MockContext;
import com.walmartlabs.concord.sdk.SecretService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.net.ssl.SSLHandshakeException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;

class PuppetTaskTest extends AbstractApiTest {

    private PuppetTask task;

    private final SecretService secretService = Mockito.mock(SecretService.class);

    @BeforeEach
    public void setup() throws Exception {
        stubForDbQuery();
        stubForTokenCreate();

        Mockito.doAnswer(invocationOnMock -> getWiremockCertFile().toString())
                .when(secretService)
                .exportAsFile(any(), anyString(), anyString(), anyString(), anyString(), nullable(String.class));

        task = new PuppetTask();
        task.secretService = secretService;
    }


    @Test
    void testQuery() throws Exception {
        MockContext ctx = new MockContext(buildDbQueryConfig());

        UtilsTest.injectVariable(task, "action", "pql");
        ctx.setVariable("queryString", "inventory[certname]{ limit 10 }");

        task.execute(ctx);

        var result = assertInstanceOf(Map.class, ctx.getVariable("result"));
        assertNotNull(result);
        assertTrue((boolean)result.get("ok"));
        List<?> data = (List<?>) result.get("data");
        assertEquals(10, data.size());
    }

    @Test
    void testBadUrl() {
        MockContext ctx = new MockContext(buildDbQueryConfig());

        UtilsTest.injectVariable(task, "action", "pql");
        ctx.setVariable("queryString", "inventory[certname]{ limit 10 }");

        // Empty URL
        ctx.setVariable("databaseUrl", "");
        var expectedMissingParam = assertThrows(MissingParameterException.class, () -> task.execute(ctx));
        assertTrue(expectedMissingParam.getMessage().contains("Cannot find value for databaseUrl"));


        // Invalid URL
        ctx.setVariable("databaseUrl", "notaurl");
        var expectedIllegalArg = assertThrows(IllegalArgumentException.class, () -> task.execute(ctx));
        assertTrue(expectedIllegalArg.getMessage().contains("URI with undefined scheme"));
    }

    @Test
    void testIgnoreErrors() throws Exception {
        MockContext ctx = new MockContext(buildDbQueryConfig());

        // Set ignoreErrors = true
        UtilsTest.injectVariable(task, "action", "bad_action");
        UtilsTest.injectVariable(task, "ignoreErrors", true);

        // Normally, this should throw an exception
        task.execute(ctx);

        // Make sure it failed gracefully and has an error message
        var result = assertInstanceOf(Map.class, ctx.getVariable("result"));
        assertNotNull(result);
        assertFalse((Boolean)result.get("ok"));
        String error = (String) result.get("error");
        assertTrue(error.contains("Not a supported action"));
    }

    @Test
    void testMissingAction() {
        MockContext ctx = new MockContext(buildDbQueryConfig());

        UtilsTest.injectVariable(task, "action", null);
        var expected = assertThrows(MissingParameterException.class, () -> task.execute(ctx));
        assertTrue(expected.getMessage().contains("action"));
    }

    @Test
    void testSelfSignedCertWithPath() throws Exception {
        MockContext ctx = new MockContext(buildDbQueryConfig());

        UtilsTest.injectVariable(task, "action", "pql");
        ctx.setVariable("queryString", "inventory[certname]{ limit 10 }");
        ctx.setVariable(Constants.Keys.DATABASE_URL_KEY, httpsRule.baseUrl());

        // Self-signed cert will fail unless we provide a cert to trust
        assertThrows(SSLHandshakeException.class, () -> task.execute(ctx));


        Map<String, Object> certificate = new HashMap<>();

        certificate.put("path", getWiremockCertFile().toString());
        ctx.setVariable("certificate", certificate);

        // now it should work
        task.execute(ctx);

        var result = assertInstanceOf(Map.class, ctx.getVariable("result"));
        assertNotNull(result);
        assertTrue((boolean)result.get("ok"));
        List<?> data = (List<?>) result.get("data");
        assertEquals(10, data.size());
    }

    @Test
    void testSelfSignedCertWithText() throws Exception {
        MockContext ctx = new MockContext(buildDbQueryConfig());

        UtilsTest.injectVariable(task, "action", "pql");
        ctx.setVariable("queryString", "inventory[certname]{ limit 10 }");
        ctx.setVariable(Constants.Keys.DATABASE_URL_KEY, httpsRule.baseUrl());

        // Self-signed cert will fail unless we provide a cert to trust
        assertThrows(SSLHandshakeException.class, () -> task.execute(ctx));


        Map<String, Object> certificate = new HashMap<>();

        certificate.put("text", getWiremockCertString());
        ctx.setVariable("certificate", certificate);

        // now it should work
        task.execute(ctx);

        var result = assertInstanceOf(Map.class, ctx.getVariable("result"));
        assertNotNull(result);
        assertTrue((boolean)result.get("ok"));
        List<?> data = (List<?>) result.get("data");
        assertEquals(10, data.size());
    }

    @Test
    void testSelfSignedCertWithSecret() throws Exception {
        MockContext ctx = new MockContext(buildDbQueryConfig());

        UtilsTest.injectVariable(task, "action", "pql");
        ctx.setVariable("queryString", "inventory[certname]{ limit 10 }");
        ctx.setVariable(Constants.Keys.DATABASE_URL_KEY, httpsRule.baseUrl());

        // Self-signed cert will fail unless we provide a cert to trust
        assertThrows(SSLHandshakeException.class, () -> task.execute(ctx));


        Map<String, Object> certificate = new HashMap<>();
        Map<String, Object> secret = new HashMap<>();
        secret.put(Constants.Keys.CERTIFICATE_ORG_KEY, "org");
        secret.put(Constants.Keys.CERTIFICATE_NAME_KEY, "name");
        secret.put(Constants.Keys.CERTIFICATE_PASSWORD_KEY, null);

        certificate.put("secret", secret);
        ctx.setVariable("certificate", certificate);

        // now it should work
        task.execute(ctx);

        var result = assertInstanceOf(Map.class, ctx.getVariable("result"));
        assertNotNull(result);
        assertTrue((boolean)result.get("ok"));
        List<?> data = (List<?>) result.get("data");
        assertEquals(10, data.size());
    }

    @Test
    void testNoCertValidation() throws Exception {
        MockContext ctx = new MockContext(buildDbQueryConfig());

        UtilsTest.injectVariable(task, "action", "pql");
        ctx.setVariable("queryString", "inventory[certname]{ limit 10 }");
        ctx.setVariable(Constants.Keys.DATABASE_URL_KEY, httpsRule.baseUrl());

        // Self-signed cert will fail unless we provide a cert to trust
        assertThrows(SSLHandshakeException.class, () -> task.execute(ctx));


        ctx.setVariable(Constants.Keys.VALIDATE_CERTS_KEY, false);

        // now it should work
        task.execute(ctx);

        var result = assertInstanceOf(Map.class, ctx.getVariable("result"));
        assertNotNull(result);
        assertTrue((boolean)result.get("ok"));
        List<?> data = (List<?>) result.get("data");
        assertEquals(10, data.size());
    }

    @Test
    void testTokenCreate() throws Exception {
        MockContext ctx = new MockContext(buildRbacCfg());

        UtilsTest.injectVariable(task, "action", "createApiToken");
        task.execute(ctx);

        var result = assertInstanceOf(Map.class, ctx.getVariable("result"));
        assertTrue((Boolean) result.get("ok"));
        String token = (String) result.get("data");
        assertNotNull(token);
    }

    @Test
    void testTokenCreateAllParams() throws Exception {
        MockContext ctx = new MockContext(buildRbacCfg());

        UtilsTest.injectVariable(task, "action", "createApiToken");
        ctx.setVariable("tokenDescription", "token description");
        ctx.setVariable("tokenLabel", "token label");
        ctx.setVariable("tokenLife", "5h");

        task.execute(ctx);

        var result = assertInstanceOf(Map.class, ctx.getVariable("result"));
        assertTrue((Boolean) result.get("ok"));
        String token = (String) result.get("data");
        assertNotNull(token);
    }

    @Test
    void testTokenPayload() throws Exception {
        MockContext ctx = new MockContext(buildRbacCfg());

        ctx.setVariable("tokenDescription", "token description");
        ctx.setVariable("tokenLabel", "token label");
        ctx.setVariable("tokenLife", "5h");

        RbacCfg cfg = Utils.createCfg(ctx, secretService, ctx.toMap(), RbacCfg.class);
        TokenPayload payload = new TokenPayload(cfg);

        assertEquals("token description", payload.getDescription());
        assertEquals("token label", payload.getLabel());
        assertEquals("5h", payload.getLifetime());
    }

    private Map<String, Object> buildRbacCfg() throws Exception {
        Map<String, Object> cfg = new HashMap<>();
        cfg.put("txId", "cd729813-e93f-4e0a-9856-dc8e65bdd9df");
        cfg.put("workDir", System.getProperty("user.dir"));
        cfg.put(Constants.Keys.RBAC_URL_KEY, httpsRule.baseUrl());
        cfg.put(Constants.Keys.USERNAME_KEY, "fake-username");
        cfg.put(Constants.Keys.PASSWORD_KEY, "fake-password");
        Map<String, Object> certificate = new HashMap<>();
        certificate.put("path", getWiremockCertFile().toString());
        cfg.put("certificate", certificate);
        return cfg;
    }

    private Map<String, Object> buildDbQueryConfig() {
        Map<String, Object> cfg = new HashMap<>();
        cfg.put(Constants.Keys.DATABASE_URL_KEY, httpRule.baseUrl());
        cfg.put(Constants.Keys.API_TOKEN_KEY, "23w4der5f6tg7yh8uj9iko");
        cfg.put("txId", "cd729813-e93f-4e0a-9856-dc8e65bdd9df");
        cfg.put("workDir", System.getProperty("user.dir"));
        return cfg;
    }

}
