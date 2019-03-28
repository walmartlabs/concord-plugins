package com.walmartlabs.concord.plugins.terraform;

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

import com.walmartlabs.concord.sdk.Context;
import com.walmartlabs.concord.sdk.SecretService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.*;

import static java.lang.System.lineSeparator;

/**
 * Generates a <a href="https://git-scm.com/docs/git#Documentation/git.txt-codeGITSSHCOMMANDcode">GIT_SSH_COMMAND</a>
 * script using the specified private key files and/or Concord secrets.
 */
public class GitSshWrapper {

    private static final Logger log = LoggerFactory.getLogger(GitSshWrapper.class);

    public static final String PRIVATE_KEYS_KEY = "privateKeys";
    public static final String SECRETS_KEY = "secrets";

    private static final String ORG_KEY = "org";
    private static final String SECRET_NAME_KEY = "secretName";
    private static final String PASSWORD_KEY = "password";

    private static final String SCRIPT_PERMISSIONS = "r-xr-xr--";

    @SuppressWarnings("unchecked")
    public static GitSshWrapper createFrom(SecretService secretService,
                                           Context ctx,
                                           String instanceId,
                                           Path workDir,
                                           Map<String, Object> cfg,
                                           boolean debug) throws Exception {

        Object v = cfg.getOrDefault(Constants.GIT_SSH_KEY, Collections.emptyMap());
        if (!(v instanceof Map)) {
            throw new IllegalArgumentException("'" + Constants.GIT_SSH_KEY + "' must be a object, got: " + v);
        }

        Map<String, Object> m = (Map<String, Object>) v;

        List<Path> externalKeys = getExternalPrivateKeys(workDir, m, debug);
        List<Path> exportedKeys = exportSecrets(secretService, ctx, instanceId, workDir, m, debug);

        return new GitSshWrapper(externalKeys, exportedKeys, debug);
    }

    @SuppressWarnings("unchecked")
    private static List<Path> getExternalPrivateKeys(Path workDir, Map<String, Object> m, boolean debug) {
        Object v = m.getOrDefault(PRIVATE_KEYS_KEY, Collections.emptyList());
        if (!(v instanceof List)) {
            throw new IllegalArgumentException("'" + Constants.GIT_SSH_KEY + "." + PRIVATE_KEYS_KEY + "' must be a list of paths, got: " + v);
        }

        List<Path> result = new ArrayList<>();
        for (Object o : (List<Object>) v) {
            Path p;

            if (o instanceof Path) {
                p = (Path) o;
            } else if (o instanceof String) {
                p = workDir.resolve((String) o);
            } else {
                throw new IllegalArgumentException("'" + Constants.GIT_SSH_KEY + "." + PRIVATE_KEYS_KEY + "' elements must be private key paths, got: " + o);
            }

            if (!p.isAbsolute()) {
                throw new IllegalArgumentException("The private key must be an absolute path, got: " + p);
            }

            if (!Files.exists(p)) {
                throw new IllegalStateException("The private key file doesn't exist: " + p);
            }

            if (debug) {
                log.info("getExternalPrivateKeys -> using {}", p);
            }

            result.add(p);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static List<Path> exportSecrets(SecretService secretService,
                                            Context ctx,
                                            String instanceId,
                                            Path workDir,
                                            Map<String, Object> m,
                                            boolean debug) throws Exception {

        Object v = m.getOrDefault(SECRETS_KEY, Collections.emptyList());
        if (!(v instanceof List)) {
            throw new IllegalArgumentException("'" + Constants.GIT_SSH_KEY + "." + SECRETS_KEY + "' must be a list of secrets to export, got: " + v);
        }

        List<Path> result = new ArrayList<>();
        for (Object o : (List<Object>) v) {
            if (!(o instanceof Map)) {
                throw new IllegalArgumentException("'" + Constants.GIT_SSH_KEY + "." + SECRETS_KEY + "' values must be Concord secrets references, got: " + o);
            }

            Path p = exportSecret(secretService, ctx, instanceId, workDir, (Map<String, Object>) o, debug);
            result.add(p);
        }
        return result;
    }

    private static Path exportSecret(SecretService secretService,
                                     Context ctx,
                                     String instanceId,
                                     Path workDir,
                                     Map<String, Object> m,
                                     boolean debug) throws Exception {

        m = new HashMap<>(m);

        String secretName = removeString(m, SECRET_NAME_KEY);
        if (secretName == null) {
            throw new IllegalArgumentException("'" + SECRET_NAME_KEY + "' is required, got: " + m);
        }

        String orgName = removeString(m, ORG_KEY);
        String password = removeString(m, PASSWORD_KEY);

        if (!m.isEmpty()) {
            throw new IllegalArgumentException("Unrecognized options of '" + Constants.GIT_SSH_KEY + "." + SECRETS_KEY + "': " + m.keySet());
        }

        Map<String, String> secret = secretService.exportKeyAsFile(ctx, instanceId, workDir.toAbsolutePath().toString(), orgName, secretName, password);

        if (debug) {
            log.info("exportSecret -> using {}/{} secret", orgName, secretName);
        }

        return workDir.resolve(secret.get("private"));
    }

    private final List<Path> externalPrivateKeys;
    private final List<Path> exportedPrivateKeys;
    private final boolean debug;

    private GitSshWrapper(List<Path> externalPrivateKeys, List<Path> exportedPrivateKeys, boolean debug) {
        this.externalPrivateKeys = externalPrivateKeys;
        this.exportedPrivateKeys = exportedPrivateKeys;
        this.debug = debug;
    }

    public Map<String, String> updateEnv(Path workDir, Map<String, String> m) throws IOException {
        String s = generateScript(workDir).toAbsolutePath().toString();
        m.put("GIT_SSH_COMMAND", s);
        return m;
    }

    public void cleanup() throws IOException {
        for (Path p : exportedPrivateKeys) {
            Files.deleteIfExists(p);
        }

        if (debug) {
            log.info("cleanup -> removed {} exported secrets...", exportedPrivateKeys.size());
        }
    }

    private Path generateScript(Path dir) throws IOException {
        StringBuilder sb = new StringBuilder("#!/bin/sh").append(lineSeparator())
                .append("ssh");

        // disable host key checking
        sb.append(" -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no");

        externalPrivateKeys.forEach(p -> addIdentityFile(sb, p));
        exportedPrivateKeys.forEach(p -> addIdentityFile(sb, p));

        sb.append(" $@");

        String cmd = sb.toString();

        Path dst = Files.createTempFile(dir, "gitSsh", ".sh");
        Files.write(dst, cmd.getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
        Files.setPosixFilePermissions(dst, PosixFilePermissions.fromString(SCRIPT_PERMISSIONS));

        return dst;
    }

    private static void addIdentityFile(StringBuilder sb, Path p) {
        sb.append(" -o IdentityFile=").append(p.toString());
    }

    private static String removeString(Map<String, Object> m, String k) {
        Object v = m.remove(k);
        if (v == null) {
            return null;
        }

        if (v instanceof String) {
            return (String) v;
        }

        throw new IllegalArgumentException("Expected a string value '" + k + "', got: " + v);
    }
}
