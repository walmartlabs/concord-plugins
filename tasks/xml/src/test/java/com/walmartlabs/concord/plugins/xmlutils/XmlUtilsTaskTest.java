package com.walmartlabs.concord.plugins.xmlutils;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2020 Walmart Inc.
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

import java.net.URL;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class XmlUtilsTaskTest {

    @Test
    public void testXpathString() throws Exception {
        String file = "test.xml";

        URL src = ClassLoader.getSystemResource(file);
        String workDir = Paths.get(src.toURI()).getParent().toAbsolutePath().toString();

        XmlUtilsTask t = new XmlUtilsTask();

        String value = t.xpathString(workDir, file, "/project/version/text()");
        assertEquals("1.52.1-SNAPSHOT", value);

        List<String> l = t.xpathListOfStrings(workDir, file, "/project/*[local-name()='groupId' or local-name()='artifactId' or local-name()='version']/text()");
        assertEquals(3, l.size());
        assertTrue(l.contains("com.walmartlabs.concord"));
        assertTrue(l.contains("parent"));
        assertTrue(l.contains("1.52.1-SNAPSHOT"));
    }

    @Test
    public void testMavenGav() throws Exception {
        URL src = ClassLoader.getSystemResource("test.xml");
        String workDir = Paths.get(src.toURI()).getParent().toAbsolutePath().toString();

        XmlUtilsTask t = new XmlUtilsTask();

        Map<String, String> m = t.mavenGav(workDir, "test.xml");
        assertEquals(3, m.size());
        assertEquals("com.walmartlabs.concord", m.get("groupId"));
        assertEquals("parent", m.get("artifactId"));
        assertEquals("1.52.1-SNAPSHOT", m.get("version"));

        m = t.mavenGav(workDir, "test2.xml");
        assertEquals(3, m.size());
        assertEquals("com.walmartlabs.concord.plugins", m.get("groupId"));
        assertEquals("xml-tasks", m.get("artifactId"));
        assertEquals("1.27.1-SNAPSHOT", m.get("version"));
    }
}
