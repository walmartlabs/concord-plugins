package com.walmartlabs.concord.plugins.terraform.backend;

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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.walmartlabs.concord.plugins.terraform.TaskConstants;
import com.walmartlabs.concord.sdk.MapUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

import static com.walmartlabs.concord.plugins.terraform.TaskConstants.*;

public class RemoteBackend extends CommonBackend {

    private final Path workDir;

    public RemoteBackend(String id, boolean debug, Map<String, Object> backendParameters, ObjectMapper objectMapper, Path workDir) {
        super(id, debug, backendParameters, objectMapper);
        this.workDir = workDir;
    }

    @Override
    public String getId() {
        return "remote";
    }

    @Override
    public boolean supportsOutFiles() {
        // the "remote" backend doesn't support "out" files
        return false;
    }

    @Override
    public Map<String, String> prepareEnv(Map<String, Object> cfg) {
        Map<String, Object> backend = MapUtils.getMap(cfg, TaskConstants.BACKEND_KEY, null);
        if (backend == null) {
            return Collections.emptyMap();
        }

        String tfCliCfgFile = getRemoteBackendTfCfgFile(backend, this.workDir);
        if (tfCliCfgFile != null) {
            // TF_CLI_CONFIG_FILE is the only way to pass the remote's credentials
            return Collections.singletonMap(TaskConstants.TF_CLI_CONFIG_FILE_KEY, tfCliCfgFile);
        }

        return Collections.emptyMap();
    }

    private static String getRemoteBackendTfCfgFile(Map<String, Object> backend, Path baseDir) {
        Map<String, Object> backendParameters = MapUtils.getMap(backend, BACKEND_REMOTE_KEY, null);
        if (!backendParameters.containsKey(HOSTNAME_KEY) || !backendParameters.containsKey(TOKEN_KEY)) {
            return null;
        }

        String host = MapUtils.assertString(backendParameters, HOSTNAME_KEY);
        String token = MapUtils.assertString(backendParameters, TOKEN_KEY);
        String cfg = String.format("credentials \"%s\" { \ntoken = \"%s\" \n}", host, token);

        try {
            Path tmpDir = assertTempDir(baseDir);
            Path dst = Files.createTempFile(tmpDir, "terraformrc", ".bin");
            Files.write(dst, cfg.getBytes());
            return baseDir.relativize(dst).toString();
        } catch (IOException e) {
            throw new RuntimeException("Error while creating the remote backend's configuration file: " + e.getMessage(), e);
        }
    }

    private static Path assertTempDir(Path baseDir) throws IOException {
        Path p = baseDir.resolve(".tmp");
        if (!Files.exists(p)) {
            Files.createDirectories(p);
        }

        return p;
    }
}
