package com.walmartlabs.concord.plugins.packer.commands;

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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.walmartlabs.concord.plugins.packer.Constants;
import com.walmartlabs.concord.plugins.packer.Packer;

public class BuildCommand {

    private static final Logger log = LoggerFactory.getLogger(BuildCommand.class);

    private final boolean debug;
    private final boolean force;

    private final int parallelBuilds;

    private final Path pwd;
    private final Path varFile;
    private final List<String> except;
    private final List<String> only;
    private final Map<String, String> env;

    public BuildCommand(boolean debug, boolean force, int parallelBuilds, Path pwd, Path varFile,
            List<String> except, List<String> only, Map<String, String> env) {
        this.debug = debug;
        this.force = force;

        this.parallelBuilds = parallelBuilds;

        if (!pwd.isAbsolute()) {
            throw new IllegalArgumentException("'pwd' must be an absolute path, got: " + pwd);
        }
        this.pwd = pwd;
        this.varFile = varFile;
        this.except = except;
        this.only = only;
        this.env = env;
    }

    public Packer.Result exec(Packer packer) throws Exception {
        List<String> args = new ArrayList<>();
        args.add("build");
        args.add("-color=false");

        if (debug) {
            args.add("-debug");
        }

        if (except != null && !except.isEmpty()) {
            args.add("-except=" + String.join(",", except));
        }

        if (force) {
            args.add("-force");
        }

        if (only != null && !only.isEmpty()) {
            args.add("-only=" + String.join(",", only));
        }

        args.add("-parallel-builds=" + parallelBuilds);

        if (varFile != null) {
            if (debug) {
                log.info("exec -> using var file: {}", varFile);
            }
            args.add("-var-file=" + varFile.toAbsolutePath().toString());
        }

        return packer.exec(pwd, Constants.PACKER_LOG_PREFIX, false, env, args);
    }
}
