package com.walmartlabs.concord.plugins.terraform;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2026 Walmart Inc.
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
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GitSshWrapperTest {

    @TempDir
    Path workDir;

    @Test
    void shouldConfigureHttpGitCredentials() throws Exception {
        Map<String, Object> cfg = new HashMap<>();
        cfg.put(TaskConstants.GIT_AUTH_KEY, Collections.singletonMap(
                GitSshWrapper.HTTP_KEY, Map.of(
                        "password", "ab+c:d@z")));

        GitSshWrapper w = GitSshWrapper.createFrom((orgName, secretName, password) -> {
            throw new IllegalStateException("should not be called");
        }, workDir, cfg, false);

        Map<String, String> env = w.updateEnv(workDir, new HashMap<>());

        assertTrue(env.containsKey("GIT_ASKPASS"));
        assertEquals("0", env.get("GIT_TERMINAL_PROMPT"));
        assertTrue(env.containsKey("GIT_SSH_COMMAND"));
        assertFalse(env.containsKey("GIT_CONFIG_COUNT"));

        Path askPassPath = Path.of(env.get("GIT_ASKPASS"));
        assertTrue(Files.exists(askPassPath));

        String askPassScript = Files.readString(askPassPath);
        assertTrue(askPassScript.contains("x-access-token"));
        assertTrue(askPassScript.contains("ab+c:d@z"));

        w.cleanup();
        assertFalse(Files.exists(askPassPath));
    }

    @Test
    void shouldRejectHttpAuthWithUrl() {
        Map<String, Object> cfg = new HashMap<>();
        cfg.put(TaskConstants.GIT_AUTH_KEY, Collections.singletonMap(
                GitSshWrapper.HTTP_KEY, Map.of(
                        "url", "ssh://github.com/example/repo.git",
                        "password", "secret")));

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> GitSshWrapper.createFrom((orgName, secretName, password) -> null, workDir, cfg, false));
        assertTrue(e.getMessage().contains("Unrecognized options"));
    }

    @Test
    void shouldPreferGitAuthOverDeprecatedGitSsh() throws Exception {
        Map<String, Object> cfg = new HashMap<>();
        cfg.put(TaskConstants.GIT_AUTH_KEY, Collections.singletonMap(
                GitSshWrapper.HTTP_KEY, Map.of(
                        "password", "from-git-auth")));
        cfg.put(TaskConstants.GIT_SSH_KEY, Collections.singletonMap(
                GitSshWrapper.HTTP_KEY, Map.of(
                        "password", "from-git-ssh")));

        GitSshWrapper w = GitSshWrapper.createFrom((orgName, secretName, password) -> null, workDir, cfg, false);
        Map<String, String> env = w.updateEnv(workDir, new HashMap<>());

        Path askPassPath = Path.of(env.get("GIT_ASKPASS"));
        String askPassScript = Files.readString(askPassPath);
        assertTrue(askPassScript.contains("from-git-auth"));
        assertFalse(askPassScript.contains("from-git-ssh"));

        w.cleanup();
    }

    @Test
    void shouldConfigureSshPrivateKeyFromGitAuth() throws Exception {
        Path privateKey = Files.createTempFile(workDir, "test", ".key");
        Map<String, Object> cfg = new HashMap<>();
        cfg.put(TaskConstants.GIT_AUTH_KEY, Collections.singletonMap(
                GitSshWrapper.PRIVATE_KEYS_KEY, Collections.singletonList(privateKey.toString())));

        GitSshWrapper w = GitSshWrapper.createFrom((orgName, secretName, password) -> null, workDir, cfg, false);
        Map<String, String> env = w.updateEnv(workDir, new HashMap<>());

        assertTrue(env.containsKey("GIT_SSH_COMMAND"));
        assertEquals("0", env.get("GIT_TERMINAL_PROMPT"));
        assertFalse(env.containsKey("GIT_CONFIG_COUNT"));

        Path wrapperPath = Path.of(env.get("GIT_SSH_COMMAND"));
        assertTrue(Files.exists(wrapperPath));
        String wrapperScript = Files.readString(wrapperPath);
        assertTrue(wrapperScript.contains("IdentityFile=" + privateKey));

        w.cleanup();
        assertFalse(Files.exists(wrapperPath));
    }

    @Test
    void shouldRejectHttpAuthList() {
        Map<String, Object> cfg = new HashMap<>();
        cfg.put(TaskConstants.GIT_AUTH_KEY, Collections.singletonMap(
                GitSshWrapper.HTTP_KEY, Collections.singletonList(Map.of("password", "secret"))));

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> GitSshWrapper.createFrom((orgName, secretName, password) -> null, workDir, cfg, false));
        assertTrue(e.getMessage().contains("must be an object"));
    }

    @Test
    void shouldDisableTerminalPromptWithoutGitAuthHttp() throws Exception {
        GitSshWrapper w = GitSshWrapper.createFrom((orgName, secretName, password) -> null, workDir, Collections.emptyMap(), false);
        Map<String, String> env = w.updateEnv(workDir, new HashMap<>());

        assertEquals("0", env.get("GIT_TERMINAL_PROMPT"));
        assertFalse(env.containsKey("GIT_ASKPASS"));

        w.cleanup();
    }

}
