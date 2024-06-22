package com.walmartlabs.concord.plugins.puppet;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2024 Walmart Inc., Concord Authors
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
import com.walmartlabs.concord.plugins.puppet.model.cfg.PuppetConfiguration;
import com.walmartlabs.concord.plugins.puppet.model.dbquery.DbQueryPayload;
import com.walmartlabs.concord.plugins.puppet.model.exception.ApiException;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.TrustManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PuppetClientTest extends AbstractApiTest {

    /**
     * Tests host name verification when custom cert store is use by executing
     * a request with loopback address '127.0.0.1' when the TLS certificate only
     * supports DNS alt name 'localhost'
     */
    @Test
    void testWrongHostName() throws Exception {
        stubForOk();

        var clientBuilder = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10));

        // no certificate validation for wiremock
        final TrustManager[] tms = IgnoringTrustManager.getManagers(true);
        final SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
        sslContext.init(null, tms, new java.security.SecureRandom());

        // Request to execute
        var badRequest = HttpRequest.newBuilder(URI.create("https://127.0.0.1:" + httpsRule.getHttpsPort() + "/ok"))
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .header("Content-Type", "application/json")
                .build();

        // Create a hostname mismatch
        clientBuilder.sslContext(sslContext);
        var expected = assertThrows(SSLHandshakeException.class, () -> clientBuilder.build().send(badRequest, HttpResponse.BodyHandlers.discarding()));
        assertTrue(expected.getMessage().contains("No subject alternative names matching IP address 127.0.0.1 found"));


        // Do it again, but with the right hostname
        var goodRequest = HttpRequest.newBuilder(URI.create("https://localhost:" + httpsRule.getHttpsPort() + "/ok"))
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .header("Content-Type", "application/json")
                .build();

        assertDoesNotThrow(() -> clientBuilder.build().send(goodRequest, HttpResponse.BodyHandlers.discarding()),
                "Hostnames match but test still failed");
    }

    @Test
    void test404() throws Exception {
        stubFor404();

        var cfg = PuppetConfiguration.parseFromMap(Map.of(
                Constants.Keys.DATABASE_URL_KEY, httpRule.baseUrl(),
                Constants.Keys.API_TOKEN_KEY, "23w4der5f6tg7yh8uj9iko",
                Constants.Keys.QUERY_STRING_KEY, "inventory[certname]{ limit 10 }"
        ), DbQueryCfg.class);
        var client = new PuppetClient(cfg);

        var expected = assertThrows(ApiException.class, () -> client.dbQuery(new DbQueryPayload(cfg)));
        assertEquals(404, expected.getCode());
    }

    /**
     * 503 errors are retried. The resulting exception will be due to retry max
     * being reached, not the 503 error itself.
     */
    @Test
    void test503() throws Exception {
        stubFor503();

        var cfg = PuppetConfiguration.parseFromMap(Map.of(
                Constants.Keys.DATABASE_URL_KEY, httpRule.baseUrl(),
                Constants.Keys.API_TOKEN_KEY, "23w4der5f6tg7yh8uj9iko",
                Constants.Keys.QUERY_STRING_KEY, "inventory[certname]{ limit 10 }",
                Constants.Keys.HTTP_RETRIES_KEY, "1"
        ), DbQueryCfg.class);
        var client = new PuppetClient(cfg);


        var expected = assertThrows(ApiException.class, () -> client.dbQuery(new DbQueryPayload(cfg)));
        assertTrue(expected.getMessage().contains("Retry max reached"));
    }

    @Test
    void testNoCertValidation() throws Exception {
        stubForDbQuery();

        // -- Task in-vars

        var failCfg = getCfg(input -> {
            input.put(Constants.Keys.DATABASE_URL_KEY, httpsRule.baseUrl());
            input.put(Constants.Keys.HTTP_RETRIES_KEY, "1");
        });

        // -- Execute - this should fail

        // Self-signed cert will fail unless we provide a cert to trust
        assertThrows(SSLHandshakeException.class, () -> getClient(failCfg).dbQuery(new DbQueryPayload(failCfg)));

        // -- Task in-vars - disable certificate verification

        var successCfg = getCfg(input -> {
            input.put(Constants.Keys.DATABASE_URL_KEY, httpsRule.baseUrl());
            input.put(Constants.Keys.HTTP_RETRIES_KEY, "1");
            input.put(Constants.Keys.VALIDATE_CERTS_KEY, false);
        });

        // -- Execute - now it should work

        var data = assertDoesNotThrow(() -> getClient(successCfg).dbQuery(new DbQueryPayload(failCfg)));

        assertNotNull(data);
        assertEquals(10, data.size());
    }

    private static DbQueryCfg getCfg(Consumer<Map<String, Object>> extraVars) {
        Map<String, Object> input = new HashMap<>();

        input.put(Constants.Keys.DATABASE_URL_KEY, httpRule.baseUrl());
        input.put(Constants.Keys.API_TOKEN_KEY, "23w4der5f6tg7yh8uj9iko");
        input.put(Constants.Keys.QUERY_STRING_KEY, "inventory[certname]{ limit 10 }");

        extraVars.accept(input);

        return PuppetConfiguration.parseFromMap(input, DbQueryCfg.class);
    }

    private static PuppetClient getClient(PuppetConfiguration cfg) {
        try {
            return new PuppetClient(cfg);
        } catch (Exception e) {
            throw new IllegalStateException("Error instantiating PuppetClient: " + e.getMessage());
        }
    }

}
