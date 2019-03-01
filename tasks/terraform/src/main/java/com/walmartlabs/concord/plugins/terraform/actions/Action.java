package com.walmartlabs.concord.plugins.terraform.actions;

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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.walmartlabs.concord.common.IOUtils;
import com.walmartlabs.concord.plugins.terraform.Terraform;
import com.walmartlabs.concord.plugins.terraform.backend.Backend;
import com.walmartlabs.concord.plugins.terraform.commands.InitCommand;
import com.walmartlabs.concord.sdk.Context;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;

public abstract class Action {

    protected static Path getAbsolute(Path workDir, Path p) {
        if (p.isAbsolute()) {
            return p;
        }

        return workDir.resolve(p);
    }

    protected static Path createVarFile(ObjectMapper objectMapper, Map<String, Object> m) throws IOException {
        if (m == null || m.isEmpty()) {
            return null;
        }

        Path p = IOUtils.createTempFile("vars", ".json");
        try (OutputStream out = Files.newOutputStream(p, StandardOpenOption.TRUNCATE_EXISTING)) {
            objectMapper.writeValue(out, m);
        }

        return p;
    }

    protected static Path init(Context ctx, Path workDir, Path dirOrPlan, Map<String, String> env, Terraform terraform, Backend backend) throws Exception {
        backend.init(ctx, workDir);

        Path dirOrPlanAbsolute = getAbsolute(workDir, dirOrPlan);

        Path initDir = workDir;
        if (Files.isDirectory(dirOrPlanAbsolute)) {
            initDir = dirOrPlanAbsolute;
        }
        new InitCommand(initDir, env).exec(terraform);

        return dirOrPlanAbsolute;
    }
}
