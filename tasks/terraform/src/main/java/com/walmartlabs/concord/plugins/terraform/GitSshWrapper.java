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
 * script using the specified private key files and/or Concord secrets and
 * optionally configures Git HTTP(S) credentials for password-based module cloning.
 */
public class GitSshWrapper {

    private static final Logger log = LoggerFactory.getLogger(GitSshWrapper.class);

    public static final String PRIVATE_KEYS_KEY = "privateKeys";
    public static final String SECRETS_KEY = "secrets";
    public static final String HTTP_KEY = "http";

    private static final String ORG_KEY = "org";
    private static final String SECRET_NAME_KEY = "secretName";
    private static final String PASSWORD_KEY = "password";
    private static final String USERNAME_KEY = "username";
    private static final String DEFAULT_HTTP_USERNAME = "x-access-token";

    private static final String SCRIPT_PERMISSIONS = "r-xr-xr--";

    @SuppressWarnings("unchecked")
    public static GitSshWrapper createFrom(SecretProvider secretProvider,
                                           Path workDir,
                                           Map<String, Object> cfg,
                                           boolean debug) throws Exception {

        Object gitAuthCfg = cfg.get(TaskConstants.GIT_AUTH_KEY);
        Object gitSshCfg = cfg.get(TaskConstants.GIT_SSH_KEY);

        if (gitSshCfg != null) {
            log.warn("'{}' is deprecated and will be removed in a future release. Use '{}' instead.",
                    TaskConstants.GIT_SSH_KEY, TaskConstants.GIT_AUTH_KEY);
        }

        String rootKey = TaskConstants.GIT_AUTH_KEY;
        Object v = Collections.emptyMap();
        if (gitAuthCfg != null) {
            v = gitAuthCfg;
        } else if (gitSshCfg != null) {
            rootKey = TaskConstants.GIT_SSH_KEY;
            v = gitSshCfg;
        }

        if (!(v instanceof Map)) {
            throw new IllegalArgumentException("'" + rootKey + "' must be a object, got: " + v);
        }

        Map<String, Object> m = (Map<String, Object>) v;

        List<Path> externalKeys = getExternalPrivateKeys(workDir, m, rootKey, debug);
        List<Path> exportedKeys = exportSecrets(secretProvider, m, rootKey, debug);
        HttpAuth httpAuth = getHttpAuth(m, rootKey, debug);

        return new GitSshWrapper(externalKeys, exportedKeys, httpAuth, debug);
    }

