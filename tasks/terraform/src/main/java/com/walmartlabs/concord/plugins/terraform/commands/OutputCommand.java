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
import java.util.Map;

public class OutputCommand {

    private static final Logger log = LoggerFactory.getLogger(OutputCommand.class);

    private final boolean debug;
    private final Path pwd;
    private final String module;
    private final Map<String, String> env;

    public OutputCommand(boolean debug, Path pwd, String module, Map<String, String> env) {
        this.debug = debug;

        if (!pwd.isAbsolute()) {
            throw new IllegalArgumentException("'pwd' must be an absolute path, got: " + pwd);
        }
        this.pwd = pwd;

        this.module = module;
        this.env = env;
    }

    public Result exec(Terraform terraform) throws Exception {
        TerraformArgs args = terraform.buildArgs(Terraform.CliAction.OUTPUT);

        args.add("-json");

        if (module != null) {
            if (debug) {
                log.info("exec -> using module: {}", module);
            }
            args.add("-module", module);
        }

        return terraform.exec(pwd, "\u001b[33moutput\u001b[0m", false, env, args);
    }
}
