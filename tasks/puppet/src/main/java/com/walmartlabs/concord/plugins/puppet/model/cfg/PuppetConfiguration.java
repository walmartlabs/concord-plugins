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
import com.walmartlabs.concord.plugins.puppet.model.SecretExporter;
import com.walmartlabs.concord.plugins.puppet.model.exception.ConfigException;
import com.walmartlabs.concord.plugins.puppet.model.exception.InvalidValueException;
import com.walmartlabs.concord.plugins.puppet.model.exception.MissingParameterException;
import com.walmartlabs.concord.sdk.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.cert.Certificate;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static com.walmartlabs.concord.plugins.puppet.Constants.Keys.CERTIFICATE_KEY;
import static com.walmartlabs.concord.plugins.puppet.Constants.Keys.CERTIFICATE_NAME_KEY;
import static com.walmartlabs.concord.plugins.puppet.Constants.Keys.CERTIFICATE_ORG_KEY;
import static com.walmartlabs.concord.plugins.puppet.Constants.Keys.CERTIFICATE_PASSWORD_KEY;
import static com.walmartlabs.concord.plugins.puppet.Constants.Keys.CERTIFICATE_PATH_KEY;
import static com.walmartlabs.concord.plugins.puppet.Constants.Keys.CERTIFICATE_SECRET_KEY;
import static com.walmartlabs.concord.plugins.puppet.Constants.Keys.CERTIFICATE_TEXT_KEY;
import static com.walmartlabs.concord.plugins.puppet.Constants.Keys.CONNECT_TIMEOUT_KEY;
import static com.walmartlabs.concord.plugins.puppet.Constants.Keys.DEBUG_KEY;
import static com.walmartlabs.concord.plugins.puppet.Constants.Keys.HTTP_RETRIES_KEY;
import static com.walmartlabs.concord.plugins.puppet.Constants.Keys.HTTP_VERSION_KEY;
import static com.walmartlabs.concord.plugins.puppet.Constants.Keys.READ_TIMEOUT_KEY;
import static com.walmartlabs.concord.plugins.puppet.Constants.Keys.VALIDATE_CERTS_KEY;
import static com.walmartlabs.concord.plugins.puppet.Constants.Keys.VALIDATE_CERTS_NOT_AFTER_KEY;
import static com.walmartlabs.concord.plugins.puppet.Constants.Keys.WRITE_TIMEOUT_KEY;

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
    @JsonProperty(value = VALIDATE_CERTS_NOT_AFTER_KEY, defaultValue = "true")
    Boolean validateCertsNotAfter = true;
    @JsonProperty(value = HTTP_VERSION_KEY, defaultValue = "default")
    private String httpVersion;
    @JsonProperty(value = HTTP_RETRIES_KEY, defaultValue = "3")
    private Number httpRetries;
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
     *
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

    public HttpVersion getHttpVersion() {
        return HttpVersion.from(httpVersion);
    }

    public int getHttpRetries() {
        return httpRetries.intValue();
    }

    public boolean doDebug() {
        return debug;
    }

    public boolean validateCerts() {
        return validateCerts;
    }

    public boolean validateCertsNotAfter() {
        return validateCertsNotAfter;
    }

    /**
     * @return List of certificates used for API call verification
     */
    public List<Certificate> getCertificates() {
        return certificates;
    }

    public boolean useCustomKeyStore() {
        return certificates != null && !certificates.isEmpty();
    }

    /**
     * Converts PEM-encoded certificate string into certificates and adds it to a
     * List of certificates
     *
     * @param text PEM-encoded certificate
     * @param pCfg PuppetConfiguration to add the certs
     */
    private static void textToCert(String text, PuppetConfiguration pCfg) {
        try (
                InputStream is = new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
                BufferedInputStream bis = new BufferedInputStream(is)
        ) {
            Utils.streamToCerts(bis, pCfg);
        } catch (Exception e) {
            throw new ConfigException("Error reading certificates: " + e.getMessage());
        }
    }

    /**
     * Adds a certificate to the list of certificates to trust
     *
     * @param c cert to trust
     */
    public void addCert(Certificate c) {
        if (certificates == null) {
            certificates = new ArrayList<>();
        }
        certificates.add(c);
    }

    /**
     * Reads one or more certificates from given sources
     *
     * @param exporter Class that can interact with some version of Concord's SecretService
     */
    public void initializeCertificates(SecretExporter exporter) {
        if (certificate == null) {
            return;
        }

        if (certificate.containsKey(CERTIFICATE_TEXT_KEY)) {
            textToCert(MapUtils.getString(certificate, CERTIFICATE_TEXT_KEY), this);
        }
        if (certificate.containsKey(CERTIFICATE_PATH_KEY)) {
            Utils.fileToCert(MapUtils.getString(certificate, CERTIFICATE_PATH_KEY), this);
        }
        if (certificate.containsKey(CERTIFICATE_SECRET_KEY)) {
            Map<String, Object> secret = MapUtils
                    .getMap(certificate, CERTIFICATE_SECRET_KEY, Collections.emptyMap());
            String sOrg = (String) secret.get(CERTIFICATE_ORG_KEY);
            String sName = (String) secret.get(CERTIFICATE_NAME_KEY);
            String sPass = (String) secret.getOrDefault(CERTIFICATE_PASSWORD_KEY, null);

            Utils.debug(log, debug, String.format("Loading cert from secret: %s/%s", sOrg, sName));

            try {
                Path filePath = exporter.export(sOrg, sName, sPass);
                Utils.fileToCert(filePath.toString(), this);
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
     *
     * @param a   ExternalParam attribute settings
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
     *
     * @param a      Attribute configuration
     * @param f      Field of the target object to set
     * @param target Object to inject value into
     * @param val    Value to inject
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
            // don't set empty strings (why isn't jackson's JsonInclude.Include.NON_EMPTY)
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
     *
     * @param name Name of the parameter that will be coerced
     * @param val  Value to coerce
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
     *
     * @param name Name of the object (for logging)
     * @param val  Object that should be a list
     * @return List value
     */
    private static List<?> coerceToList(String name, Object val) {
        if (val instanceof List) {
            return (List<?>) val;
        }

        if (val instanceof String && ((String) val).isBlank()) {
            // it was a default value
            return new ArrayList<>(0);
        }

        throw new InvalidValueException(name, val, "Unable to convert to List");
    }

    /**
     * Attempts to convert and Object to Map
     *
     * @param name name of hte object (for logging)
     * @param val  Object that should be a Map
     * @return Map value
     */
    private static Map<?, ?> coerceToMap(String name, Object val) {
        if (val instanceof Map) {
            return (Map<?, ?>) val;
        }

        if (val instanceof String && ((String) val).isBlank()) {
            // it was a default value
            return new HashMap<>(0);
        }

        throw new InvalidValueException(name, val, "Unable to convert to Map");
    }

    /**
     * Initializes an object from a map of attributes
     *
     * @param m map of attributes for the object
     * @return instantiated object containing the attributes from the Map
     */
    public static <T extends PuppetConfiguration> T parseFromMap(Map<String, Object> m, Class<T> clazz) {
        T config;

        // get fields from the class and its parent
        List<Field> fields = new LinkedList<>(getJsonPropertyFields(clazz));
        fields.addAll(getJsonPropertyFields(clazz.getSuperclass()));

        try {
            config = clazz.getDeclaredConstructor().newInstance();
            for (Field f : fields) {
                JsonProperty a = f.getAnnotation(JsonProperty.class);
                Object val = m.get(a.value());
                setDefaultValue(a, config, f, val);
            }

            config.validateAttributes();
        } catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
            // ooh that's really bad
            throw new ConfigException(e.getMessage());
        }

        return config;
    }

    /**
     * Gets the fields with the {@link JsonProperty} annotation from a class.
     *
     * @param clazz Class from which to get the fields
     * @return List of fields
     */
    private static List<Field> getJsonPropertyFields(Class<?> clazz) {

        return Arrays.stream(clazz.getDeclaredFields())
                .filter(e -> e.getAnnotation(JsonProperty.class) != null)
                .toList();
    }

    public enum HttpVersion {
        HTTP_1_1("HTTP/1.1"),
        HTTP_2("HTTP/2.0"),
        DEFAULT("DEFAULT");

        private final String value;

        HttpVersion(String value) {
            this.value = value;
        }

        public static HttpVersion from(String val) {
            if (val == null || val.isBlank()) {
                return DEFAULT;
            }

            var sanitizedVal = val.toUpperCase();

            for (HttpVersion version : HttpVersion.values()) {
                if (version.value.equals(sanitizedVal)) {
                    return version;
                }
            }

            throw new IllegalArgumentException("Unsupported  HTTP version: " + val);
        }
    }
}
