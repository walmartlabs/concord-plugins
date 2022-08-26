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

import com.walmartlabs.concord.plugins.xmlutils.v2.XmlUtilsTaskV2;
import com.walmartlabs.concord.runtime.v2.sdk.Compiler;
import com.walmartlabs.concord.runtime.v2.sdk.*;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class XmlUtilsTaskV2Test {

    @Test
    public void testXpathString() throws Exception {
        String file = "test.xml";

        URL src = ClassLoader.getSystemResource(file);
        Context ctx = new MockContextV2(Paths.get(src.toURI()).getParent().toAbsolutePath());
        XmlUtilsTaskV2 t = new XmlUtilsTaskV2(ctx);

        String value = t.xpathString(file, "/project/version/text()");
        assertEquals("1.52.1-SNAPSHOT", value);

        List<String> l = t.xpathListOfStrings(file, "/project/*[local-name()='groupId' or local-name()='artifactId' or local-name()='version']/text()");
        assertEquals(3, l.size());
        assertTrue(l.contains("com.walmartlabs.concord"));
        assertTrue(l.contains("parent"));
        assertTrue(l.contains("1.52.1-SNAPSHOT"));
    }

    @Test
    public void testMavenGav() throws Exception {
        URL src = ClassLoader.getSystemResource("test.xml");
        Context ctx = new MockContextV2(Paths.get(src.toURI()).getParent().toAbsolutePath());
        XmlUtilsTaskV2 t = new XmlUtilsTaskV2(ctx);

        Map<String, String> m = t.mavenGav( "test.xml");
        assertEquals(3, m.size());
        assertEquals("com.walmartlabs.concord", m.get("groupId"));
        assertEquals("parent", m.get("artifactId"));
        assertEquals("1.52.1-SNAPSHOT", m.get("version"));

        m = t.mavenGav("test2.xml");
        assertEquals(3, m.size());
        assertEquals("com.walmartlabs.concord.plugins", m.get("groupId"));
        assertEquals("xml-tasks", m.get("artifactId"));
        assertEquals("1.27.1-SNAPSHOT", m.get("version"));
    }

    private static class MockContextV2 implements Context {
        private final Path workDir;

        public MockContextV2(Path workDir) {
            this.workDir = workDir;
        }

        @Override
        public Path workingDirectory() {
            return workDir;
        }

        @Override
        public UUID processInstanceId() {
            throw new RuntimeException("not implemented context method: processInstanceId()");
        }

        @Override
        public Variables variables() {
            throw new RuntimeException("not implemented context method: variables()");
        }

        @Override
        public Variables defaultVariables() {
            throw new RuntimeException("not implemented context method: defaultVariables()");
        }

        @Override
        public FileService fileService() {
            throw new RuntimeException("not implemented context method: fileService()");
        }

        @Override
        public DockerService dockerService() {
            throw new RuntimeException("not implemented context method: dockerService()");
        }

        @Override
        public SecretService secretService() {
            throw new RuntimeException("not implemented context method: secretService()");
        }

        @Override
        public LockService lockService() {
            throw new RuntimeException("not implemented context method: lockService()");
        }

        @Override
        public ApiConfiguration apiConfiguration() {
            throw new RuntimeException("not implemented context method: implemented()");
        }

        @Override
        public ProcessConfiguration processConfiguration() {
            throw new RuntimeException("not implemented context method: processConfiguration()");
        }

        @Override
        public Execution execution() {
            throw new RuntimeException("not implemented context method: execution()");
        }

        @Override
        public Compiler compiler() {
            throw new RuntimeException("not implemented context method: compiler()");
        }

        @Override
        public <T> T eval(Object o, Class<T> aClass) {
            throw new RuntimeException("not implemented context method: eval()");
        }

        @Override
        public <T> T eval(Object o, Map<String, Object> map, Class<T> aClass) {
            throw new RuntimeException("not implemented context method: eval()");
        }

        @Override
        public void suspend(String s) {
            throw new RuntimeException("not implemented context method: suspend()");
        }

        @Override
        public void reentrantSuspend(String s, Map<String, Serializable> map) {
            throw new RuntimeException("not implemented context method: reentrantSuspend()");
        }
    }
}
