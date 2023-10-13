package com.walmartlabs.concord.plugins.git;

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

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.walmartlabs.concord.common.secret.KeyPair;
import com.walmartlabs.concord.common.secret.UsernamePassword;
import com.walmartlabs.concord.sdk.Secret;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.transport.*;
import org.eclipse.jgit.util.FS;

import java.nio.file.Path;
import java.util.Properties;

public class JGitClient implements GitClient {

    @Override
    public void cloneRepo(String uri, String branchName, Secret secret, Path dst) throws Exception {
        try (Git repo = Git.cloneRepository()
                .setURI(uri)
                .setBranch(branchName)
                .setDirectory(dst.toFile())
                .setTransportConfigCallback(createTransportConfigCallback(secret))
                .call()) {

            // check if the branch actually exists
            if (branchName != null) {
                repo.checkout()
                        .setName(branchName)
                        .call();
            }
        }
    }

    public static TransportConfigCallback createTransportConfigCallback(Secret secret) {
        if (secret == null) {
            return null;
        }

        if (secret instanceof UsernamePassword) {
            return createHttpTransportConfigCallback((UsernamePassword) secret);
        } else if (secret instanceof KeyPair) {
            return createSshTransportConfigCallback((KeyPair) secret);
        } else if (secret instanceof TokenSecret) {
            return createHttpTransportConfigCallback(new UsernamePassword(((TokenSecret) secret).getToken(), "".toCharArray()));
        }

        // empty callback
        return transport -> {
        };
    }

    private static TransportConfigCallback createSshTransportConfigCallback(KeyPair secret) {
        return transport -> {
            if (transport instanceof SshTransport) {
                configureSshTransport((SshTransport) transport, secret);
            } else {
                throw new IllegalArgumentException("Use SSH GIT URL ");
            }
        };
    }

    private static TransportConfigCallback createHttpTransportConfigCallback(UsernamePassword secret) {
        return transport -> {
            if (transport instanceof HttpTransport) {
                transport.setCredentialsProvider(
                        new UsernamePasswordCredentialsProvider(secret.getUsername(), secret.getPassword()));
            } else {
                throw new IllegalArgumentException("Use HTTP(S) GIT URL");
            }
        };
    }

    private static void configureSshTransport(SshTransport t, KeyPair secret) {
        SshSessionFactory f = new JschConfigSessionFactory() {
            @Override
            protected JSch createDefaultJSch(FS fs) throws JSchException {
                JSch d = super.createDefaultJSch(fs);
                d.removeAllIdentity();
                d.addIdentity("key", secret.getPrivateKey(), null, null);
                return d;
            }

            @Override
            protected void configure(OpenSshConfig.Host hc, Session session) {
                Properties config = new Properties();
                config.put("StrictHostKeyChecking", "no");
                session.setConfig(config);
            }
        };

        t.setSshSessionFactory(f);
    }
}
