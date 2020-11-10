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
import com.walmartlabs.concord.plugins.terraform.commands.ApplyCommand;
import com.walmartlabs.concord.sdk.MapUtils;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static com.walmartlabs.concord.plugins.terraform.Utils.getPath;

public class ApplyAction extends Action {

    private final Map<String, Object> cfg;
    private final boolean verbose;
    private final Path workDir;
    private final Path dir;
    private final Path plan;
    private final Map<String, Object> extraVars;
    private final List<String> userSuppliedVarFileNames;
    private final Map<String, String> env;
    private final boolean ignoreErrors;
    private final boolean saveOutput;
    private final ObjectMapper objectMapper;

    @SuppressWarnings("unchecked")
    public ApplyAction(Map<String, Object> cfg, Map<String, String> env) {
        this.cfg = cfg;
        this.env = env;

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
        this.saveOutput = MapUtils.get(cfg, TaskConstants.SAVE_OUTPUT_KEY, false, Boolean.class);
        this.objectMapper = new ObjectMapper();
    }

    public TerraformActionResult exec(Terraform terraform, Backend backend) throws Exception {
        try {
            init(workDir, dir, !verbose, env, terraform, backend);
            if (plan == null) {
                // running without a previously created plan file
                // save 'extraVars' into a file that can be automatically picked up by TF
                createVarsFile(workDir, objectMapper, extraVars);
            }

            Path dirOrPlanAbsolute = workDir.resolve(plan != null ? plan : dir);
            List<Path> userSuppliedVarFiles = Utils.resolve(workDir, userSuppliedVarFileNames);

            Terraform.Result r = new ApplyCommand(workDir, dirOrPlanAbsolute, userSuppliedVarFiles, env).exec(terraform);
            if (r.getCode() != 0) {
                throw new RuntimeException("Process finished with code " + r.getCode() + ": " + r.getStderr());
            }

            Map<String, Object> data = null;
            if (saveOutput) {
                TerraformActionResult o = new OutputAction(cfg, env, true).exec(terraform, backend);
                if (!o.isOk()) {
                    return TerraformActionResult.error(o.getError());
                }

                data = o.getData();
            }

            return TerraformActionResult.ok(data, r.getStdout());
        } catch (Exception e) {
            if (!ignoreErrors) {
                throw e;
            }

            return TerraformActionResult.error(e.getMessage());
        }
    }
}
