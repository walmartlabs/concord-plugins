/**
 * Concord
 * -----
 * Copyright (C) 2017 - 2021 Walmart Inc.
 * -----
 */
package com.walmartlabs.concord.plugins.argocd.model;

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

import com.walmartlabs.concord.plugins.argocd.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

public class ApplicationTest {

    @Test
    public void testUnmarshall() throws Exception {
        Application app = read("application.json", Application.class);
        assertNotNull(app.status());
        assertNotNull(app.status().reconciledAt());
        assertEquals("2021-12-21T20:52:05Z", app.status().reconciledAt().toString());
    }

    private static <T> T read(String resourceName, Class<T> clazz) throws IOException {
        try (InputStream in = ApplicationTest.class.getResourceAsStream(resourceName)) {
            return new ObjectMapper().readValue(in, clazz);
        }
    }
}
