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

import com.walmartlabs.concord.plugins.puppet.model.exception.MissingParameterException;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.SecretService;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import com.walmartlabs.concord.runtime.v2.sdk.Variables;
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
import static org.mockito.Mockito.doAnswer;

class PuppetTaskV2Test extends AbstractApiTest {
    private PuppetTaskV2 task;

    private Map<String, Object> variables;
    private Map<String, Object> globals;
    private final SecretService secretService = Mockito.mock(SecretService.class);
    private final Context taskContext = Mockito.mock(Context.class);
    private final Variables input = Mockito.mock(Variables.class);
    private final Variables defaults = Mockito.mock(Variables.class);

    @BeforeEach
    public void setup() throws Exception {
        stubForDbQuery();
        stubForTokenCreate();

        doAnswer(invocationOnMock -> getWiremockCertFile())
                .when(secretService)
                .exportAsFile(anyString(), anyString(), nullable(String.class));

        variables = new HashMap<>();
        variables.put("puppetParams", getDefaults());
        doAnswer(invocationOnMock -> variables).when(input).toMap();
        // Set global variables that are used by the task
        // Defaults use the http API URLs. HTTPs needs to be set in taskVars
        globals = new HashMap<>();
        globals.put("puppetParams", getDefaults());
        doAnswer(invocationOnMock -> defaults).when(taskContext).variables();
        doAnswer(invocationOnMock -> globals.get((String) invocationOnMock.getArgument(0)))
                .when(defaults).getMap(anyString(), any());

        task = new PuppetTaskV2(secretService, taskContext);
    }

    @Test
    void testQuery() throws Exception {

        // -- Task in-vars

        variables.put("action", "pql");
        variables.put("queryString", "inventory[certname]{ limit 10 }");

        // -- Execute

        TaskResult.SimpleResult result = task.execute(input);

        // -- Validate

        assertNotNull(result);
        assertTrue(result.ok());
        List<?> data = (List<?>) result.values().get("data");
        assertEquals(10, data.size());
    }

    @Test
    void testBadUrl() {

        // -- Task in-vars

        variables.put("action", "pql");
        variables.put("queryString", "inventory[certname]{ limit 10 }");
        // Empty URL
        variables.put("databaseUrl", "");

        // -- Execute - empty url value

        var expectedMissingParam = assertThrows(MissingParameterException.class, () -> task.execute(input));
        assertTrue(expectedMissingParam.getMessage().contains("Cannot find value for databaseUrl"));


        // -- Execute - mis-formatted url value

        // Invalid URL
        variables.put("databaseUrl", "notaurl");

        var expectedIllegalArg = assertThrows(IllegalArgumentException.class, () -> task.execute(input));
        assertTrue(expectedIllegalArg.getMessage().contains("URI with undefined scheme"));
    }

    @Test
    void testIgnoreErrors() throws Exception {

        // -- Task in-vars

        variables.put("action", "bad_action");
        variables.put("ignoreErrors", true);

        // -- Execute

        // Normally, this should throw an exception
        TaskResult.SimpleResult result = task.execute(input);

        // -- Validate

        // Make sure it failed gracefully and has an error message
        assertNotNull(result);
        assertFalse(result.ok());
        String error = result.error();
        assertNotNull(error);
        assertTrue(error.contains("Not a supported action"));
    }

    @Test
    void testMissingAction() {

        // -- Task in-vars

        variables.put("action", null);

        // -- Execute

        var expected = assertThrows(MissingParameterException.class, () -> task.execute(input));
        assertTrue(expected.getMessage().contains("Cannot find value for action"));


        // -- Task in-vars - remove action altogether

        variables.remove("action");

        // -- Execute

        expected = assertThrows(MissingParameterException.class, () -> task.execute(input));
        assertTrue(expected.getMessage().contains("Cannot find value for action"));
    }

