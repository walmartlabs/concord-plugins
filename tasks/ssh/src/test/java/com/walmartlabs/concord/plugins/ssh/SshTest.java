package com.walmartlabs.concord.plugins.ssh;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2025 Walmart Inc., Concord Authors
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

import com.walmartlabs.concord.runtime.v2.sdk.MapBackedVariables;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult.SimpleResult;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class SshTest {

    private static GenericContainer<?> sshd;
    private static final String TEST_USER = "root";
    private static final String TEST_PASSWORD = "foobar";

    @BeforeAll
    public static void setUp() {
        sshd = new GenericContainer<>("testcontainers/sshd:1.3.0")
                .withExposedPorts(22)
                .withEnv("USERNAME", TEST_USER)
                .withEnv("PASSWORD", TEST_PASSWORD)
                .waitingFor(Wait.forListeningPort())
                .withStartupTimeout(Duration.ofMinutes(1));

        sshd.start();
    }

    @AfterAll
    public static void tearDown() {
        sshd.stop();
    }

    @Test
    public void sshTask() throws Exception {
        var task = new SshTask();
        var result = (SimpleResult) task.execute(new MapBackedVariables(Map.of(
                "host", sshd.getHost(),
                "port", sshd.getMappedPort(22),
                "user", TEST_USER,
                "password", TEST_PASSWORD,
                "run", "echo 'Hello from SSH!'"
        )));
        var stdout = result.values().get("stdout").toString();
        assertTrue(stdout.contains("Hello from SSH!"));
    }

    @Test
    public void scpTask(@TempDir Path tempDir) throws Exception {
        var src = tempDir.resolve("test.txt");
        Files.writeString(src, "Hello from SCP!");

        // upload the file

        var scpTask = new ScpTask();
        scpTask.execute(new MapBackedVariables(Map.of(
                "host", sshd.getHost(),
                "port", sshd.getMappedPort(22),
                "user", TEST_USER,
                "password", TEST_PASSWORD,
                "src", src.toAbsolutePath().toString(),
                "dest", "/tmp/test.txt"
        )));

        // check the remote file's content

        var sshTask = new SshTask();
        var result = (SimpleResult) sshTask.execute(new MapBackedVariables(Map.of(
                "host", sshd.getHost(),
                "port", sshd.getMappedPort(22),
                "user", TEST_USER,
                "password", TEST_PASSWORD,
                "run", "cat /tmp/test.txt"
        )));
        var stdout = result.values().get("stdout").toString();
        assertTrue(stdout.contains("Hello from SCP!"));
    }
}
