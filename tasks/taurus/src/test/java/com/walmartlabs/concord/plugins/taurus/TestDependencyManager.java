package com.walmartlabs.concord.plugins.taurus;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2022 Walmart Inc., Concord Authors
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

import com.walmartlabs.concord.dependencymanager.DependencyEntity;
import com.walmartlabs.concord.dependencymanager.DependencyManager;
import com.walmartlabs.concord.dependencymanager.DependencyManagerConfiguration;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

public class TestDependencyManager {

    private final DependencyManager dm;

    public TestDependencyManager(String tool) throws IOException {
        Path toolDir = Paths.get(System.getProperty("user.home"), ".m2/tools/", tool);
        assertToolDir(toolDir);

        this.dm = new DependencyManager(DependencyManagerConfiguration.of(toolDir));
    }

    private static void assertToolDir(Path toolDir) {
        if (Files.exists(toolDir)) {
            return;
        }

        Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rwxr-x---");

        if (Files.exists(toolDir)) {
            return;
        }

        try {
            if (!Files.exists(Files.createDirectories(toolDir, PosixFilePermissions.asFileAttribute(perms)))) {
                throw new Exception("Failed to crate tool cache directory: " + toolDir);
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to create cache directory for terraform executable.");
        }
    }

    public Path resolve(URI dependency) throws IOException {

        DependencyEntity de = dm.resolveSingle(dependency);

        return de.getPath();
    }
}
