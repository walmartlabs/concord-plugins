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
import com.walmartlabs.concord.plugins.puppet.model.exception.ApiException;
import com.walmartlabs.concord.plugins.puppet.model.exception.MissingParameterException;
import com.walmartlabs.concord.plugins.puppet.model.token.TokenPayload;
import com.walmartlabs.concord.sdk.MockContext;
import com.walmartlabs.concord.sdk.SecretService;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.net.ssl.*;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;

public class PuppetTaskTest extends AbstractApiTest {

    private PuppetTask task;

    private SecretService secretService = Mockito.mock(SecretService.class);

    @Before
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
    public void testQuery() throws Exception {
        MockContext ctx = new MockContext(buildDbQueryConfig());

        UtilsTest.injectVariable(task, "action", "pql");
        ctx.setVariable("queryString", "inventory[certname]{ limit 10 }");

        task.execute(ctx);

        Map result = (Map) ctx.getVariable("result");
        assertNotNull(result);
        assertTrue((boolean)result.get("ok"));
        List data = (List) result.get("data");
        assertEquals(10, data.size());
    }

    @Test
    public void testBadUrl() {
        MockContext ctx = new MockContext(buildDbQueryConfig());

        UtilsTest.injectVariable(task, "action", "pql");
        ctx.setVariable("queryString", "inventory[certname]{ limit 10 }");

        // Empty URL
        ctx.setVariable("databaseUrl", "");
        try {
            task.execute(ctx);
            fail("Bad url should cause an exception");
        } catch(MissingParameterException expected) {
            assert(expected.getMessage().contains("Cannot find value for databaseUrl"));
        } catch (Exception e) {
            fail("Unexpected exception with bad URL: " + e.getMessage());
        }


        // Invalid URL
        ctx.setVariable("databaseUrl", "notaurl");
        try {
            task.execute(ctx);
            fail("Bad url should cause an exception");
        } catch(IllegalArgumentException expected) {
            assert(expected.getMessage().contains("Invalid URL"));
        } catch (Exception e) {
            fail("Unexpected exception with bad URL: " + e.getMessage());
        }
    }

    @Test
    public void testIgnoreErrors() throws Exception {
        MockContext ctx = new MockContext(buildDbQueryConfig());

        // Set ignoreErrors = true
        UtilsTest.injectVariable(task, "action", "bad_action");
        UtilsTest.injectVariable(task, "ignoreErrors", true);

        // Normally, this should throw an exception
        task.execute(ctx);

        // Make sure it failed gracefully and has an error message
        Map result = (Map) ctx.getVariable("result");
        assertNotNull(result);
        assertFalse((Boolean)result.get("ok"));
        String error = (String) result.get("error");
        assertTrue(error.contains("Not a supported action"));
    }

    @Test
    public void testMissingAction() {
        MockContext ctx = new MockContext(buildDbQueryConfig());

        try {
            UtilsTest.injectVariable(task, "action", null);
            task.execute(ctx);
            fail("Missing action value should cause exception");
        } catch (MissingParameterException expected) {
            // ok
            assertTrue(expected.getMessage().contains("action"));
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }

    }

    @Test
    public void testSelfSignedCertWithPath() throws Exception {
        MockContext ctx = new MockContext(buildDbQueryConfig());

        UtilsTest.injectVariable(task, "action", "pql");
        ctx.setVariable("queryString", "inventory[certname]{ limit 10 }");
        ctx.setVariable(Constants.Keys.DATABASE_URL_KEY, httpsRule.baseUrl());

        // Self-signed cert will fail unless we provide a cert to trust
        try {
            task.execute(ctx);
            throw new Exception("Task should fail when self-signed cert is used without a provided certificate to trust.");
        } catch (SSLHandshakeException expected) {
            // that's fine
            log.info("hit expected ssl exception. Not a problem");
        } catch (Exception e) {
            log.error(e.getMessage());
            throw e;
        }

        Map<String, Object> certificate = new HashMap<>();

        certificate.put("path", getWiremockCertFile().toString());
        ctx.setVariable("certificate", certificate);

        // now it should work
        task.execute(ctx);

        Map result = (Map) ctx.getVariable("result");
        assertNotNull(result);
        assertTrue((boolean)result.get("ok"));
        List data = (List) result.get("data");
        assertEquals(10, data.size());
    }

