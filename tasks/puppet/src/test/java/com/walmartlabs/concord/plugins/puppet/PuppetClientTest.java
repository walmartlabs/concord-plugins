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

import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
        final TrustManager[] tms = new TrustManager[] {
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new java.security.cert.X509Certificate[0];
                    }
                }
        };
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

}
