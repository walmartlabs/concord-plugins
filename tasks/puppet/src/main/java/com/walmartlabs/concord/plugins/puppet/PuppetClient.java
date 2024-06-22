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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.walmartlabs.concord.plugins.puppet.model.cfg.PuppetConfiguration;
import com.walmartlabs.concord.plugins.puppet.model.dbquery.DbQueryPayload;
import com.walmartlabs.concord.plugins.puppet.model.exception.ApiException;
import com.walmartlabs.concord.plugins.puppet.model.exception.ConfigException;
import com.walmartlabs.concord.plugins.puppet.model.token.TokenPayload;
import com.walmartlabs.concord.plugins.puppet.model.token.TokenResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;

class PuppetClient {

    private static final Logger log = LoggerFactory.getLogger(PuppetClient.class);
    private static final String KEYSTORE_PASS = UUID.randomUUID().toString();

    private final HttpClient client;
    private final PuppetConfiguration cfg;
    private final ObjectMapper objectMapper;


    PuppetClient(PuppetConfiguration configuration) throws Exception {
        this.cfg = configuration;
        this.client = createClient(configuration);
        this.objectMapper = new ObjectMapper()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    private <T> T post(URI uri, String payload, JavaType valueType) throws ApiException, SSLHandshakeException {
        var rBuilder = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .uri(uri)
                .timeout(Duration.ofSeconds(cfg.getReadTimeout()))
                .header("Content-Type", "application/json");

        cfg.getHeaders().forEach(rBuilder::header);

        var request = rBuilder.build();

        return invokeRequest(request, valueType);
    }

    @SuppressWarnings("unchecked")
    private <T> T invokeRequest(HttpRequest request, final JavaType valueType) throws ApiException, SSLHandshakeException {
        try {
            var response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            var statusCode = response.statusCode();

            if (statusCode < 200 || statusCode >= 300) {
                throw ApiException.buildException(statusCode, new String(new BufferedInputStream(response.body()).readAllBytes()));
            }

            if (valueType.equals(objectMapper.constructType(String.class))) {
                // if it returns just a string and not JSON
                return (T) new String(new BufferedInputStream(response.body()).readAllBytes());
            } else {
                return objectMapper.readValue(response.body(), valueType);
            }
        } catch (SSLHandshakeException e) {
            throw e;
        } catch (IOException e) {
            throw new ApiException("IO Exception calling API: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted", e);
        }
    }

    /**
     * Queries Puppet database for a list of results
     *
     * @param query Query string to execute
     * @return List of results
     * @throws Exception when error occurs executing query
     */
    List<Map<String, Object>> dbQuery(DbQueryPayload query) throws Exception {
        JavaType t = objectMapper.getTypeFactory().constructCollectionType(ArrayList.class, Map.class);
        var url = validateUrl(cfg.getBaseUrl(), "/pdb/query/v4");

        return withRetry(cfg.getHttpRetries(), 1000,
                () -> post(url, objectMapper.writeValueAsString(query), t)
        );
    }

    /**
     * Creates and API token for use with Puppet API calls
     *
     * @param tp Token payload containing parameters for creation
     * @return the new API token
     * @throws Exception when error encountered creating API token
     */
    String createToken(TokenPayload tp) throws Exception {
        JavaType t = objectMapper.constructType(TokenResult.class);
        URI url = validateUrl(cfg.getBaseUrl(), "/rbac-api/v1/auth/token");
        String body = objectMapper.writeValueAsString(tp);

        TokenResult r = withRetry(cfg.getHttpRetries(), 1000, () -> post(url, body, t));

        return r.getToken();
    }

    /**
     * Checks for a seemingly-usable API URL to execute against. Combines it with
     * a path to generate an encoded, full URL for an API call.
     *
     * @param path API Path for execution, include leading forward-slash '/'
     * @return Full, encoded URL string
     */
    private static URI validateUrl(String baseUrl, String path) {
        if (baseUrl == null || baseUrl.isEmpty()) {
            throw new IllegalArgumentException("No base url is set for HTTP requests.");
        }
        String url = String.format("%s%s", baseUrl, path);

        return URI.create(url);
    }

    /**
     * Executes a Callable until successful, up to a given number of retries
     *
     * @param retryCount    Number of allowed retry attempts
     * @param retryInterval Milliseconds to wait between retries
     * @param c             Callable to execute
     * @param <T>           Type of Callable
     * @return results of the Callable
     * @throws ApiException when api call can't be made successfully
     */
    private static <T> T withRetry(int retryCount, long retryInterval, Callable<T> c) throws Exception {
        Exception exception = null;
        int tryCount = 0;
        while (!Thread.currentThread().isInterrupted() && tryCount < retryCount) {
            try {
                return c.call(); // execute it
            } catch (ApiException e) {
                exception = e;

                log.error("call error: '{}'", e.getMessage());

                // these errors aren't worth retrying (auth, not found, etc)
                if (e.getCode() >= 400 && e.getCode() < 500) {
                    throw e;
                }
            } catch (SSLHandshakeException e) {
                // probably due to self-signed cert that isn't trusted
                log.error("Error during SSL handshake. Likely due to untrusted self-signed certificate.");
                throw e;
            } catch (IllegalArgumentException e) {
                // probably invalid url
                throw e;
            } catch (Exception e) {
                exception = e;
                log.error("call error", e);
            }

            // take a break
            log.info("retry after {} sec", retryInterval / 1000);
            Utils.sleep(1000);

            tryCount++;
        }

        // too many attempts, time to give up
        if (tryCount == retryCount) {
            var msg = Optional.ofNullable(exception).map(Exception::getMessage)
                    .orElse("no exception message");
            throw new ApiException(String.format("Retry max reached: %s", msg));
        }

        // Very unexpected exception
        throw new ApiException(exception);
    }

    /**
     * Creates an http client for use with API calls. Custom certificates can be
     * added, or certificate verification can be disabled, in the provided
     * {@link PuppetConfiguration}.
     *
     * @param cfg config with certificate settings
     * @return http client with certificate settings from the config
     */
    private HttpClient createClient(PuppetConfiguration cfg) throws Exception {
        var clientBuilder = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(cfg.getConnectTimeout()));

        if (cfg.getHttpVersion() == PuppetConfiguration.HttpVersion.HTTP_1_1) {
            clientBuilder.version(HttpClient.Version.HTTP_1_1);
        } else if (cfg.getHttpVersion() == PuppetConfiguration.HttpVersion.HTTP_2) {
            clientBuilder.version(HttpClient.Version.HTTP_2);
        }

        if (!cfg.validateCerts()) {
            Utils.debug(log, cfg.doDebug(), "Disabling certificate verification.");
            final TrustManager[] tms = IgnoringTrustManager.getManagers(cfg.validateCertsNotAfter());
            final SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
            sslContext.init(null, tms, new SecureRandom());

            clientBuilder.sslContext(sslContext);
        } else if (cfg.useCustomKeyStore()) {
            Utils.debug(log, cfg.doDebug(), "Adding master cert to trusted certs");
            KeyStore keyStore = generateKeystore(cfg.getCertificates(), cfg.doDebug());
            X509TrustManager tm = getTrustManager(keyStore);
            SSLContext sslContext = getSslContext(keyStore, tm);

            clientBuilder.sslContext(sslContext);
        } else {
            Utils.debug(log, cfg.doDebug(), "Using default keystore and validating certificates");
        }

        return clientBuilder.build();
    }

