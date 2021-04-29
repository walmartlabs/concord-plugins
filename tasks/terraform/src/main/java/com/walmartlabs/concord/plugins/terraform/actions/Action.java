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
import com.walmartlabs.concord.plugins.terraform.TaskConstants;
import com.walmartlabs.concord.plugins.terraform.Terraform;
import com.walmartlabs.concord.plugins.terraform.Utils;
import com.walmartlabs.concord.plugins.terraform.backend.Backend;
import com.walmartlabs.concord.plugins.terraform.commands.InitCommand;
import com.walmartlabs.concord.sdk.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;

import static com.walmartlabs.concord.plugins.terraform.Utils.getAbsolute;
import static com.walmartlabs.concord.plugins.terraform.Utils.getPath;

public abstract class Action {

    private static final Logger log = LoggerFactory.getLogger(Action.class);

    private final Map<String, Object> cfg;
    private final Path workDir;
    private final Path pwd;
    private final Path dir;
    private final boolean verbose;
    private final boolean debug;
    private final boolean saveOutput;
    private final Map<String, Object> extraVars;
    private final boolean ignoreErrors;
    private final ObjectMapper objectMapper;

    /**
     * @param workDir Process' working directory
     * @param cfg Task configuration parameters
     */
    protected Action(Path workDir, Map<String, Object> cfg) {
        this.cfg = cfg;
        this.workDir = workDir; // the process' working directory
        this.pwd = getAbsolute(workDir, getPath(cfg, TaskConstants.PWD_KEY, null));
        this.dir = getPath(cfg, TaskConstants.DIR_KEY, pwd);
        this.verbose = MapUtils.get(cfg, TaskConstants.VERBOSE_KEY, false, Boolean.class);
        this.debug = MapUtils.get(cfg, TaskConstants.DEBUG_KEY, false, Boolean.class);
        this.saveOutput = MapUtils.get(cfg, TaskConstants.SAVE_OUTPUT_KEY, false, Boolean.class);
        this.extraVars = MapUtils.get(cfg, TaskConstants.EXTRA_VARS_KEY, null);
        this.ignoreErrors = MapUtils.get(cfg, TaskConstants.IGNORE_ERRORS_KEY, false, Boolean.class);
        this.objectMapper = new ObjectMapper();
    }

    protected Path getPwd() {
        return pwd;
    }

    protected Path getTFDir() {
        if (dir == null) {
            return pwd;
        }

        return dir;
    }

    public static Logger getLog() {
        return log;
    }

    public Path getWorkDir() {
        return workDir;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public boolean isDebug() {
        return debug;
    }

    public Map<String, Object> getExtraVars() {
        return extraVars;
    }

    public boolean isIgnoreErrors() {
        return ignoreErrors;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    /**
     * Saves the provided map object as a JSON file. The resulting file name uses the TF naming convention for
     * variable files: *.auto.tfvars.json.
     */
    protected void createVarsFile(Map<String, Object> m) throws IOException {
        if (m == null || m.isEmpty()) {
            return;
        }

        Path p = Files.createTempFile(getTFDir(), ".vars", ".auto.tfvars.json");
        log.info("Saving 'extraVars' as {}...", p);
        try (OutputStream out = Files.newOutputStream(p, StandardOpenOption.TRUNCATE_EXISTING)) {
            objectMapper.writeValue(out, m);
        }
    }

    protected void init(Map<String, String> env, Terraform terraform, Backend backend) throws Exception {
        log.info("init -> initializing the backend and running `terraform init`...");

        Path absTFDir = Utils.getAbsolute(pwd, dir);
        backend.init(absTFDir);

        Terraform.Result r = new InitCommand(pwd, absTFDir, env, !verbose).exec(terraform);
        if (r.getCode() != 0) {
            throw new RuntimeException("Initialization finished with code " + r.getCode() + ": " + r.getStderr());
        }
    }

    protected TerraformActionResult handleBasicCommandResult(Terraform.Result r,
                                                             Map<String, String> env,
                                                             Terraform terraform,
                                                             Backend backend) throws Exception {
        if (r.getCode() != 0) {
            throw new RuntimeException("Process finished with code " + r.getCode() + ": " + r.getStderr());
        }

        Map<String, Object> data = null;
        if (saveOutput) {
            TerraformActionResult o = new OutputAction(getWorkDir(), cfg, env, true).exec(terraform, backend);
            if (!o.isOk()) {
                return TerraformActionResult.error(o.getError());
            }

            data = o.getData();
        }

        return TerraformActionResult.ok(data, r.getStdout());
    }
}
