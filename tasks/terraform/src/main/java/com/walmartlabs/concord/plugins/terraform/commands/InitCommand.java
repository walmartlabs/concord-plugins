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
import com.walmartlabs.concord.plugins.terraform.VersionUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class InitCommand {

    private final Path pwd;
    private final Path dir;
    private final Map<String, String> env;
    private final boolean silent;

    public InitCommand(Path pwd, Path dir, Map<String, String> env, boolean silent) {
        this.silent = silent;
        if (!pwd.isAbsolute()) {
            throw new IllegalArgumentException("'pwd' must be an absolute path, got: " + pwd);
        }
        this.pwd = pwd;

        if (!dir.isAbsolute()) {
            throw new IllegalArgumentException("'dir' must be an absolute path, got: " + dir);
        }
        this.dir = dir;

        this.env = env;
    }

    public Result exec(Terraform terraform) throws Exception {
        List<String> args = new ArrayList<>();
        boolean hasChdir = VersionUtils.ge(terraform, 0, 14, 0);
        if (hasChdir) {
            args.add("-chdir=" + dir.toString());
        }
        args.add("init");
        args.add("-input=false");
        if (!hasChdir) {
            args.add(dir.toString());
        }

        return terraform.exec(pwd, "\u001b[36minit\u001b[0m", silent, env, args);
    }
}