    @Test
    public void testSelfSignedCertWithText() throws Exception {
        MockContext ctx = new MockContext(buildDbQueryConfig());

        UtilsTest.injectVariable(task, "action", "pql");
        ctx.setVariable("queryString", "inventory[certname]{ limit 10 }");
        ctx.setVariable(Constants.Keys.DATABASE_URL_KEY, httpsRule.baseUrl());

        // Self-signed cert will fail unless we provide a cert to trust
        try {
            task.execute(ctx);
            fail("Task should fail when self-signed cert is used without a provided certificate to trust.");
        } catch (SSLHandshakeException expected) {
            // that's fine
            log.info("hit expected ssl exception. Not a problem");
        } catch (Exception e) {
            log.error(e.getMessage());
            throw e;
        }

        Map<String, Object> certificate = new HashMap<>();

        certificate.put("text", getWiremockCertString());
        ctx.setVariable("certificate", certificate);

        // now it should work
        task.execute(ctx);

        Map result = (Map) ctx.getVariable("result");
        assertNotNull(result);
        assertTrue((boolean)result.get("ok"));
        List data = (List) result.get("data");
        assertEquals(10, data.size());
    }

    @Test
    public void testSelfSignedCertWithSecret() throws Exception {
        MockContext ctx = new MockContext(buildDbQueryConfig());

        UtilsTest.injectVariable(task, "action", "pql");
        ctx.setVariable("queryString", "inventory[certname]{ limit 10 }");
        ctx.setVariable(Constants.Keys.DATABASE_URL_KEY, httpsRule.baseUrl());

        // Self-signed cert will fail unless we provide a cert to trust
        try {
            task.execute(ctx);
            fail("Task should fail when self-signed cert is used without a provided certificate to trust.");
        } catch (SSLHandshakeException expected) {
            // that's fine
            log.info("hit expected ssl exception. Not a problem");
        } catch (Exception e) {
            log.error(e.getMessage());
            throw e;
        }


        Map<String, Object> certificate = new HashMap<>();
        Map<String, Object> secret = new HashMap<>();
        secret.put(Constants.Keys.CERTIFICATE_ORG_KEY, "org");
        secret.put(Constants.Keys.CERTIFICATE_NAME_KEY, "name");
        secret.put(Constants.Keys.CERTIFICATE_PASSWORD_KEY, null);

        certificate.put("secret", secret);
        ctx.setVariable("certificate", certificate);

        // now it should work
        task.execute(ctx);

        Map result = (Map) ctx.getVariable("result");
        assertNotNull(result);
        assertTrue((boolean)result.get("ok"));
        List data = (List) result.get("data");
        assertEquals(10, data.size());
    }

    @Test
    public void testNoCertValidation() throws Exception {
        MockContext ctx = new MockContext(buildDbQueryConfig());

        UtilsTest.injectVariable(task, "action", "pql");
        ctx.setVariable("queryString", "inventory[certname]{ limit 10 }");
        ctx.setVariable(Constants.Keys.DATABASE_URL_KEY, httpsRule.baseUrl());

        // Self-signed cert will fail unless we provide a cert to trust
        try {
            task.execute(ctx);
            fail("Task should fail when self-signed cert is used without a provided certificate to trust.");
        } catch (SSLHandshakeException expected) {
            // that's fine
            log.info("hit expected ssl exception. Not a problem");
        } catch (Exception e) {
            log.error(e.getMessage());
            throw e;
        }


        ctx.setVariable(Constants.Keys.VALIDATE_CERTS_KEY, false);

        // now it should work
        task.execute(ctx);

        Map result = (Map) ctx.getVariable("result");
        assertNotNull(result);
        assertTrue((boolean)result.get("ok"));
        List data = (List) result.get("data");
        assertEquals(10, data.size());
    }
    @Test
    public void testTokenCreate() throws Exception {
        MockContext ctx = new MockContext(buildRbacCfg());

        UtilsTest.injectVariable(task, "action", "createApiToken");
        task.execute(ctx);

        Map result = (Map) ctx.getVariable("result");
        assertTrue((Boolean) result.get("ok"));
        String token = (String) result.get("data");
        assertNotNull(token);
    }

