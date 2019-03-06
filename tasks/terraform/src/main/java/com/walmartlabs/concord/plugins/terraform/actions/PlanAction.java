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
import com.walmartlabs.concord.plugins.terraform.Constants;
import com.walmartlabs.concord.plugins.terraform.Terraform;
import com.walmartlabs.concord.plugins.terraform.backend.Backend;
import com.walmartlabs.concord.plugins.terraform.commands.PlanCommand;
import com.walmartlabs.concord.sdk.Context;
import com.walmartlabs.concord.sdk.MapUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

import static com.walmartlabs.concord.plugins.terraform.Utils.getPath;

public class PlanAction extends Action {

    private final Context ctx;
    private final boolean debug;
    private final Path workDir;
    private final Path dirOrPlan;
    private final Map<String, Object> extraVars;
    private final Map<String, String> env;
    private final boolean ignoreErrors;
    private final ObjectMapper objectMapper;

    @SuppressWarnings("unchecked")
    public PlanAction(Context ctx, Map<String, Object> cfg, Map<String, String> env) {
        this.ctx = ctx;
        this.env = env;

        this.debug = MapUtils.get(cfg, Constants.DEBUG_KEY, false, Boolean.class);

        this.workDir = getPath(cfg, com.walmartlabs.concord.sdk.Constants.Context.WORK_DIR_KEY, null);
        if (!workDir.isAbsolute()) {
            throw new IllegalArgumentException("'workDir' must be an absolute path, got: " + workDir);
        }

        this.dirOrPlan = getPath(cfg, Constants.DIR_OR_PLAN_KEY, workDir);

        this.extraVars = MapUtils.get(cfg, Constants.EXTRA_VARS_KEY, null, Map.class);
        this.ignoreErrors = MapUtils.get(cfg, Constants.IGNORE_ERRORS_KEY, false, Boolean.class);
        this.objectMapper = new ObjectMapper();
    }

    public PlanResult exec(Terraform terraform, Backend backend) throws Exception {
        try {
            init(ctx, workDir, dirOrPlan, env, terraform, backend);

            Path varsFile = createVarsFile(workDir, objectMapper, extraVars);
            Path outFile = getOutFile(workDir);

            Terraform.Result r = new PlanCommand(debug, workDir, workDir.resolve(dirOrPlan), varsFile, outFile, env).exec(terraform);
            switch (r.getCode()) {
                case 0: {
                    return PlanResult.noChanges(r.getStdout(), workDir.relativize(outFile).toString());
                }
                case 1: {
                    throw new RuntimeException("Process finished with code " + r.getCode() + ": " + r.getStderr());
                }
                case 2: {
                    return PlanResult.hasChanges(r.getStdout(), workDir.relativize(outFile).toString());
                }
                default:
                    throw new IllegalStateException("Unsupported Terraform exit code: " + r.getCode());
            }
        } catch (Exception e) {
            if (!ignoreErrors) {
                throw e;
            }

            return PlanResult.error(e.getMessage());
        }
    }

    private static Path getOutFile(Path workDir) throws IOException {
        // store the plan files as process attachments
        // otherwise they will be lost if the process suspends
        // TODO store in a regular directory when the process workDir persistence is implemented
        Path attachmentsDir = workDir.resolve(com.walmartlabs.concord.sdk.Constants.Files.JOB_ATTACHMENTS_DIR_NAME);

        Path dst = attachmentsDir.resolve("terraform");
        if (!Files.exists(dst)) {
            Files.createDirectories(dst);
        }

        return dst.resolve("tf" + UUID.randomUUID() + ".plan");
    }
}
