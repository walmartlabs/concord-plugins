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

import com.fasterxml.jackson.core.type.TypeReference;
import com.walmartlabs.concord.plugins.terraform.TaskConstants;
import com.walmartlabs.concord.plugins.terraform.Terraform;
import com.walmartlabs.concord.plugins.terraform.backend.Backend;
import com.walmartlabs.concord.plugins.terraform.commands.OutputCommand;
import com.walmartlabs.concord.sdk.MapUtils;

import java.nio.file.Path;
import java.util.Map;

public class OutputAction extends Action {

    private final String module;
    private final Map<String, String> env;
    private final boolean skipInit;

    public OutputAction(Path workDir, Map<String, Object> cfg, Map<String, String> env, boolean skipInit) {
        super(workDir, cfg);

        this.env = env;
        this.module = MapUtils.getString(cfg, TaskConstants.MODULE_KEY);
        this.skipInit = skipInit;
    }

    public TerraformActionResult exec(Terraform terraform, Backend backend) throws Exception {
        try {
            if (!skipInit) {
                // normally we'd run `terraform init` in the specified `dir`
                // the backend configuration must be placed there as well
                init(env, terraform, backend);
            } else {
                // however, if we're running `terraform output` as a part of the apply action
                // we skip the `terraform init` run and we need to run `terraform output`
                // in the root directory
                // the backend configuration must be in the root directory too
                backend.init(getPwd());
            }

            Terraform.Result r = new OutputCommand(isDebug(), getPwd(), module, env).exec(terraform);
            if (r.getCode() != 0) {
                throw new RuntimeException("Process finished with code " + r.getCode() + ": " + r.getStderr());
            }

            TypeReference<Map<String, Object>> mapType = new TypeReference<Map<String, Object>>() {};

            Map<String, Object> data = getObjectMapper().readValue(r.getStdout(), mapType);
            return TerraformActionResult.ok(data);
        } catch (Exception e) {
            if (!isIgnoreErrors()) {
                throw e;
            }

            return TerraformActionResult.error(e.getMessage());
        }
    }
}
