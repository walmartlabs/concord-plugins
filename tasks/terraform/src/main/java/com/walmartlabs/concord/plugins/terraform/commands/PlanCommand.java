package com.walmartlabs.concord.plugins.terraform.commands;

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

import com.walmartlabs.concord.plugins.terraform.Terraform;
import com.walmartlabs.concord.plugins.terraform.Terraform.Result;
import com.walmartlabs.concord.plugins.terraform.TerraformArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class PlanCommand {

    private static final Logger log = LoggerFactory.getLogger(PlanCommand.class);

    private final boolean debug;
    private final boolean destroy;
    private final Path pwd;
    private final Path dirOrPlan;
    private final List<Path> userSuppliedVarFiles;
    private final Path outFile;
    private final Map<String, String> env;

    public PlanCommand(boolean debug, boolean destroy, Path pwd, Path dirOrPlan, List<Path> userSuppliedVarFiles, Path outFile,
                       Map<String, String> env) {

        this.debug = debug;
        this.destroy = destroy;

        this.pwd = pwd;
        if (!pwd.isAbsolute()) {
            throw new IllegalArgumentException("'pwd' must be an absolute path, got: " + pwd);
        }

        if (!dirOrPlan.isAbsolute()) {
            throw new IllegalArgumentException("'dirOrPlan' must be an absolute path, got: " + dirOrPlan);
        }
        this.dirOrPlan = dirOrPlan;

        this.userSuppliedVarFiles = userSuppliedVarFiles;
        this.outFile = outFile;
        this.env = env;
    }

    public Result exec(Terraform terraform) throws Exception {
        TerraformArgs args = terraform.buildArgs(Terraform.CLI_ACTION.PLAN, dirOrPlan);

        args.add("-input", "false");
        args.add("-detailed-exitcode");

        if (destroy) {
            args.add("-destroy");
        }

        userSuppliedVarFiles.forEach(f -> args.add("-var-file", f));

        if (outFile != null) {
            if (debug) {
                log.info("exec -> using out file: {}", outFile);
            }
            args.add("-out", outFile);
        }

        if (!args.hasChdir()) {
            args.add(dirOrPlan);
        }

        return terraform.exec(pwd, "\u001b[32mplan\u001b[0m", false, env, args);
    }
}
