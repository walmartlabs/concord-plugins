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
import com.walmartlabs.concord.plugins.terraform.TaskConstants;
import com.walmartlabs.concord.plugins.terraform.Terraform;
import com.walmartlabs.concord.plugins.terraform.Utils;
import com.walmartlabs.concord.plugins.terraform.backend.Backend;
import com.walmartlabs.concord.plugins.terraform.commands.PlanCommand;
import com.walmartlabs.concord.sdk.MapUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.walmartlabs.concord.plugins.terraform.Utils.getPath;

public class PlanAction extends Action {

    private final boolean debug;
    private final boolean destroy;
    private final boolean verbose;
    private final Path workDir;
    private final Path dir;
    private final Path plan;
    private final Map<String, Object> extraVars;
    private final List<String> userSuppliedVarFileNames;
    private final Map<String, String> env;
    private final boolean ignoreErrors;
    private final ObjectMapper objectMapper;

    @SuppressWarnings("unchecked")
    public PlanAction(Map<String, Object> cfg, Map<String, String> env) {
        this.env = env;

        this.debug = MapUtils.get(cfg, TaskConstants.DEBUG_KEY, false, Boolean.class);
        this.destroy = MapUtils.get(cfg, TaskConstants.DESTROY_KEY, false, Boolean.class);
        this.verbose = MapUtils.get(cfg, TaskConstants.VERBOSE_KEY, false, Boolean.class);

        // the process' working directory (aka the payload directory)
        this.workDir = getPath(cfg, com.walmartlabs.concord.sdk.Constants.Context.WORK_DIR_KEY, null);
        if (!workDir.isAbsolute()) {
            throw new IllegalArgumentException("'workDir' must be an absolute path, got: " + workDir);
        }

        // the TF files directory
        this.dir = getPath(cfg, TaskConstants.DIR_KEY, workDir);

        // a file, created by the plan action
        this.plan = getPath(cfg, TaskConstants.PLAN_KEY, null);

        this.extraVars = MapUtils.get(cfg, TaskConstants.EXTRA_VARS_KEY, null, Map.class);
        this.userSuppliedVarFileNames = MapUtils.get(cfg, TaskConstants.VARS_FILES, null, List.class);
        this.ignoreErrors = MapUtils.get(cfg, TaskConstants.IGNORE_ERRORS_KEY, false, Boolean.class);
        this.objectMapper = new ObjectMapper();
    }

    public TerraformActionResult exec(Terraform terraform, Backend backend) throws Exception {
        try {
            init(workDir, dir, !verbose, env, terraform, backend);

            // save 'extraVars' into a file that can be automatically picked up by TF
            createVarsFile(workDir, objectMapper, extraVars);

            Path dirOrPlanAbsolute = workDir.resolve(plan != null ? plan : dir);
            List<Path> userSuppliedVarFiles = Utils.resolve(workDir, userSuppliedVarFileNames);

            Path outFile = null;
            String outFileStr = null;
            if (backend.supportsOutFiles()) {
                outFile = getOutFile(workDir);
                outFileStr = workDir.relativize(outFile).toString();
            }

            Terraform.Result r = new PlanCommand(debug, destroy, workDir, dirOrPlanAbsolute, userSuppliedVarFiles, outFile, env).exec(terraform);
            switch (r.getCode()) {
                case 0: {
                    return TerraformActionResult.noChanges(r.getStdout(), outFileStr);
                }
                case 1: {
                    throw new RuntimeException("Process finished with code " + r.getCode() + ": " + r.getStderr());
                }
                case 2: {
                    return TerraformActionResult.hasChanges(r.getStdout(), outFileStr);
                }
                default:
                    throw new IllegalStateException("Unsupported Terraform exit code: " + r.getCode());
            }
        } catch (Exception e) {
            if (!ignoreErrors) {
                throw e;
            }

            return TerraformActionResult.error(e.getMessage());
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