    /**
     * Gets an X509TrustManager for se with ssl connections
     *
     * @param keyStore Keystore to use with the trust manager
     * @return TrustManager to validate ssl connections
     * @throws NoSuchAlgorithmException KeyStoreException when default trust manager can't be obtained
     */
    private static X509TrustManager getTrustManager(KeyStore keyStore) throws NoSuchAlgorithmException, KeyStoreException {
        TrustManagerFactory tmf = TrustManagerFactory
                .getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(keyStore);
        TrustManager[] trustManagers = tmf.getTrustManagers();
        if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
            throw new IllegalStateException("Unexpected default trust managers:"
                    + Arrays.toString(trustManagers));
        }

        return (X509TrustManager) trustManagers[0];
    }

    /**
     * Creates an SSL Context for validating SSL connections.
     *
     * @param keyStore KeyStore to use for certificate validations
     * @param tm       for making trust decisions
     * @return SSL Context with the given keystore and trustmanager
     */
    private static SSLContext getSslContext(KeyStore keyStore, X509TrustManager tm) throws Exception {
        SSLContext sslContext;

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, KEYSTORE_PASS.toCharArray());

        sslContext = SSLContext.getInstance("TLSv1.2");
        sslContext.init(null, new TrustManager[]{tm}, null);

        return sslContext;
    }

    /**
     * Creates a KeyStore for use with SSL connections containing a given list of certificates
     *
     * @param certificates list of Certificates to add to the keystore
     * @return Keystore used for ssl verification
     */
    static KeyStore generateKeystore(List<Certificate> certificates, boolean debug) {
        KeyStore keyStore;
        try {
            keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);

            int i = 0;
            for (Certificate c : certificates) {
                keyStore.setCertificateEntry("custom-cert-" + i++, c);
            }
            Utils.debug(log, debug, String.format("Added %s custom cert(s) to keystore", i));
        } catch (FileNotFoundException e) {
            throw new ConfigException("Error setting up keystore. Cannot find certificate file.");
        } catch (Exception ex) {
            log.info("Error setting up keystore: ", ex);
            throw new ConfigException("Error Error setting up keystore.");
        }

        return keyStore;
    }
}
