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
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.FileNotFoundException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

class PuppetClient {

    private static final Logger log = LoggerFactory.getLogger(PuppetClient.class);

    private static final MediaType APPLICATION_JSON = MediaType.parse("application/json");

    private final OkHttpClient client;
    private final PuppetConfiguration cfg;

    private final ObjectMapper objectMapper;

    PuppetClient(PuppetConfiguration configuration) throws Exception {
        this.cfg = configuration;
        this.client = createClient(configuration);
        this.objectMapper = new ObjectMapper()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    private <T> T post(HttpUrl url, String payload, JavaType valueType) throws Exception {
        Request.Builder rBuilder = new Request.Builder()
                .url(url)
                .post(RequestBody.create(APPLICATION_JSON, payload));
        for (Map.Entry<String, String> e : cfg.getHeaders().entrySet()) {
            rBuilder.addHeader(e.getKey(), e.getValue());
        }

        Request request = rBuilder.build();

        return invokeRequest(request, valueType);
    }

    @SuppressWarnings("unchecked")
    private <T> T invokeRequest(Request request, final JavaType valueType) throws Exception {
        try (Response response = client.newCall(request).execute();
             ResponseBody body = response.body()) {

            if (!response.isSuccessful()) {
                throw ApiException.buildException(response.code(), response.message());
            }

            if (body == null) {
                throw ApiException.buildException(response.code(), "No body returned.");
            }

            if (valueType.equals(objectMapper.constructType(String.class))) {
                // if it returns just a string and not JSON
                return (T) body.string();
            } else {
                return objectMapper.readValue(body.byteStream(), valueType);
            }
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
        HttpUrl url = validateUrl(cfg.getBaseUrl(), "/pdb/query/v4");

        return withRetry(3, 1000,
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
        HttpUrl url = validateUrl(cfg.getBaseUrl(), "/rbac-api/v1/auth/token");
        TokenResult r = withRetry(3, 1000,
                () -> post(
                        url,
                        objectMapper.writeValueAsString(tp),
                        t
                )
        );

        return r.getToken();
    }

    /**
     * Checks for a seemingly-usable API URL to execute against. Combines it with
     * a path to generate an encoded, full URL for an API call.
     *
     * @param path API Path for execution, include leading forward-slash '/'
     * @return Full, encoded URL string
     */
    private static HttpUrl validateUrl(String baseUrl, String path) {
        if (baseUrl == null || baseUrl.isEmpty()) {
            throw new IllegalArgumentException("No base url is set for HTTP requests.");
        }
        String url = String.format("%s%s", baseUrl, path);

        HttpUrl urlObj = HttpUrl.parse(url);
        if (urlObj == null) {
            throw new IllegalArgumentException("Invalid URL: " + baseUrl);
        }

        return urlObj;
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
            } catch (Exception e) {
                exception = e;
                log.error("call error", e);
            }

            if (tryCount < retryCount) {
                // take a break
                log.info("retry after {} sec", retryInterval / 1000);
                Utils.sleep(1000);

                tryCount++;
            }
        }

        // too many attempts, time to give up
        if (tryCount == retryCount) {
            throw new ApiException(String.format("Retry max reached: %s", exception.getMessage()));
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
     * @throws Exception
     */
    private OkHttpClient createClient(PuppetConfiguration cfg) throws Exception {
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                .connectTimeout(cfg.getConnectTimeout(), TimeUnit.SECONDS)
                .readTimeout(cfg.getReadTimeout(), TimeUnit.SECONDS)
                .writeTimeout(cfg.getWriteTimeout(), TimeUnit.SECONDS);

        if (!cfg.validateCerts()) {
            Utils.debug(log, cfg.doDebug(), "Disabling certificate verification.");
            final TrustManager[] tms = new TrustManager[]{
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

            clientBuilder
                    .sslSocketFactory(sslSocketFactory, (X509TrustManager) tms[0])
                    .hostnameVerifier(new PuppetHostnameVerifier(cfg.getBaseUrl()));
        } else if (cfg.useCustomKeyStore()) {
            Utils.debug(log, cfg.doDebug(), "Adding master cert to trusted certs");
            KeyStore keyStore = generateKeystore(cfg.getCertificates());
            X509TrustManager tm = getTrustManager(keyStore);
            SSLContext sslContext = newGetSslContext(keyStore, tm);

            clientBuilder
                    .sslSocketFactory(sslContext.getSocketFactory(), tm)
                    .hostnameVerifier(new PuppetHostnameVerifier(cfg.getBaseUrl()));
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
     * @throws Exception when default trust manager can't be obtained
     */
    private static X509TrustManager getTrustManager(KeyStore keyStore) throws Exception {
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
     * @throws Exception
     */
    private static SSLContext newGetSslContext(KeyStore keyStore, X509TrustManager tm) throws Exception {
        SSLContext sslContext;


        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, "keystore_pass".toCharArray());

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
    private KeyStore generateKeystore(List<Certificate> certificates) {
        KeyStore keyStore;
        try {

            keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);

            int i = 0;
            for (Certificate c : certificates) {
                keyStore.setCertificateEntry("custom-cert-" + i++, c);
            }
            Utils.debug(log, cfg.doDebug(), String.format("Added %s custom cert(s) to keystore", i));
        } catch (FileNotFoundException e) {
            throw new ConfigException("Error setting up keystore. Cannot find certificate file.");
        } catch (Exception ex) {
            log.info("Error setting up keystore: ", ex);
            throw new ConfigException("Error Error setting up keystore.");
        }

        return keyStore;
    }
}
