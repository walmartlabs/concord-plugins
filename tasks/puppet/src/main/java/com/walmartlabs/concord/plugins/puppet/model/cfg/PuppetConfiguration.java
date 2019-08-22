package com.walmartlabs.concord.plugins.puppet.model.cfg;

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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.walmartlabs.concord.plugins.puppet.Utils;
import com.walmartlabs.concord.plugins.puppet.model.exception.ConfigException;
import com.walmartlabs.concord.plugins.puppet.model.exception.InvalidValueException;
import com.walmartlabs.concord.plugins.puppet.model.exception.MissingParameterException;
import com.walmartlabs.concord.sdk.Context;
import com.walmartlabs.concord.sdk.MapUtils;
import com.walmartlabs.concord.sdk.SecretService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Field;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

import static com.walmartlabs.concord.plugins.puppet.Constants.Keys.*;

public abstract class PuppetConfiguration {
    private static final Logger log = LoggerFactory.getLogger(PuppetConfiguration.class);

    private List<Certificate> certificates;

    @JsonProperty(value = DEBUG_KEY, defaultValue = "false", required = true)
    private Boolean debug = false;
    @JsonProperty(value = CONNECT_TIMEOUT_KEY, defaultValue = "30")
    private Number connectTimeout;
    @JsonProperty(value = READ_TIMEOUT_KEY, defaultValue = "30")
    private Number readTimeout;
    @JsonProperty(value = WRITE_TIMEOUT_KEY, defaultValue = "30")
    private Number writeTimeout;
    @JsonProperty(value = VALIDATE_CERTS_KEY, defaultValue = "true")
    private Boolean validateCerts = true;
    @JsonProperty(value = CERTIFICATE_KEY)
    private Map<String, Object> certificate;

    PuppetConfiguration() {
    }

    /**
     * @return API call base URL
     */
    public abstract String getBaseUrl();

    /**
     * Returns heads that are required for an API call
     * @return Map of headers
     */
    public abstract Map<String, String> getHeaders();

    public long getConnectTimeout() {
        return connectTimeout.intValue();
    }

    public long getWriteTimeout() {
        return writeTimeout.intValue();
    }

    public long getReadTimeout() {
        return readTimeout.intValue();
    }

    public boolean doDebug() {
        return debug;
    }

    public boolean validateCerts() {
        return validateCerts;
    }

    public List<Certificate> getCertificates() {
        return certificates;
    }

    public boolean useCustomKeyStore() {
        return certificates != null && !certificates.isEmpty();
    }

    /**
     * Converts PEM-encoded certificate string into certificates and adds it to a
     * List of certificates
     * @param text PEM-encoded certificate
     * @param pCfg PuppetConfiguration to add the certs
     */
    private static void textToCert(String text, PuppetConfiguration pCfg)  {
        try (
                InputStream is = new ByteArrayInputStream(text.getBytes());
                BufferedInputStream bis = new BufferedInputStream(is)
        ) {
            streamToCerts(bis, pCfg);
        } catch (Exception e) {
            throw new ConfigException("Error reading certificates: " + e.getMessage());
        }
    }

