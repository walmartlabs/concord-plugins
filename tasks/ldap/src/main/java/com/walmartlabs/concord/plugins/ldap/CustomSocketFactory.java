package com.walmartlabs.concord.plugins.ldap;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2021 Walmart Inc., Concord Authors
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.SocketFactory;
import javax.net.ssl.*;
import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class CustomSocketFactory extends SocketFactory {
    private static final Logger log = LoggerFactory.getLogger(CustomSocketFactory.class);
    // reuse single instance via getDefault() method
    private static CustomSocketFactory instance;
    static final String CERT_PATH = ".ldapcerts";

    private final SocketFactory sslSocketFactory;


    public CustomSocketFactory() {
        sslSocketFactory = createSSLSocketFactory();
    }

    public static SocketFactory getDefault() {
        synchronized (CustomSocketFactory.class) {
            if (instance == null) {
                instance = new CustomSocketFactory();
            }
        }

        return instance;
    }

    private static SocketFactory createSSLSocketFactory() {
        try {
            KeyStore keyStore = generateKeystore(getCerts());
            X509TrustManager tm = getTrustManager(keyStore);
            SSLContext sslContext = getSslContext(keyStore, tm);

            return sslContext.getSocketFactory();
        } catch (Exception e) {
            log.error("Error setting up custom socket factory", e);
        }

        throw new RuntimeException("Something went wrong generating socket factory");
    }

    /**
     * Creates an SSL Context for validating SSL connections.
     *
     * @param keyStore KeyStore to use for certificate validations
     * @param tm       for making trust decisions
     * @return SSL Context with the given keystore and trustmanager
     * @throws Exception
     */
    private static SSLContext getSslContext(KeyStore keyStore, X509TrustManager tm) throws Exception {
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, "keystore_pass".toCharArray());

        SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
        TrustManager[] managers = new TrustManager[1];
        managers[0] = tm;
        sslContext.init(null, managers, null);

        return sslContext;
    }

    /**
     * Gets an X509TrustManager for use with ssl connections
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
     * Creates a KeyStore for use with SSL connections containing a given list of certificates
     *
     * @param certificates list of Certificates to add to the keystore
     * @return Keystore used for ssl verification
     */
    private static KeyStore generateKeystore(List<Certificate> certificates) {
        KeyStore keyStore;
        try {

            keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);

            int i = 0;
            for (Certificate c : certificates) {
                keyStore.setCertificateEntry("custom-cert-" + i++, c);
            }
            log.debug("Added {} custom cert(s) to keystore", i);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Error setting up keystore. Cannot find certificate file.");
        } catch (Exception ex) {
            log.info("Error setting up keystore: ", ex);
            throw new RuntimeException("Error Error setting up keystore.");
        }

        return keyStore;
    }

    private static List<Certificate> getCerts() throws CertificateException {
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        List<Certificate> certs = new LinkedList<>();

        Path certFile = Paths.get(CERT_PATH);
        try (InputStream is = Files.newInputStream(certFile);
             BufferedInputStream bis = new BufferedInputStream(is)) {

            while (bis.available() > 0) {
                certs.add(certificateFactory.generateCertificate(bis));
            }
        } catch (Exception e) {
            log.error("Error reading certificates", e);
        }

        log.debug("Using {} custom CA certs", certs.size());

        return certs;
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        return sslSocketFactory.createSocket(host, port);
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost,
                               int localPort) throws IOException {
        return sslSocketFactory.createSocket(host, port, localHost, localPort);
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        return sslSocketFactory.createSocket(host, port);
    }

    @Override
    public Socket createSocket(InetAddress address, int port,
                               InetAddress localAddress, int localPort) throws IOException {
        return sslSocketFactory.createSocket(address, port, localAddress, localPort);
    }
}
