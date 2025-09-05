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

import com.walmartlabs.concord.runtime.v2.sdk.Task;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import com.walmartlabs.concord.runtime.v2.sdk.Variables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import static com.walmartlabs.concord.plugins.ssh.JSchUtils.initJsch;
import static com.walmartlabs.concord.plugins.ssh.JSchUtils.sshExec;
import static java.nio.charset.StandardCharsets.UTF_8;

@Named("ssh")
public class SshTask implements Task {

    private static final Logger log = LoggerFactory.getLogger(SshTask.class);

    @Override
    public TaskResult execute(Variables input) throws Exception {
        var host = input.assertString("host");
        var command = input.assertString("run");
        var user = input.assertString("user");
        var password = input.getString("password");
        var identities = input.getList("identities", List.<String>of());
        var port = input.getInt("port", 22);
        var timeout = input.getInt("timeout", 30000);

        var stdout = new StringBuilder();
        var stderr = new StringBuilder();

        var jsch = initJsch(identities);
        try (var exec = sshExec(jsch, user, password, host, port, timeout)) {
            var channel = exec.channel();
            channel.setCommand(command);

            var stdoutReader = new BufferedReader(new InputStreamReader(channel.getInputStream(), UTF_8));
            var stderrReader = new BufferedReader(new InputStreamReader(channel.getErrStream(), UTF_8));

            channel.connect();

            var stdoutThread = new Thread(() -> {
                try {
                    String line;
                    while ((line = stdoutReader.readLine()) != null) {
                        log.info("{}", line);
                        stdout.append(line).append("\n");
                    }
                } catch (IOException e) {
                    log.error("Error reading stdout", e);
                }
            }, "STDOUT");

            var stderrThread = new Thread(() -> {
                try {
                    String line;
                    while ((line = stderrReader.readLine()) != null) {
                        log.info("{}", line);
                        stderr.append(line).append("\n");
                    }
                } catch (IOException e) {
                    log.error("Error reading stderr", e);
                }
            }, "STDERR");

            stdoutThread.start();
            stderrThread.start();

            while (channel.isConnected()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            try {
                stdoutThread.join();
                stderrThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            var exitCode = channel.getExitStatus();
            if (exitCode != 0) {
                throw new RuntimeException("Non-zero exit code: " + exitCode);
            }

            return TaskResult.success()
                    .value("stdout", stdout.toString())
                    .value("stderr", stderr.toString());
        }
    }
}
