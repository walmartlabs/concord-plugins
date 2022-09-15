/**
 * Concord
 * -----
 * Copyright (C) 2017 - 2021 Walmart Inc.
 * -----
 */
package com.walmartlabs.concord.plugins.argocd;

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

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class MapUtilsTest {

    @Test
    public void testGet() {
        {
            Map<String, Object> m = new HashMap<>();
            m.put("a", "1");

            assertEquals("1", MapUtils.get(m, "a", null));
            assertNull(MapUtils.get(m, "a.b", null));
        }

        {
            Map<String, Object> m = new HashMap<>();
            m.put("a", Collections.singletonMap("b", Collections.singletonMap("c", "one")));
            assertEquals("one", MapUtils.get(m, "a.b.c", null));
            assertEquals(Collections.singletonMap("c", "one"), MapUtils.get(m, "a.b", null));
        }
    }

    @Test
    public void testSet() {
        {
            Map<String, Object> m = Collections.emptyMap();
            m = MapUtils.set(m, "a.b", "one");
            assertEquals("one", MapUtils.get(m, "a.b", null));
        }

        {
            Map<String, Object> m = new HashMap<>();

            m = MapUtils.set(m, "k", "one");

            assertEquals("one", m.get("k"));
        }

        {
            Map<String, Object> m = new HashMap<>();

            m = MapUtils.set(m, "a.b.c", "one");

            assertEquals("one", MapUtils.get(m, "a.b.c", null));
        }

        {
            Map<String, Object> m = new HashMap<>();
            m.put("a", Collections.emptyMap());

            m = MapUtils.set(m, "a.b.c", "one");

            assertEquals("one", MapUtils.get(m, "a.b.c", null));
            assertNull(MapUtils.get(m, "a.b.c.d", null));
        }
    }
}
