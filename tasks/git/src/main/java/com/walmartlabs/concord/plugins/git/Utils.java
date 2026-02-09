package com.walmartlabs.concord.plugins.git;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2018 Wal-Mart Store, Inc.
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.walmartlabs.concord.common.secret.UsernamePassword;
import com.walmartlabs.concord.sdk.Secret;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import static com.walmartlabs.concord.sdk.MapUtils.getString;
import static java.util.Objects.requireNonNull;

public final class Utils {

    private static final String HOST_API = "api.github.com";
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    public static boolean getBoolean(Map<String, Object> in, String k, boolean fallback) {
        Object v = in.get(k);

        if (v == null) {
            return fallback;
        }

        if (v instanceof String s) {
            return Boolean.parseBoolean(s);
        }

        if (!(v instanceof Boolean)) {
            throw new IllegalArgumentException("Expected a boolean value '" + k + "', got " + v);
        }

        return (Boolean) v;
    }

    public static String getUrl(Map<String, Object> defaults, Map<String, Object> in, String k) {
        String v = getString(in, k, (String) (defaults != null ? defaults.get(k) : null));
        if (v == null) {
            throw new IllegalArgumentException("Mandatory parameter '" + k + "' is required");
        }
        return v;
    }

    public static String buildApiUrl(String baseUrl, String path) throws URISyntaxException {
        var uri = new URI(baseUrl);
        var host = uri.getHost();
        String prefix = null;
        if ("github.com".equals(host) || "gist.github.com".equals(host)) {
            host = HOST_API;
        }

        // Use URI prefix on non-standard host names
        if (!HOST_API.equals(host)) {
            prefix = "/api/v3";
        }

        var scheme = requireNonNull(uri.getScheme(), "Base URL without schema");
        var port = uri.getPort();

        var apiUri = new URI(scheme, null, host, port, joinPaths(prefix, path), null, null);
        return apiUri.toString();
    }

    private static String joinPaths(String a, String b) {
        var p2 = b.startsWith("/") ? b : "/" + b;
        if (a == null) {
            return p2;
        }
        var p1 = a.endsWith("/") ? a.substring(0, a.length() - 1) : a;
        return p1 + p2;
    }

    public static String hideSensitiveData(String s, Secret secret) {
        if (s == null) {
            return null;
        }

        if (secret instanceof UsernamePassword up) {
            char[] password = up.getPassword();
            if (password != null && password.length != 0) {
                s = s.replace(new String(password), "***");
            }
        } else if (secret instanceof TokenSecret ts) {
            String token = ts.getToken();
            if (token != null && !token.trim().isEmpty()) {
                s = s.replace(token, "***");
            }
        }
        return s;
    }

    public static ObjectMapper getObjectMapper() {
        return MAPPER;
    }

    private Utils() {
    }
}