    /**
     * Converts PEM-encoded certificate file into certificates and adds it to a
     * List of certificates
     * @param filePath Path to the PEM-encoded certificate file
     * @param pCfg PuppetConfiguration to add the certs
     */
    private static void fileToCert(String filePath, PuppetConfiguration pCfg) {
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
     * @param is InputSteam of pem-encoded certificate data
     * @param pCfg PuppetConfiguration to add the certs
     * @throws IOException when error is encountered with the InputStream
     * @throws CertificateException when certificate cannot be parsed
     */
    private static void streamToCerts(InputStream is, PuppetConfiguration pCfg) throws IOException, CertificateException {
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");

        while (is.available() > 0) {
            pCfg.addCert(certificateFactory.generateCertificate(is));
        }
    }

    /**
     * Adds a certificate to the list of certificates to trust
     * @param c cert to trust
     */
    private void addCert(Certificate c) {
        if (certificates == null) {
            certificates = new ArrayList<>();
        }
        certificates.add(c);
    }


    /**
     * Reads one or more certificates from given sources
     * @param secretService Concord {@link SecretService} for retrieving file secrets
     * @param ctx Concord {@link Context} for getting process info and use with SecretService
     */
    public void initializeCertificates(SecretService secretService, Context ctx) {
        if (certificate == null) {
            return;
        }

        if (certificate.containsKey(CERTIFICATE_TEXT_KEY)) {
            textToCert(MapUtils.getString(certificate, CERTIFICATE_TEXT_KEY), this);
        }
        if (certificate.containsKey(CERTIFICATE_PATH_KEY)) {
            fileToCert(MapUtils.getString(certificate, CERTIFICATE_PATH_KEY), this);
        }
        if (certificate.containsKey(CERTIFICATE_SECRET_KEY)) {
            Map<String, Object> secret = (Map<String, Object>) certificate.get(CERTIFICATE_SECRET_KEY);
            String sOrg = (String) secret.get(CERTIFICATE_ORG_KEY);
            String sName = (String) secret.get(CERTIFICATE_NAME_KEY);
            String sPass = (String) secret.getOrDefault(CERTIFICATE_PASSWORD_KEY, null);

            try {
                String filename = secretService.exportAsFile(
                        ctx, (String) ctx.getVariable(TX_ID), (String) ctx.getVariable(WORK_DIR), sOrg, sName, sPass
                );
                fileToCert(filename, this);
            } catch (Exception e) {
                throw new ConfigException("Error reading certificate from secret: " + e.getMessage());
            }
        }
    }

    /**
     * Ensures all attributes annotated with {@link JsonProperty} are set with acceptable values
     */
    void validateAttributes() {
        Field[] fields = Arrays.stream(this.getClass().getDeclaredFields())
                .filter(e -> e.getAnnotation(JsonProperty.class) != null).toArray(Field[]::new);

        // Check each ExternalParam field
        for (Field f : fields) {
            JsonProperty a = f.getAnnotation(JsonProperty.class);
            Object obj = Utils.getFieldValue(this, f); // Current value in this object
            String val = obj == null ? a.defaultValue() : obj.toString(); // Parameter value we'll work with

            // Make sure required parameters are not null or empty
            checkForEmptyValue(a, val);

            // Set default values if they haven't been set yet.
            if (obj == null) {
                setDefaultValue(a, this, f, val);
            }
        }
    }

    /**
     * Throws a {@link MissingParameterException} when a required parameter is missing or empty/null
     * @param a ExternalParam attribute settings
     * @param val Value to check
     */
    private static void checkForEmptyValue(JsonProperty a, Object val) {
        if (!a.required()) {
            // don't worry about it
            return;
        }

        if (val == null) {
            throw new MissingParameterException(a.value());
        }

        // Empty strings are a problem
        if (val instanceof String && ((String) val).trim().isEmpty()) {
            throw new MissingParameterException(a.value());
        }
    }

    /**
     * Sets the defaults for a field, specified by an {@link JsonProperty} attribute
     * @param a Attribute configuration
     * @param f Field of the target object to set
     * @param target Object to inject value into
     * @param val Value to inject
     */
    private static void setDefaultValue(JsonProperty a, PuppetConfiguration target, Field f, Object val) {
        // Use default value if not set
        if (val == null) {
            val = a.defaultValue();
            Utils.debug(log, target.doDebug(),
                    String.format("Using default value for parameter '%s': %s", a.value(), a.defaultValue()));
        }

        // Handle required parameters
        checkForEmptyValue(a, val);

        if (f.getType() == Number.class) {
            Utils.setField(target, f, coerceToNumber(a.value(), val));
        } else if (f.getType() == List.class) {
            Utils.setField(target, f, coerceToList(a.value(), val));
        } else if (f.getType() == Map.class) {
            Utils.setField(target, f, coerceToMap(a.value(), val));
        } else if (f.getType() == Boolean.class) {
            Utils.setField(target, f, Boolean.parseBoolean(val.toString()));
        } else if (f.getType() == String.class) {
            // don't set empty strings (why isn't jackson's JsonInclude.Include.NON_EMPTY
            // taking care of this??
            if (!((String) val).trim().isEmpty()) {
                Utils.setField(target, f, val);
            }
        } else {
            Utils.setField(target, f, val);
        }
    }

    /**
     * Attempts to convert an object to a Number.
     * is true
     * @param name Name of the parameter that will be coerced
     * @param val Value to coerce
     * @return Value as a number
     */
    private static Number coerceToNumber(String name, Object val) {
        try {
            return NumberFormat.getInstance().parse(val.toString());
        } catch (ParseException e) {
            throw new InvalidValueException(name, val, "Unable to convert to Number");
        }
    }

    /**
     * Attempts ot convert an Object to a List.
     * @param name Name of the object (for logging)
     * @param val Object that should be a list
     * @return List value
     */
    private static List coerceToList(String name, Object val) {
        if (val instanceof String && ((String) val).trim().isEmpty()) {
            // it was a default value
            return new ArrayList<>(0);
        }
        try {
            //noinspection ConstantConditions
            return (List) val;
        } catch (ClassCastException ex) {
            throw new InvalidValueException(name, val, "Unable to convert to List");
        }
    }

    /**
     * Attempts to convert and Object to Map
     * @param name name of hte object (for logging)
     * @param val Object that should be a Map
     * @return Map value
     */
    private static Map coerceToMap(String name, Object val) {
        if (val instanceof String && ((String) val).trim().isEmpty()) {
            // it was a default value
            return new HashMap<>(0);
        }

        try {
            //noinspection ConstantConditions
            return (Map) val;
        } catch (ClassCastException ex) {
            throw new InvalidValueException(name, val, "Unable to convert to Map");
        }
    }

    /**
     * Initializes an object from a map of attributes
     * @param m map of attributes for the object
     * @return instantiated object containing the attributes from the Map
     */
    public static <T extends PuppetConfiguration> T parseFromMap(Map<String, Object> m, Class<T> clazz) {
        T config;

        // get fields from the class and its parent
        List<Field> fields = getJsonPropertyFields(clazz);
        fields.addAll(getJsonPropertyFields(clazz.getSuperclass()));

        try {
            config = clazz.newInstance();
            for (Field f : fields) {
                JsonProperty a = f.getAnnotation(JsonProperty.class);
                Object val = m.get(a.value());
                setDefaultValue(a, config, f, val);
            }

            config.validateAttributes();
        } catch (IllegalAccessException | InstantiationException e) {
            // ooh that's really bad
            throw new ConfigException(e.getMessage());
        }

        return config;
    }

    /**
     * Gets the fields with the {@link JsonProperty} annotation from a class.
     * @param clazz Class from which to get the fields
     * @return Array of fields
     */
    private static List<Field> getJsonPropertyFields(Class clazz) {

        return Arrays.stream(clazz.getDeclaredFields())
                .filter(e -> e.getAnnotation(JsonProperty.class) != null).collect(Collectors.toList());
    }
}
