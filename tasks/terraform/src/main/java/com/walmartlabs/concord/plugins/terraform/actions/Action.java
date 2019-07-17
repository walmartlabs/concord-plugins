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
import com.walmartlabs.concord.plugins.terraform.Terraform;
import com.walmartlabs.concord.plugins.terraform.Utils;
import com.walmartlabs.concord.plugins.terraform.backend.Backend;
import com.walmartlabs.concord.plugins.terraform.commands.InitCommand;
import com.walmartlabs.concord.sdk.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;

public abstract class Action {

    private static final Logger log = LoggerFactory.getLogger(Action.class);

    protected static Path createVarsFile(Path dir, ObjectMapper objectMapper, Map<String, Object> m) throws IOException {
        if (m == null || m.isEmpty()) {
            return null;
        }

        Path p = Files.createTempFile(dir, ".vars", ".json");
        try (OutputStream out = Files.newOutputStream(p, StandardOpenOption.TRUNCATE_EXISTING)) {
            objectMapper.writeValue(out, m);
        }

        return p;
    }

    protected static void init(Context ctx, Path workDir, Path dir, boolean silent, Map<String, String> env, Terraform terraform, Backend backend) throws Exception {
        log.info("init -> initializing the backend and running `terraform init`...");

        Path tfDir = Utils.getAbsolute(workDir, dir);
        backend.init(ctx, tfDir);

        Terraform.Result r = new InitCommand(workDir, tfDir, env, silent).exec(terraform);
        if (r.getCode() != 0) {
            throw new RuntimeException("Initialization finished with code " + r.getCode() + ": " + r.getStderr());
        }
    }
}
