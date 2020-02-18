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

import com.walmartlabs.concord.sdk.MapUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.walmartlabs.concord.plugins.terraform.Constants.*;

public final class Utils {

    public static Path getPath(Map<String, Object> cfg, String k, Path defaultValue) {
        String v = MapUtils.getString(cfg, k);
        if (v == null) {
            return defaultValue;
        }

        return Paths.get(v);
    }

    public static Path getAbsolute(Path workDir, Path p) {
        if (p.isAbsolute()) {
            return p;
        }

        return workDir.resolve(p);
    }

    public static List<Path> resolve(Path dir, List<String> paths) {
        if (paths == null) {
            return Collections.emptyList();
        }

        return paths.stream().map(dir::resolve).collect(Collectors.toList());
    }

    public static String getRemoteBackendTfCfgFile(Map<String, Object> backend, Path baseDir) throws Exception {
        Map<String, Object> backendParameters = MapUtils.getMap(backend, BACKEND_REMOTE_KEY, null);
        if (!backendParameters.containsKey(HOSTNAME_KEY) || !backendParameters.containsKey(TOKEN_KEY)) {
            return null;
        }

        String host = MapUtils.assertString(backendParameters, HOSTNAME_KEY);
        String token = MapUtils.assertString(backendParameters, TOKEN_KEY);
        String tfCliConfig = String.format("credentials \"%s\" { \ntoken = \"%s\" \n}", host, token);

        Path tmpDir = assertTempDir(baseDir);
        Path tfCliConfigFile = Files.createTempFile(tmpDir, "terraformrc", ".bin");
        Files.write(tfCliConfigFile, tfCliConfig.getBytes());
        return baseDir.relativize(tfCliConfigFile).toString();
    }

    private static Path assertTempDir(Path baseDir) throws Exception {
        Path p = baseDir.resolve(".tmp");
        if (!Files.exists(p)) {
            Files.createDirectories(p);
        }
        return p;
    }

    private Utils() {
    }
}
