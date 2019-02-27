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

public class InitCommand {

    private static final Logger log = LoggerFactory.getLogger(InitCommand.class);

    private final Path dir;
    private final Map<String, String> env;

    public InitCommand(Path dir, Map<String, String> env) {
        this.dir = dir;
        this.env = env;
    }

    public Result exec(Terraform terraform) throws Exception {
        List<String> args = new ArrayList<>();
        args.add("init");
        args.add("-input=false");

        log.info("exec -> using directory: {}", dir);
        args.add(dir.toAbsolutePath().toString());

        return terraform.exec("\u001b[36minit\u001b[0m", env, args);
    }
}
