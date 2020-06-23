package com.walmartlabs.concord.plugins.puppet;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2020 Walmart Inc.
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

import com.walmartlabs.concord.plugins.puppet.model.PuppetResult;
import com.walmartlabs.concord.plugins.puppet.model.exception.MissingParameterException;
import com.walmartlabs.concord.runtime.v2.sdk.SecretService;
import com.walmartlabs.concord.runtime.v2.sdk.Variables;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.net.ssl.SSLHandshakeException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;

public class PuppetTaskV2Test extends AbstractApiTest {
    private PuppetTaskV2 task;

    private Map<String, Object> variables;
    private Map<String, Object> globals;
    private final SecretService secretService = Mockito.mock(SecretService.class);
    private final Context taskContext = Mockito.mock(Context.class);
    private final Variables input = Mockito.mock(Variables.class);
    private final Variables defaults = Mockito.mock(Variables.class);

    @Before
    public void setup() throws Exception {
        stubForDbQuery();
        stubForTokenCreate();

        Mockito.doAnswer(invocationOnMock -> getWiremockCertFile())
                .when(secretService)
                .exportAsFile(anyString(), anyString(), nullable(String.class));

        variables = new HashMap<>();
        variables.put("puppetParams", getDefaults());
        Mockito.doAnswer(invocationOnMock -> variables).when(input).toMap();
        // Set global variables that are used by the task
        // Defaults use the http API URLs. HTTPs needs to be set in taskVars
        globals = new HashMap<>();
        globals.put("puppetParams", getDefaults());
        Mockito.doAnswer(invocationOnMock -> defaults).when(taskContext).variables();
        Mockito.doAnswer(invocationOnMock -> globals.get((String) invocationOnMock.getArgument(0))).when(defaults).getMap(anyString(), any());

        task = new PuppetTaskV2(secretService, taskContext);
    }

    @Test
    public void testQuery() throws Exception {

        // -- Task in-vars

        variables.put("action", "pql");
        variables.put("queryString",  "inventory[certname]{ limit 10 }");

        // -- Execute

        PuppetResult result = (PuppetResult) task.execute(input);

        // -- Validate

        assertNotNull(result);
        assertTrue(result.isOk());
        List data = (List) result.getData();
        assertEquals(10, data.size());
    }

    @Test
    public void testBadUrl() {

        // -- Task in-vars

        variables.put("action", "pql");
        variables.put("queryString",  "inventory[certname]{ limit 10 }");
        // Empty URL
        variables.put("databaseUrl", "");

        // -- Execute - empty url value

        try {
            task.execute(input);
            fail("Bad url should cause an exception");
        } catch(MissingParameterException expected) {
            assert(expected.getMessage().contains("Cannot find value for databaseUrl"));
        } catch (Exception e) {
            fail("Unexpected exception with bad URL: " + e.getMessage());
        }

        // -- Execute - mis-formatted url value

        // Invalid URL
        variables.put("databaseUrl", "notaurl");
        try {
            task.execute(input);
            fail("Bad url should cause an exception");
        } catch(IllegalArgumentException expected) {
            assert(expected.getMessage().contains("Invalid URL"));
        } catch (Exception e) {
            fail("Unexpected exception with bad URL: " + e.getMessage());
        }
    }

    @Test
    public void testIgnoreErrors() throws Exception {

        // -- Task in-vars

        variables.put("action", "bad_action");
        variables.put("ignoreErrors", true);

        // -- Execute

        // Normally, this should throw an exception
        PuppetResult result = (PuppetResult) task.execute(input);

        // -- Validate

        // Make sure it failed gracefully and has an error message
        assertNotNull(result);
        assertFalse(result.isOk());
        String error = result.getError();
        assertTrue(error.contains("Not a supported action"));
    }

    @Test
    public void testMissingAction() {

        // -- Task in-vars

        variables.put("action", null);

        // -- Execute

        try {
            task.execute(input);
            fail("Missing action value should cause exception");
        } catch (MissingParameterException expected) {
            // ok, that's expected
            assertTrue(expected.getMessage().contains("Cannot find value for action"));
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }


        // -- Task in-vars - remove action altogether

        variables.remove("action");

        // -- Execute

        try {
            task.execute(input);
            fail("Missing action value should cause exception");
        } catch (MissingParameterException expected) {
            // ok, that's expected
            assertTrue(expected.getMessage().contains("Cannot find value for action"));
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }



    }

