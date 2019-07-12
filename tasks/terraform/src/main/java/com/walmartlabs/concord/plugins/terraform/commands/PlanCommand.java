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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PlanCommand {

    private static final Logger log = LoggerFactory.getLogger(PlanCommand.class);

    private final boolean debug;
    private final boolean destroy;
    private final Path pwd;
    private final Path dirOrPlan;
    private final Path varFile;
    private final Path outFile;
    private final Map<String, String> env;

    public PlanCommand(boolean debug, boolean destroy, Path pwd, Path dirOrPlan, Path varFile, Path outFile,
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

        this.varFile = varFile;
        this.outFile = outFile;
        this.env = env;
    }

    public Result exec(Terraform terraform) throws Exception {
        List<String> args = new ArrayList<>();
        args.add("plan");
        args.add("-input=false");
        args.add("-detailed-exitcode");

        if (destroy) {
            args.add("-destroy");
        }

        if (varFile != null) {
            if (debug) {
                log.info("exec -> using var file: {}", varFile);
            }
            args.add("-var-file=" + varFile.toAbsolutePath().toString());
        }

        if (outFile != null) {
            if (debug) {
                log.info("exec -> using out file: {}", outFile);
            }
            args.add("-out=" + outFile.toAbsolutePath().toString());
        }

        args.add(dirOrPlan.toString());

        return terraform.exec(pwd, "\u001b[32mplan\u001b[0m", false, env, args);
    }
}
