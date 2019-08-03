package com.walmartlabs.concord.plugins.packer.actions;

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

import static com.walmartlabs.concord.plugins.packer.Utils.getPath;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.walmartlabs.concord.plugins.packer.Constants;
import com.walmartlabs.concord.plugins.packer.Packer;
import com.walmartlabs.concord.plugins.packer.commands.BuildCommand;
import com.walmartlabs.concord.sdk.MapUtils;

public class BuildAction extends Action {

    private final boolean debug;
    private final boolean force;
    private final int parallelBuilds;
    private final Path workDir;
    private final List<String> except;
    private final List<String> only;
    private final Map<String, Object> extraVars;
    private final Map<String, String> env;
    private final boolean ignoreErrors;
    private final ObjectMapper objectMapper;

    @SuppressWarnings("unchecked")
    public BuildAction(Map<String, Object> cfg, Map<String, String> env) {
        this.env = env;

        this.debug = MapUtils.get(cfg, Constants.DEBUG_KEY, false, Boolean.class);
        this.force = MapUtils.get(cfg, Constants.FORCE_KEY, false, Boolean.class);

        this.parallelBuilds = MapUtils.get(cfg, Constants.PARALLEL_BUILDS_KEY, 0, Integer.class);

        this.workDir = getPath(cfg, com.walmartlabs.concord.sdk.Constants.Context.WORK_DIR_KEY, null);
        if (!workDir.isAbsolute()) {
            throw new IllegalArgumentException("'workDir' must be an absolute path, got: " + workDir);
        }

        this.except = MapUtils.get(cfg, Constants.EXCEPT_KEY, null, List.class);
        this.only = MapUtils.get(cfg, Constants.ONLY_KEY, null, List.class);

        this.extraVars = MapUtils.get(cfg, Constants.EXTRA_VARS_KEY, null, Map.class);
        this.ignoreErrors = MapUtils.get(cfg, Constants.IGNORE_ERRORS_KEY, false, Boolean.class);
        this.objectMapper = new ObjectMapper();
    }

    public BuildResult exec(Packer packer) throws Exception {
        try {
            Path varsFile = createVarsFile(workDir, objectMapper, extraVars);

            Packer.Result r = new BuildCommand(debug, force, parallelBuilds, workDir, varsFile, except, only, env)
                    .exec(packer);
            if (r.getCode() != 0) {
                throw new RuntimeException("Process finished with code " + r.getCode() + ": " + r.getStderr());
            }

            return BuildResult.ok(r.getStdout(), Collections.emptyMap());
        } catch (Exception e) {
            if (!ignoreErrors) {
                throw e;
            }

            return BuildResult.error(e.getMessage());
        }
    }
}
