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
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.StringJoiner;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

public abstract class AbstractApiTest {
    static final Logger log = LoggerFactory.getLogger(AbstractApiTest.class);

    private static final String CONTENT_TYPE = "Content-Type";
    private static final String ACCEPT = "Accept";
    private static final String APPLICATION_JSON = "application/json";

    private Path certPath;
    private String certString;

    private static final String WM_KEYSTORE_PASSWORD = UUID.randomUUID().toString();

    @RegisterExtension
    static WireMockExtension httpRule = WireMockExtension.newInstance()
            .options(wireMockConfig()
                    .dynamicPort()
                    .notifier(new ConsoleNotifier(false))) // set to true for verbose logging
            .build();

    @RegisterExtension
    static WireMockExtension httpsRule = WireMockExtension.newInstance()
            .options(wireMockConfig()
                    .dynamicPort()
                    .dynamicHttpsPort()
                    .keystorePath(generateWireMockHttpsKeyStore().toString()) // Either a path to a file or a resource on the classpath
                    .keystorePassword(WM_KEYSTORE_PASSWORD) // The password used to access the keystore. Defaults to "password" if omitted
                    .keyManagerPassword(WM_KEYSTORE_PASSWORD) // The password used to access individual keys in the keystore. Defaults to "password" if omitted
                    .notifier(new ConsoleNotifier(false))) // set to true for verbose logging
                    .build();

    AbstractApiTest() {
    }

    /**
     * Values used for most tests
     * @return map of defaults for puppetParams
     */
    HashMap<String, Object> getDefaults() {
        HashMap<String, Object> d = new HashMap<>();
        d.put(Constants.Keys.RBAC_URL_KEY, httpRule.baseUrl());
        d.put(Constants.Keys.USERNAME_KEY, "fake-username");
        d.put(Constants.Keys.PASSWORD_KEY, "fake-password");
        d.put(Constants.Keys.DATABASE_URL_KEY, httpRule.baseUrl());
        d.put(Constants.Keys.API_TOKEN_KEY, "23w4der5f6tg7yh8uj9iko");

        return d;
    }

    private static Path generateWireMockHttpsKeyStore() {
        var privKey = loadWireMockPrivateKey(Paths.get("wiremock_cert/server.key"));
        var cert = loadWireMockCert(Paths.get("wiremock_cert/server.crt"));

        try {
            Path outFile = Files.createTempFile("wiremock_keystore", ".jks");
            return saveKeyStore(cert, privKey, outFile);
        } catch (IOException e) {
            throw new IllegalStateException("Error creating keystore: " + e.getMessage(), e);
        }
    }

    private static X509Certificate loadWireMockCert(Path certPath)  {
        try (var is = Files.newInputStream(certPath)) {
            CertificateFactory fact = CertificateFactory.getInstance("X.509");

            return (X509Certificate) fact.generateCertificate(is);
        } catch (Exception e) {
            throw new IllegalStateException("Error loading cert from disk: " + e.getMessage(), e);
        }
    }

    private static PrivateKey loadWireMockPrivateKey(Path keyPath) {
        try {
            // Read file to a byte array, remove newline characters
            var b64Decode = Base64.getDecoder();
            byte[] privKeyByteArray = b64Decode.decode(Files.readString(keyPath)
                        .replace("-----BEGIN PRIVATE KEY-----", "")
                        .replace("-----END PRIVATE KEY-----", "")
                        .replace("\n", ""));

            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privKeyByteArray);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");

            return keyFactory.generatePrivate(keySpec);
        } catch (Exception e) {
            throw new RuntimeException("Error loading load private key:" + e.getMessage());
        }
    }

    private static Path saveKeyStore(X509Certificate cert, PrivateKey key, Path outFile) {
        try (var os = Files.newOutputStream(outFile)) {
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);
            keyStore.setKeyEntry("wiremock_test_cert", key, WM_KEYSTORE_PASSWORD.toCharArray(), new java.security.cert.Certificate[]{cert});
            keyStore.store(os, WM_KEYSTORE_PASSWORD.toCharArray());

            return outFile;
        } catch (Exception e) {
            throw new IllegalStateException("Error saving keystore: " + e.getMessage(), e);
        }
    }

    /**
     * Gets the public certificate of the Wiremock https rule and saves it to a
     * file for use with self-signed certificate tests
     * @return path to the certificate file
     * @throws Exception when certificate cannot be obtained
     */
    Path getWiremockCertFile() throws Exception {
        if (certPath != null) { return certPath; }

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

        SSLSocket socket = (SSLSocket) factory.createSocket(hostname, httpsRule.getHttpsPort());
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
            fos.write(encodedKey.toString().getBytes(StandardCharsets.UTF_8));
        }

        return certPath;
    }

    /**
     * Gets Base64-encoded certificate for the HTTPS Wiremock server as a string
     * @return Base64-encoded certificate
     * @throws Exception when certificate cannot be read
     */
    String getWiremockCertString() throws Exception {
        if (certString != null) { return certString; }

        certString = Files.readString(getWiremockCertFile());
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

        httpRule.stubFor(WireMock.post(WireMock
                .urlPathMatching("^/rbac-api/v1/auth/token$"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withHeader(ACCEPT, APPLICATION_JSON)
                        .withBody(json)
                )
        );
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
