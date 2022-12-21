package com.walmartlabs.concord.plugins.akeyless;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2022 Walmart Inc., Concord Authors
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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

public final class Util {

    private Util() {
    }

    public static String hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            return toHex(encodedhash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder(2 * bytes.length);

        for (byte aByte : bytes) {
            String hex = Integer.toHexString(0xff & aByte);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }

        return hexString.toString();
    }

    public static void debug(boolean enabled, Logger log, String msg) {
        if (!enabled) {
            return;
        }

        log.info("DEBUG: {}", msg);
    }

    /**
     * @param o Input value with may be a String or Map of Concord secret
     *          info ('org', 'name', 'password')
     * @param secretExporter for access Concord's Secrets API
     * @return String value from direct input or exported Secret value
     */
    @SuppressWarnings("unchecked")
    public static String stringOrSecret(Object o, SecretExporter secretExporter) {
        if (o == null) {
            return null;
        }

        if (o instanceof String) {
            return (String) o;
        }

        if (!(o instanceof Map)) {
            throw new IllegalArgumentException("Invalid data type given for sensitive argument. Must be string or map.");
        }

        ((Map<?, ?>) o).forEach((key, value) -> {
            if (!(key instanceof String)) {
                throw new IllegalArgumentException("Non-string key used for secret definition");
            }

            if (!(value instanceof String)) {
                throw new IllegalArgumentException("Non-string value used for key '" + key + "' in secret definition");
            }
        });

        Map<String, String> secretInfo = (Map<String, String>) o;

        return secretExporter.exportAsString(secretInfo.get("org"), secretInfo.get("name"), secretInfo.get("password")).getValue();
    }
}
