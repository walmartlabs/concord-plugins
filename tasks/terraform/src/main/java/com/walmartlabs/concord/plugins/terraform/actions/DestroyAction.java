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
import com.walmartlabs.concord.plugins.terraform.commands.DestroyCommand;
import com.walmartlabs.concord.sdk.MapUtils;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class DestroyAction extends Action {

    private final String target;
    private final List<String> userSuppliedVarFileNames;
    private final Map<String, String> env;

    @SuppressWarnings("unchecked")
    public DestroyAction(Path workDir, Map<String, Object> cfg, Map<String, String> env) {
        super(workDir, cfg);
        this.env = env;
        this.target = MapUtils.getString(cfg, TaskConstants.TARGET_KEY);
        this.userSuppliedVarFileNames = MapUtils.get(cfg, TaskConstants.VARS_FILES, null, List.class);
    }

    public TerraformActionResult exec(Terraform terraform, Backend backend) throws Exception {
        try {
            init(env, terraform, backend);

            createVarsFile(getExtraVars());

            Path dirAbsolute = getPwd().resolve(getTFDir());
            List<Path> userSuppliedVarFiles = Utils.resolve(getPwd(), userSuppliedVarFileNames);

            Terraform.Result r = new DestroyCommand(getPwd(), dirAbsolute, target, userSuppliedVarFiles, env)
                    .exec(terraform);

            return handleBasicCommandResult(r, env, terraform, backend);
        } catch (Exception e) {
            if (!isIgnoreErrors()) {
                throw e;
            }

            return TerraformActionResult.error(e.getMessage());
        }
    }
}
