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

    private final boolean destroy;
    private final Path plan;
    private final List<String> userSuppliedVarFileNames;
    private final Map<String, String> env;

    @SuppressWarnings("unchecked")
    public PlanAction(Path workDir, Map<String, Object> cfg, Map<String, String> env) {
        super(workDir, cfg);

        this.env = env;
        this.destroy = MapUtils.get(cfg, TaskConstants.DESTROY_KEY, false, Boolean.class);
        this.userSuppliedVarFileNames = MapUtils.get(cfg, TaskConstants.VARS_FILES, null, List.class);

        // a file, created by the plan action
        this.plan = getPath(cfg, TaskConstants.PLAN_KEY, null);
    }

    public TerraformActionResult exec(Terraform terraform, Backend backend) throws Exception {
        try {
            init(env, terraform, backend);

            // save 'extraVars' into a file that can be automatically picked up by TF
            createVarsFile(getExtraVars());

            Path dirOrPlanAbsolute = getPwd().resolve(plan != null ? plan : getTFDir());
            List<Path> userSuppliedVarFiles = Utils.resolve(getPwd(), userSuppliedVarFileNames);

            Path outFile = null;
            String outFileStr = null;
            if (backend.supportsOutFiles()) {
                outFile = getOutFile(getPwd());
                outFileStr = getPwd().relativize(outFile).toString();
            }

            Terraform.Result r = new PlanCommand(isDebug(), destroy, getPwd(),
                    dirOrPlanAbsolute, userSuppliedVarFiles, outFile, env).exec(terraform);

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
            if (!isIgnoreErrors()) {
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
