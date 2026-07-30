package com.walmartlabs.concord.plugins.terraform.actions;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2026 Walmart Inc., Concord Authors
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

import com.walmartlabs.concord.plugins.terraform.Terraform;
import com.walmartlabs.concord.plugins.terraform.backend.Backend;

import java.nio.file.Path;
import java.util.Map;

public class InitAction extends Action {

    private final Map<String, String> env;

    public InitAction(Path workDir, Map<String, Object> cfg, Map<String, String> env) {
        super(workDir, cfg);
        this.env = env;
    }

    public TerraformActionResult exec(Terraform terraform, Backend backend) throws Exception {
        try {
            init(env, terraform, backend);
            return TerraformActionResult.ok(null);
        } catch (Exception e) {
            if (!isIgnoreErrors()) {
                throw e;
            }

            return TerraformActionResult.error(e.getMessage());
        } finally {
            cleanup(null, backend);
        }
    }
}
