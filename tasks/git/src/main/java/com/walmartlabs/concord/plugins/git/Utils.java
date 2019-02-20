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

import com.walmartlabs.concord.sdk.Context;
import com.walmartlabs.concord.sdk.ContextUtils;

import java.util.Map;

public final class Utils {

    public static boolean getBoolean(Context ctx, String k, boolean fallback) {
        Object v = ctx.getVariable(k);

        if (v == null) {
            return fallback;
        }

        if (v instanceof String) {
            return Boolean.parseBoolean((String) v);
        }

        if (!(v instanceof Boolean)) {
            throw new IllegalArgumentException("Expected a boolean value '" + k + "', got " + v);
        }

        return (Boolean) v;
    }

    public static String getUrl(Map<String, Object> defaults, Context ctx, String k) {
        String v = ContextUtils.getString(ctx, k, (defaults != null ? defaults.get(k).toString() : null));
        if (v == null) {
            throw new IllegalArgumentException("Mandatory parameter '" + k + "' is required");
        }
        return v;
    }

    private Utils() {
    }
}
