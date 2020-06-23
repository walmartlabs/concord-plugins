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

import com.walmartlabs.concord.plugins.puppet.model.cfg.PuppetConfiguration;
import com.walmartlabs.concord.plugins.puppet.model.exception.ConfigException;
import com.walmartlabs.concord.sdk.Context;
import com.walmartlabs.concord.sdk.MapUtils;
import com.walmartlabs.concord.sdk.SecretService;
import org.slf4j.Logger;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Paths;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.HashMap;
import java.util.Map;

import static com.walmartlabs.concord.plugins.puppet.Constants.Keys.TX_ID;
import static com.walmartlabs.concord.plugins.puppet.Constants.Keys.WORK_DIR;

public final class Utils {

    private Utils() {
        // private constructor to prevent instantiation
    }

    static <T extends PuppetConfiguration> T
    createCfg(Context ctx, SecretService secretService, Map<String, Object> defaults, Class<T> clazz) {

        // start with a map of default values
        Map<String, Object> m = new HashMap<>(defaults != null ? defaults : new HashMap<>());

        // add values from context, overriding defaults if value is non-null
        for (String k : Constants.Keys.getAllInParams()) {
            putIntoMap(m, k, ctx);
        }

        // Instantiate a PuppetConfig obj
        T cfg = PuppetConfiguration.parseFromMap(m, clazz);

        String txId = MapUtils.assertString(m, TX_ID);
        String workDir = MapUtils.assertString(m, WORK_DIR);

        cfg.initializeCertificates((o, n, p) -> Paths.get(
                secretService.exportAsFile(ctx, txId, workDir, o, n, p)
        ));

        return cfg;
    }

    /**
     * Converts PEM-encoded certificate file into certificates and adds it to a
     * List of certificates
     *
     * @param filePath Path to the PEM-encoded certificate file
     * @param pCfg     PuppetConfiguration to add the certs
     */
    public static void fileToCert(String filePath, PuppetConfiguration pCfg) {
        try (
                FileInputStream is = new FileInputStream(filePath);
                BufferedInputStream bis = new BufferedInputStream(is)
        ) {
            streamToCerts(bis, pCfg);
        } catch (Exception e) {
            throw new ConfigException("Error reading certificates: " + e.getMessage());
        }
    }

    /**
     * Converts PEM-encoded InputStream into certificates and adds it to a
     * List of certificates
     *
     * @param is   InputSteam of pem-encoded certificate data
     * @param pCfg PuppetConfiguration to add the certs
     * @throws IOException          when error is encountered with the InputStream
     * @throws CertificateException when certificate cannot be parsed
     */
    public static void streamToCerts(InputStream is, PuppetConfiguration pCfg) throws IOException, CertificateException {
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");

        while (is.available() > 0) {
            pCfg.addCert(certificateFactory.generateCertificate(is));
        }
    }

    static String normalizeUrl(String url) {
        if (url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }

    public static Map<String, Object> getDefaultParams(Map<String, Object> globalVars) {
        return MapUtils.getMap(globalVars, Constants.Keys.PARAMS_KEY, new HashMap<>());
    }

    /**
     * Logs info only if debug is enabled
     *
     * @param log   {@link Logger} to use
     * @param debug True if debug is enabled
     * @param msg   Message to log
     */
    public static void debug(Logger log, boolean debug, String msg) {
        if (debug) {
            log.info(msg);
        }
    }

    /**
     * Puts a key-value pair into a given Map if the value is not null
     *
     * @param m   Map to store the key-value pair
     * @param k   Key to store
     * @param ctx Context from which to get the value
     */
    static void putIntoMap(Map<String, Object> m, String k, Context ctx) {
        Object v = ctx.getVariable(k);
        if (v == null) {
            return;
        }

        m.put(k, v);
    }

    /**
     * Set the value of a Field in and object
     *
     * @param target Target object in which to set the field value
     * @param field  Field to inject
     * @param value  Value to inject
     */
    public static void setField(Object target, Field field, Object value) {
        try {
            if (!field.isAccessible()) {
                field.setAccessible(true);
            }

            field.set(target, value);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException("Variable injection failed on field " + field.getName());
        }

    }

    /**
     * Get the value of a Field in an object
     *
     * @param target Target object from which to get the field's value
     * @param field  Field to retrieve the value from
     * @return the field's value
     */
    public static Object getFieldValue(Object target, Field field) {
        Object value;
        try {
            boolean wasAccessible = field.isAccessible();
            if (!wasAccessible) {
                field.setAccessible(true);
            }

            value = field.get(target);

            field.setAccessible(wasAccessible);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException("Cannot access field: " + field.getName());
        }

        return value;
    }

    static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
