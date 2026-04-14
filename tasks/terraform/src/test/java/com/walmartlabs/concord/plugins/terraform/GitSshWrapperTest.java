package com.walmartlabs.concord.plugins.terraform;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2026 Walmart Inc., Concord Authors
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
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GitSshWrapperTest {

    @TempDir
    Path workDir;

    @Test
    void generatedScriptChecksHostKeysByDefault() throws Exception {
        Path privateKey = Files.createFile(workDir.resolve("private key"));
        Path knownHosts = Files.createFile(workDir.resolve("known hosts"));

        Map<String, Object> gitSsh = new HashMap<>();
        gitSsh.put(GitSshWrapper.PRIVATE_KEYS_KEY, List.of(privateKey.toString()));
        gitSsh.put(GitSshWrapper.KNOWN_HOSTS_KEY, knownHosts.getFileName().toString());

        GitSshWrapper wrapper = GitSshWrapper.createFrom(noSecrets(), workDir, Map.of(TaskConstants.GIT_SSH_KEY, gitSsh), false);
        Map<String, String> env = wrapper.updateEnv(workDir, new HashMap<>());

        String script = Files.readString(Paths.get(env.get("GIT_SSH_COMMAND")));

        assertTrue(script.contains("StrictHostKeyChecking=yes"));
        assertTrue(script.contains("UserKnownHostsFile=" + GitSshWrapper.shellQuote(knownHosts.toString())));
        assertTrue(script.contains("IdentityFile=" + GitSshWrapper.shellQuote(privateKey.toString())));
        assertTrue(script.contains(" \"$@\""));
        assertFalse(script.contains("UserKnownHostsFile=/dev/null"));

        wrapper.cleanup();
    }

    @Test
    void generatedScriptRequiresExplicitHostKeyOptOut() throws Exception {
        Map<String, Object> gitSsh = Map.of(GitSshWrapper.STRICT_HOST_KEY_CHECKING_KEY, false);

        GitSshWrapper wrapper = GitSshWrapper.createFrom(noSecrets(), workDir, Map.of(TaskConstants.GIT_SSH_KEY, gitSsh), false);
        Map<String, String> env = wrapper.updateEnv(workDir, new HashMap<>());

        String script = Files.readString(Paths.get(env.get("GIT_SSH_COMMAND")));

        assertTrue(script.contains("StrictHostKeyChecking=no"));
        assertFalse(script.contains("UserKnownHostsFile=/dev/null"));
        assertTrue(script.contains(" \"$@\""));

        wrapper.cleanup();
    }

    private static GitSshWrapper.SecretProvider noSecrets() {
        return (orgName, secretName, password) -> {
            throw new UnsupportedOperationException("No secrets expected");
        };
    }
}