    @Test
    public void testSelfSignedCertWithPath() throws Exception {

        // -- Task in-vars

        variables.put("action", "pql");
        variables.put("queryString",  "inventory[certname]{ limit 10 }");
        variables.put("databaseUrl", httpsRule.baseUrl());

        // -- Execute - this should fail

        // Self-signed cert will fail unless we provide a cert to trust
        try {
            task.execute(input);
            throw new Exception("Task should fail when self-signed cert is used without a provided certificate to trust.");
        } catch (SSLHandshakeException expected) {
            // that's fine
            log.info("hit expected ssl exception. Not a problem");
        } catch (Exception e) {
            log.error(e.getMessage());
            throw e;
        }

        // -- Task in-vars - add certificate info (file path)

        Map<String, Object> certificate = new HashMap<>();
        certificate.put("path", getWiremockCertFile().toString());
        variables.put("certificate", certificate);

        // -- Execute - now it should work

        PuppetResult result = (PuppetResult) task.execute(input);

        assertNotNull(result);
        assertTrue(result.isOk());
        List data = (List) result.getData();
        assertEquals(10, data.size());
    }

    @Test
    public void testSelfSignedCertWithText() throws Exception {

        // -- Task in-vars

        variables.put("action", "pql");
        variables.put("queryString",  "inventory[certname]{ limit 10 }");
        variables.put("databaseUrl", httpsRule.baseUrl());

        // -- Execute - this should fail

        // Self-signed cert will fail unless we provide a cert to trust
        try {
            task.execute(input);
            fail("Task should fail when self-signed cert is used without a provided certificate to trust.");
        } catch (SSLHandshakeException expected) {
            // that's fine
            log.info("hit expected ssl exception. Not a problem");
        } catch (Exception e) {
            log.error(e.getMessage());
            throw e;
        }

        // -- Task in-vars - add certificate info (text)

        Map<String, Object> certificate = new HashMap<>();
        certificate.put("text", getWiremockCertString());
        variables.put("certificate", certificate);

        // -- Execute - now it should work

        PuppetResult result = (PuppetResult) task.execute(input);

        assertNotNull(result);
        assertTrue(result.isOk());
        List data = (List) result.getData();
        assertEquals(10, data.size());
    }

    @Test
    public void testSelfSignedCertWithSecret() throws Exception {

        // -- Task in-vars

        variables.put("action", "pql");
        variables.put("queryString",  "inventory[certname]{ limit 10 }");
        variables.put("databaseUrl", httpsRule.baseUrl());

        // -- Execute - this should fail

        // Self-signed cert will fail unless we provide a cert to trust
        try {
            task.execute(input);
            fail("Task should fail when self-signed cert is used without a provided certificate to trust.");
        } catch (SSLHandshakeException expected) {
            // that's fine
            log.info("hit expected ssl exception. Not a problem");
        } catch (Exception e) {
            log.error(e.getMessage());
            throw e;
        }

        // -- Task in-vars - add certificate info (secret)

        Map<String, Object> certificate = new HashMap<>();
        Map<String, Object> secret = new HashMap<>();
        secret.put("org", "org");
        secret.put("name", "name");
        secret.put("password", null);

        certificate.put("secret", secret);
        variables.put("certificate", certificate);

        // -- Execute - now it should work

        PuppetResult result = (PuppetResult) task.execute(input);

        assertNotNull(result);
        assertTrue(result.isOk());
        List data = (List) result.getData();
        assertEquals(10, data.size());
    }

    @Test
    public void testNoCertValidation() throws Exception {

        // -- Task in-vars

        variables.put("action", "pql");
        variables.put("queryString",  "inventory[certname]{ limit 10 }");
        variables.put("databaseUrl", httpsRule.baseUrl());

        // -- Execute - this should fail

        // Self-signed cert will fail unless we provide a cert to trust
        try {
            task.execute(input);
            fail("Task should fail when self-signed cert is used without a provided certificate to trust.");
        } catch (SSLHandshakeException expected) {
            // that's fine
            log.info("hit expected ssl exception. Not a problem");
        } catch (Exception e) {
            log.error(e.getMessage());
            throw e;
        }

        // -- Task in-vars - disable certificate verification

        variables.put("validateCerts", false);

        // -- Execute - now it should work

        PuppetResult result = (PuppetResult) task.execute(input);

        assertNotNull(result);
        assertTrue(result.isOk());
        List data = (List) result.getData();
        assertEquals(10, data.size());
    }

    @Test
    public void testTokenCreate() throws Exception {

        // -- Task in-vars

        variables.put("action", "createApiToken");

        // -- Execute - now it should work

        PuppetResult result = (PuppetResult) task.execute(input);

        assertTrue(result.isOk());
        String token = (String) result.getData();
        assertNotNull(token);
    }

}