    @SuppressWarnings("unchecked")
    private static List<Path> getExternalPrivateKeys(Path workDir, Map<String, Object> m, String rootKey, boolean debug) {
        Object v = m.getOrDefault(PRIVATE_KEYS_KEY, Collections.emptyList());
        if (!(v instanceof List)) {
            throw new IllegalArgumentException("'" + rootKey + "." + PRIVATE_KEYS_KEY + "' must be a list of paths, got: " + v);
        }

        List<Path> result = new ArrayList<>();
        for (Object o : (List<Object>) v) {
            Path p;

            if (o instanceof Path) {
                p = (Path) o;
            } else if (o instanceof String) {
                p = workDir.resolve((String) o);
            } else {
                throw new IllegalArgumentException("'" + rootKey + "." + PRIVATE_KEYS_KEY + "' elements must be private key paths, got: " + o);
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
    private static List<Path> exportSecrets(SecretProvider secretProvider,
                                            Map<String, Object> m,
                                            String rootKey,
                                            boolean debug) throws Exception {

        Object v = m.getOrDefault(SECRETS_KEY, Collections.emptyList());
        if (!(v instanceof List)) {
            throw new IllegalArgumentException("'" + rootKey + "." + SECRETS_KEY + "' must be a list of secrets to export, got: " + v);
        }

        List<Path> result = new ArrayList<>();
        for (Object o : (List<Object>) v) {
            if (!(o instanceof Map)) {
                throw new IllegalArgumentException("'" + rootKey + "." + SECRETS_KEY + "' values must be Concord secrets references, got: " + o);
            }

            Path p = exportSecret(secretProvider, (Map<String, Object>) o, rootKey, debug);
            result.add(p);
        }
        return result;
    }

    private static Path exportSecret(SecretProvider secretProvider,
                                     Map<String, Object> m,
                                     String rootKey,
                                     boolean debug) throws Exception {

        m = new HashMap<>(m);

        String secretName = removeString(m, SECRET_NAME_KEY);
        if (secretName == null) {
            throw new IllegalArgumentException("'" + SECRET_NAME_KEY + "' is required, got: " + m);
        }

        String orgName = removeString(m, ORG_KEY);
        String password = removeString(m, PASSWORD_KEY);

        if (!m.isEmpty()) {
            throw new IllegalArgumentException("Unrecognized options of '" + rootKey + "." + SECRETS_KEY + "': " + m.keySet());
        }

        Path p = secretProvider.getPrivateKey(orgName, secretName, password);
        if (debug) {
            log.info("exportSecret -> using {}/{} secret", orgName, secretName);
        }

        return p;
    }

    @SuppressWarnings("unchecked")
    private static HttpAuth getHttpAuth(Map<String, Object> m, String rootKey, boolean debug) {
        Object v = m.get(HTTP_KEY);
        if (v == null) {
            return null;
        }

        if (!(v instanceof Map)) {
            throw new IllegalArgumentException("'" + rootKey + "." + HTTP_KEY + "' must be an object, got: " + v);
        }

        HttpAuth a = parseHttpAuth((Map<String, Object>) v, rootKey);
        if (debug) {
            log.info("getHttpAuth -> using HTTP git auth with username {}", a.username);
        }
        return a;
    }

    private static HttpAuth parseHttpAuth(Map<String, Object> m, String rootKey) {
        m = new HashMap<>(m);

        String password = removeString(m, PASSWORD_KEY);
        if (password == null) {
            throw new IllegalArgumentException("'" + PASSWORD_KEY + "' is required, got: " + m);
        }

        String username = removeString(m, USERNAME_KEY);
        if (username == null) {
            username = DEFAULT_HTTP_USERNAME;
        }

        if (!m.isEmpty()) {
            throw new IllegalArgumentException("Unrecognized options of '" + rootKey + "." + HTTP_KEY + "': " + m.keySet());
        }

        return new HttpAuth(username, password);
    }

    private final List<Path> externalPrivateKeys;
    private final List<Path> exportedPrivateKeys;
    private final HttpAuth httpAuth;
    private final boolean debug;

    // path to the generated SSH wrapper script, removed in cleanup()
    private Path wrapperPath;
    // path to generated askpass file, removed in cleanup()
    private Path askPassPath;

    private GitSshWrapper(List<Path> externalPrivateKeys, List<Path> exportedPrivateKeys, HttpAuth httpAuth, boolean debug) {
        this.externalPrivateKeys = externalPrivateKeys;
        this.exportedPrivateKeys = exportedPrivateKeys;
        this.httpAuth = httpAuth;
        this.debug = debug;
    }

    public Map<String, String> updateEnv(Path workDir, Map<String, String> m) throws IOException {
        this.wrapperPath = generateScript(workDir);
        String s = wrapperPath.toAbsolutePath().toString();
        m.put("GIT_SSH_COMMAND", s);
        m.put("GIT_TERMINAL_PROMPT", "0");

        if (httpAuth != null) {
            this.askPassPath = generateAskPassScript(workDir, httpAuth);
            m.put("GIT_ASKPASS", askPassPath.toAbsolutePath().toString());
        }

        return m;
    }

    public void cleanup() throws IOException {
        if (wrapperPath != null) {
            Files.deleteIfExists(wrapperPath);
        }

        if (askPassPath != null) {
            Files.deleteIfExists(askPassPath);
        }

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

        String cmd = sb.append("\n").toString();

        Path dst = Files.createTempFile(dir, "gitSsh", ".sh");
        Files.write(dst, cmd.getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
        Files.setPosixFilePermissions(dst, PosixFilePermissions.fromString(SCRIPT_PERMISSIONS));

        return dst;
    }

    private static void addIdentityFile(StringBuilder sb, Path p) {
        sb.append(" -o IdentityFile=").append(p.toString());
    }

    private Path generateAskPassScript(Path dir, HttpAuth auth) throws IOException {
        String cmd = "#!/bin/sh" + lineSeparator() +
                "case \"$1\" in" + lineSeparator() +
                "  *sername*) printf '%s\\n' " + shellQuote(auth.username) + " ;;" + lineSeparator() +
                "  *assword*) printf '%s\\n' " + shellQuote(auth.password) + " ;;" + lineSeparator() +
                "  *) printf '\\n' ;;" + lineSeparator() +
                "esac\n";

        Path dst = Files.createTempFile(dir, "gitAskPass", ".sh");
        Files.write(dst, cmd.getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
        Files.setPosixFilePermissions(dst, PosixFilePermissions.fromString(SCRIPT_PERMISSIONS));
        return dst;
    }

    private static String shellQuote(String value) {
        return "'" + value.replace("'", "'\"'\"'") + "'";
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

    public interface SecretProvider {

        Path getPrivateKey(String orgName, String secretName, String password) throws Exception;
    }

    private record HttpAuth(String username, String password) {
    }

}