    @Test
    void testSelfSignedCertWithPath() throws Exception {

        // -- Task in-vars

        variables.put("action", "pql");
        variables.put("queryString", "inventory[certname]{ limit 10 }");
        variables.put("databaseUrl", httpsRule.baseUrl());

        // -- Execute - this should fail

        // Self-signed cert will fail unless we provide a cert to trust
        assertThrows(SSLHandshakeException.class, () -> task.execute(input));

        // -- Task in-vars - add certificate info (file path)

        Map<String, Object> certificate = new HashMap<>();
        certificate.put("path", getWiremockCertFile().toString());
        variables.put("certificate", certificate);

        // -- Execute - now it should work

        TaskResult.SimpleResult result = task.execute(input);

        assertNotNull(result);
        assertTrue(result.ok());
        List<?> data = (List<?>) result.values().get("data");
        assertEquals(10, data.size());
    }

    @Test
    void testSelfSignedCertWithText() throws Exception {

        // -- Task in-vars

        variables.put("action", "pql");
        variables.put("queryString", "inventory[certname]{ limit 10 }");
        variables.put("databaseUrl", httpsRule.baseUrl());

        // -- Execute - this should fail

        // Self-signed cert will fail unless we provide a cert to trust
        var ex = assertThrows(SSLHandshakeException.class, () -> task.execute(input));

        assertInstanceOf(SSLHandshakeException.class, ex.getCause());
        // that's fine


        // -- Task in-vars - add certificate info (text)

        Map<String, Object> certificate = new HashMap<>();
        certificate.put("text", getWiremockCertString());
        variables.put("certificate", certificate);

        // -- Execute - now it should work

        TaskResult.SimpleResult result = task.execute(input);

        assertNotNull(result);
        assertTrue(result.ok());
        List<?> data = (List<?>) result.values().get("data");
        assertEquals(10, data.size());
    }

    @Test
    void testSelfSignedCertWithSecret() throws Exception {

        // -- Task in-vars

        variables.put("action", "pql");
        variables.put("queryString", "inventory[certname]{ limit 10 }");
        variables.put("databaseUrl", httpsRule.baseUrl());

        // -- Execute - this should fail

        // Self-signed cert will fail unless we provide a cert to trust
        assertThrows(SSLHandshakeException.class, () -> task.execute(input));

        // -- Task in-vars - add certificate info (secret)

        Map<String, Object> certificate = new HashMap<>();
        Map<String, Object> secret = new HashMap<>();
        secret.put("org", "org");
        secret.put("name", "name");
        secret.put("password", null);

        certificate.put("secret", secret);
        variables.put("certificate", certificate);

        // -- Execute - now it should work

        TaskResult.SimpleResult result = task.execute(input);

        assertNotNull(result);
        assertTrue(result.ok());
        List<?> data = (List<?>) result.values().get("data");
        assertEquals(10, data.size());
    }

    @Test
    void testNoCertValidation() throws Exception {

        // -- Task in-vars

        variables.put("action", "pql");
        variables.put("queryString", "inventory[certname]{ limit 10 }");
        variables.put("databaseUrl", httpsRule.baseUrl());

        // -- Execute - this should fail

        // Self-signed cert will fail unless we provide a cert to trust
        assertThrows(SSLHandshakeException.class, () -> task.execute(input));

        // -- Task in-vars - disable certificate verification

        variables.put("validateCerts", false);

        // -- Execute - now it should work

        TaskResult.SimpleResult result = task.execute(input);

        assertNotNull(result);
        assertTrue(result.ok());
        List<?> data = (List<?>) result.values().get("data");
        assertEquals(10, data.size());
    }

    @Test
    void testTokenCreate() throws Exception {

        // -- Task in-vars

        variables.put("action", "createApiToken");

        // -- Execute - now it should work

        TaskResult.SimpleResult result = task.execute(input);

        assertTrue(result.ok());
        String token = (String) result.values().get("data");
        assertNotNull(token);
    }

}
