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

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Rule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.Certificate;
import java.util.Base64;
import java.util.StringJoiner;

public abstract class AbstractApiTest {
    static final Logger log = LoggerFactory.getLogger(AbstractApiTest.class);

    private static final String CONTENT_TYPE = "Content-Type";
    private static final String ACCEPT = "Accept";
    private static final String APPLICATION_JSON = "application/json";

    private Path certPath;
    private String certString;

    @Rule
    public WireMockRule httpRule = new WireMockRule(
            WireMockConfiguration.wireMockConfig()
                    .dynamicPort()
                    .notifier(new ConsoleNotifier(false)) // set to true for verbose logging
        );
    @Rule
    public WireMockRule httpsRule = new WireMockRule(
            WireMockConfiguration.wireMockConfig()
                .dynamicPort() // still binds to 8080 for an http port if not specified
                .dynamicHttpsPort()
                .notifier(new ConsoleNotifier(false)) // set to true for verbose logging
    );

    AbstractApiTest() {
    }


    /**
     * Gets the public certificate of the Wiremock https rule and saves it to a
     * file for use with self-signed certificate tests
     * @return path to the certificate file
     * @throws Exception when certificate cannot be obtained
     */
    Path getWiremockCertFile() throws Exception {
        if (certPath != null) { return certPath; }

        try {
            // Create a trust manager that does not validate certificate chains
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }
                        public void checkClientTrusted(
                                java.security.cert.X509Certificate[] certs, String authType) {
                        }
                        public void checkServerTrusted(
                                java.security.cert.X509Certificate[] certs, String authType) {
                        }
                    }
            };

            // Install the all-trusting trust manager
            SSLContext sc = SSLContext.getInstance("TLSv1.2");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());


            String hostname = "localhost";
            SSLSocketFactory factory = HttpsURLConnection.getDefaultSSLSocketFactory();

            SSLSocket socket = (SSLSocket) factory.createSocket(hostname, httpsRule.httpsPort());
            socket.startHandshake();
            Certificate[] certs = socket.getSession().getPeerCertificates();
            Certificate cert = certs[0];
            socket.close();


            String keyEncodedString = Base64.getEncoder().encodeToString(cert.getEncoded());

            StringBuilder encodedKey = new StringBuilder();
            encodedKey.append("-----BEGIN CERTIFICATE-----\n");

            int col = 0;
            for (int i=0; i < keyEncodedString.length(); i++) {
                encodedKey.append(keyEncodedString.charAt(i));
                col++;
                if (col == 64) {
                    encodedKey.append("\n");
                    col = 0;
                }
            }
            encodedKey.append("\n-----END CERTIFICATE-----");

            certPath = Paths.get("target/wiremock.pem");
            try (FileOutputStream fos = new FileOutputStream(certPath.toString())) {
                fos.write(encodedKey.toString().getBytes());
            }

            return certPath;

        } catch (Exception ex) {
            log.error("Failed to get wiremock public key");
        }

        throw new Exception("Failed to get wiremock public cert for testing.");
    }

    /**
     * Gets Base64-encoded certificate for the HTTPS Wiremock server as a string
     * @return Base64-encoded certificate
     * @throws Exception when certificate cannot be read
     */
    String getWiremockCertString() throws Exception {
        if (certString != null) { return certString; }

        certString = new String(Files.readAllBytes(getWiremockCertFile()));
        return certString;
    }

    /**
     * Mocks a RBAC API call to create an API token
     * @throws URISyntaxException  when resource path is invalid
     * @throws IOException when resource cannot be read
     */
    void stubForTokenCreate() throws URISyntaxException, IOException {
        URL resourcePath = this.getClass().getClassLoader().getResource("tokenCreate.json");
        if (resourcePath == null) {
            throw new ResourceNotFoundException("tokenCreate.json");
        }
        StringJoiner sj = new StringJoiner("\n");
        Files.readAllLines(Paths.get(resourcePath.toURI())).forEach(sj::add);
        String json = sj.toString();


        httpsRule.stubFor(WireMock.post(WireMock
                .urlPathMatching("^/rbac-api/v1/auth/token$"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withHeader(ACCEPT, APPLICATION_JSON)
                        .withBody(json)
                )
        );
    }

    /**
     * Mocks a PuppetDB API call. The API call returns a JSON list of objects
     * @throws URISyntaxException  when resource path is invalid
     * @throws IOException when resource cannot be read
     */
    void stubForDbQuery() throws URISyntaxException, IOException {
        URL resourcePath = this.getClass().getClassLoader().getResource("simpleQueryResult.json");
        if (resourcePath == null) {
            throw new ResourceNotFoundException("simpleQueryResult.json");
        }

        StringJoiner sj = new StringJoiner("\n");
        Files.readAllLines(Paths.get(resourcePath.toURI())).forEach(sj::add);
        String json = sj.toString();

        httpRule.stubFor(WireMock.post(WireMock
                .urlPathMatching("^/pdb/query/v4$"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withHeader(ACCEPT, APPLICATION_JSON)
                        .withBody(json)
                )
        );
        httpsRule.stubFor(WireMock.post(WireMock
                .urlPathMatching("^/pdb/query/v4$"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withHeader(ACCEPT, APPLICATION_JSON)
                        .withBody(json)
                )
        );
    }

    /**
     * Mocks a simple POST request. Useful for testing connection settings, not
     * a Puppet API.
     */
    void stubForOk() {
        httpRule.stubFor(WireMock.post(WireMock
                .urlPathMatching("^/ok$"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withHeader(ACCEPT, APPLICATION_JSON)
                        .withBody("[ \"ok\" ]")
                )
        );
        httpsRule.stubFor(WireMock.post(WireMock
                .urlPathMatching("^/ok$"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withHeader(ACCEPT, APPLICATION_JSON)
                        .withBody("[ \"ok\" ]")
                )
        );
    }

    void stubFor404() {
        httpRule.stubFor(WireMock.post(WireMock
                .urlPathMatching("^/pdb/query/v4$"))
                .withRequestBody(WireMock.matchingJsonPath("$.query"))
                .willReturn(WireMock.aResponse()
                        .withStatus(404)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withHeader(ACCEPT, APPLICATION_JSON)
                        .withBody("[ \"not found\" ]")
                )
        );
    }

    void stubFor503() {
        httpRule.stubFor(WireMock.post(WireMock
                .urlPathMatching("^/pdb/query/v4$"))
                .withRequestBody(WireMock.matchingJsonPath("$.query"))
                .willReturn(WireMock.aResponse()
                        .withStatus(503)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withHeader(ACCEPT, APPLICATION_JSON)
                        .withBody("[ \"service unavailable\" ]")
                )
        );
    }

    static class ResourceNotFoundException extends RuntimeException {
        ResourceNotFoundException(String resourceName) {
            super("Resource could not be found: " + resourceName);
        }
    }
}
