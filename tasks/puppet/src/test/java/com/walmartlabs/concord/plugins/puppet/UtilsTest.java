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

import com.walmartlabs.concord.sdk.InjectVariable;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UtilsTest {

    @Test
    void normalizeUrlTest() {
        String normalizedUrl = "https://my-api.com";
        String normalizedUrlWithPort = "https://my-api.com:8080";

        assertEquals(normalizedUrl, Utils.normalizeUrl("https://my-api.com/"));
        assertEquals(normalizedUrlWithPort, Utils.normalizeUrl("https://my-api.com:8080/"));
        assertEquals(normalizedUrl, Utils.normalizeUrl("https://my-api.com"));
        assertEquals(normalizedUrlWithPort, Utils.normalizeUrl("https://my-api.com:8080"));
    }

    @Test
    void v2MergeParamsDoesNotMutateDefaults() {
        Map<String, Object> defaults = new HashMap<>();
        defaults.put(Constants.Keys.DATABASE_URL_KEY, "https://default.example.com");
        defaults.put(Constants.Keys.HTTP_RETRIES_KEY, 1);

        Map<String, Object> input = Map.of(
                Constants.Keys.DATABASE_URL_KEY, "https://input.example.com",
                Constants.Keys.USERNAME_KEY, "puppet-user"
        );

        Map<String, Object> merged = UtilsV2.mergeParams(input, defaults);

        assertEquals("https://input.example.com", merged.get(Constants.Keys.DATABASE_URL_KEY));
        assertEquals("puppet-user", merged.get(Constants.Keys.USERNAME_KEY));
        assertEquals("https://default.example.com", defaults.get(Constants.Keys.DATABASE_URL_KEY));
        assertEquals(1, defaults.get(Constants.Keys.HTTP_RETRIES_KEY));
    }

    static void injectVariable(Object target, String key, Object value) {
        Field[] fields = target.getClass().getDeclaredFields();
        for (Field field : fields) {
            InjectVariable a = field.getAnnotation(InjectVariable.class);
            if (a != null && a.value().equals(key)) {
                Utils.setField(target, field, value);
            }
        }
    }
}