    @Test
    public void testTokenCreateAllParams() throws Exception {
        MockContext ctx = new MockContext(buildRbacCfg());

        UtilsTest.injectVariable(task, "action", "createApiToken");
        ctx.setVariable("tokenDescription", "token description");
        ctx.setVariable("tokenLabel", "token label");
        ctx.setVariable("tokenLife", "5h");

        task.execute(ctx);

        Map result = (Map) ctx.getVariable("result");
        assertTrue((Boolean) result.get("ok"));
        String token = (String) result.get("data");
        assertNotNull(token);
    }

    @Test
    public void testTokenPayload() throws Exception {
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


    @Test
    public void test404() {
        stubFor404();

        MockContext ctx = new MockContext(buildDbQueryConfig());

        UtilsTest.injectVariable(task, "action", "pql");
        ctx.setVariable("queryString", "inventory[certname]{ limit 10 }");

        try {
            task.execute(ctx);
            throw new Exception("No exception was thrown in task.");
        } catch (ApiException expected) {
            assertEquals(404, expected.getCode());
        } catch (Exception e) {
            fail("404 error should cause API exception");
        }
    }

    /**
     * 503 errors are retried. The resulting exception will be due to retry max
     * being reached, not the 503 error itself.
     */
    @Test
    public void test503() {
        stubFor503();

        MockContext ctx = new MockContext(buildDbQueryConfig());

        UtilsTest.injectVariable(task, "action", "pql");
        ctx.setVariable("queryString", "inventory[certname]{ limit 10 }");

        try {
            task.execute(ctx);
            throw new Exception("No exception was thrown in task.");
        } catch (ApiException expected) {
            assertTrue(expected.getMessage().contains("Retry max reached"));
        } catch (Exception e) {
            fail("404 error should cause API exception");
        }
    }

    /**
     * Tests {@link PuppetHostnameVerifier} by executing a request with loopback
     * address 'localhost' but configuring a hostname verifier to only trust
     * '127.0.0.1'
     */
    @Test
    public void testWrongHostName() throws Exception {
        stubForOk();

        OkHttpClient.Builder clientBuilder =  new OkHttpClient.Builder()
                .connectTimeout(10000L, TimeUnit.SECONDS)
                .readTimeout(10000L, TimeUnit.SECONDS)
                .writeTimeout(10000L, TimeUnit.SECONDS);

        // no certificate validation for wiremock
        final TrustManager[] tms = new TrustManager[] {
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                    }

                    @Override
                    public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                    }

                    @Override
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new java.security.cert.X509Certificate[0];
                    }
                }
        };
        final SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
        sslContext.init(null, tms, new java.security.SecureRandom());
        final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

        // Request to execute
        Request.Builder rBuilder = new Request.Builder()
                .url(httpsRule.baseUrl() + "/ok")
                .post(RequestBody.create(MediaType.parse("application/json"), "{}"));
        Request request = rBuilder.build();


        // Create a hostname mismatch
        clientBuilder
                .sslSocketFactory(sslSocketFactory, (X509TrustManager)tms[0])
                .hostnameVerifier(new PuppetHostnameVerifier("https://127.0.0.1"));
        OkHttpClient client = clientBuilder.build();

        try {
            client.newCall(request).execute();
            fail("Hostname mismatch should result in SSLPeerUnverifiedException");
        } catch (SSLPeerUnverifiedException expected) {
            // that's a good thing
        } catch (Exception e) {
            log.info("Hostname mismatch should result in SSLPeerUnverifiedException");
            fail("Hostname mismatch should result in SSLPeerUnverifiedException");
        }


        // Do it again, but with the right hostname
        clientBuilder
                .sslSocketFactory(sslSocketFactory, (X509TrustManager)tms[0])
                .hostnameVerifier(new PuppetHostnameVerifier("https://localhost"));

        client = clientBuilder.build();

        try {
            client.newCall(request).execute();
        } catch (Exception e) {
            fail("Hostnames match but test still failed: " + e.getMessage());
        }
    }
}
