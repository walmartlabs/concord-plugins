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
import com.walmartlabs.concord.plugins.terraform.commands.OutputCommand;
import com.walmartlabs.concord.sdk.Context;
import com.walmartlabs.concord.sdk.MapUtils;

import java.nio.file.Path;
import java.util.Map;

import static com.walmartlabs.concord.plugins.terraform.Utils.getPath;

public class OutputAction extends Action {

    private final Context ctx;
    private final boolean debug;
    private final boolean verbose;
    private final Path workDir;
    private final Path dir;
    private final String module;
    private final Map<String, String> env;
    private final boolean ignoreErrors;
    private final boolean skipInit;

    private final ObjectMapper objectMapper;

    public OutputAction(Context ctx, Map<String, Object> cfg, Map<String, String> env, boolean skipInit) {
        this.ctx = ctx;
        this.env = env;

        this.debug = MapUtils.get(cfg, Constants.DEBUG_KEY, false, Boolean.class);
        this.verbose = MapUtils.get(cfg, Constants.VERBOSE_KEY, false, Boolean.class);

        this.workDir = getPath(cfg, com.walmartlabs.concord.sdk.Constants.Context.WORK_DIR_KEY, null);
        if (!workDir.isAbsolute()) {
            throw new IllegalArgumentException("'workDir' must be an absolute path, got: " + workDir);
        }

        this.dir = getPath(cfg, Constants.DIR_KEY, workDir);
        this.module = MapUtils.getString(cfg, Constants.MODULE_KEY);
        this.ignoreErrors = MapUtils.get(cfg, Constants.IGNORE_ERRORS_KEY, false, Boolean.class);
        this.skipInit = skipInit;

        this.objectMapper = new ObjectMapper();
    }

    @SuppressWarnings("unchecked")
    public CommonResult exec(Terraform terraform, Backend backend) throws Exception {
        try {
            if (!skipInit) {
                // normally we'd run `terraform init` in the specified `dir`
                // the backend configuration must be placed there as well
                init(ctx, workDir, dir, !verbose, env, terraform, backend);
            } else {
                // however, if we're running `terraform output` as a part of the apply action
                // we skip the `terraform init` run and we need to run `terraform output`
                // in the root directory
                // the backend configuration must be in the root directory too
                backend.init(ctx, workDir);
            }

            Terraform.Result r = new OutputCommand(debug, workDir, module, env).exec(terraform);
            if (r.getCode() != 0) {
                throw new RuntimeException("Process finished with code " + r.getCode() + ": " + r.getStderr());
            }

            Map<String, Object> data = objectMapper.readValue(r.getStdout(), Map.class);
            return CommonResult.ok(data);
        } catch (Exception e) {
            if (!ignoreErrors) {
                throw e;
            }

            return CommonResult.error(e.getMessage());
        }
    }
}
