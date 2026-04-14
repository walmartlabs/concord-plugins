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

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public final class JSchUtils {

    private static final Logger log = LoggerFactory.getLogger(JSchUtils.class);

    public static Exec sshExec(JSch jsch, String user, String password, String host, int port, int timeout, boolean strictHostKeyChecking) throws JSchException {
        Session session = null;
        try {
            session = jsch.getSession(user, host, port);
            if (password != null) {
                session.setPassword(password);
                session.setConfig("PreferredAuthentications", "password");
            }
            if (strictHostKeyChecking) {
                session.setConfig("StrictHostKeyChecking", "yes");
            } else {
                session.setConfig("StrictHostKeyChecking", "no");
                session.setHostKeyRepository(new NoopHostKeyRepository());
            }

            session.connect(timeout);

            var channel = (ChannelExec) session.openChannel("exec");
            return new Exec(session, channel);
        } catch (JSchException e) {
            if (session != null) {
                session.disconnect();
            }
            throw e;
        }
    }

    public static JSch initJsch(List<String> identities, String knownHosts) throws JSchException {
        var jsch = new JSch();

        if (knownHosts != null && !knownHosts.isBlank()) {
            var p = Paths.get(knownHosts);
            if (!Files.exists(p) || !Files.isReadable(p)) {
                throw new JSchException("The 'knownHosts' file is not readable: " + knownHosts);
            }
            jsch.setKnownHosts(p.toString());
        }

        identities.stream()
                .map(Paths::get)
                .filter(p -> Files.exists(p) && Files.isReadable(p))
                .forEach(p -> {
                    try {
                        jsch.addIdentity(p.toString());
                    } catch (JSchException e) {
                        log.warn("Unable to add {} as a SSH identity: {}", p, e.getMessage());
                    }
                });

        return jsch;
    }

    public record Exec(Session session, ChannelExec channel) implements AutoCloseable {

        @Override
        public void close() {
            session.disconnect();
            channel.disconnect();
        }
    }

    private JSchUtils() {
    }
}
