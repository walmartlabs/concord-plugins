package com.walmartlabs.concord.plugins.terraform;

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

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.util.VersionUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Terraform {

    private static final Logger log = LoggerFactory.getLogger(Terraform.class);

    private final boolean debug;
    private final Path processWorkDir;
    private final TerraformExecutable binary;
    private final Map<String, String> baseEnv;
    private final ExecutorService executor;
    private final Version version;
    private final String dockerImage;
    private final TerraformDockerService dockerService;

    public enum CliAction {
        APPLY,
        DESTROY,
        INIT,
        OUTPUT,
        PLAN,
        VERSION
    }

    /**
     * @param debug   enable/disable additional debug output
     */
    public Terraform(Path processWorkDir,
                     TerraformBinaryResolver binaryResolver,
                     boolean debug,
                     Map<String, String> baseEnv,
                     String dockerImage,
                     TerraformDockerService dockerService) throws Exception {
        this.debug = debug;
        this.processWorkDir = processWorkDir;
        this.baseEnv = baseEnv;
        this.binary = binaryResolver.resolve();
        this.executor = Executors.newCachedThreadPool();
        this.dockerImage = dockerImage;
        this.dockerService = dockerService;
        this.version = getBinaryVersion();

        if (dockerImage != null) {
            log.info("Executing terraform commands in container using image: {}", dockerImage);
        }
    }

    public Version version() {
        return version;
    }

    public Result exec(Path pwd, String logPrefix, TerraformArgs args) throws Exception {
        return exec(pwd, logPrefix, false, Collections.emptyMap(), args);
    }

    public Result exec(Path pwd, String logPrefix, boolean silent, Map<String, String> env, TerraformArgs args) throws Exception {
        if (dockerImage == null) {
            return execLocal(pwd, logPrefix, silent, env, args);
        }

        return execDocker(pwd, logPrefix, silent, env, args);
    }

    public Result execLocal(Path pwd, String logPrefix, boolean silent, Map<String, String> env, TerraformArgs args) throws Exception {
        List<String> cmd = new ArrayList<>();
        cmd.add(binary.getExecutablePath().toAbsolutePath().toString());
        cmd.addAll(args.get());

        if (debug) {
            log.info("exec -> {} in {}", String.join(" ", cmd), pwd);
        }

        ProcessBuilder pb = new ProcessBuilder(cmd)
                .directory(pwd.toFile());

        Map<String, String> combinedEnv = new HashMap<>(baseEnv);
        combinedEnv.putAll(env);

        if (debug) {
            log.info("exec -> using env: {}", combinedEnv);
        }

        pb.environment().putAll(combinedEnv);

        Process p = pb.start();

        Future<String> stderr = executor.submit(new StreamReader(logPrefix, false, p.getErrorStream()));
        Future<String> stdout = executor.submit(new StreamReader(logPrefix, silent, p.getInputStream()));

        int code = p.waitFor();
        return new Result(code, stdout.get(), stderr.get());
    }

    public Result execDocker(Path pwd, String logPrefix, boolean silent, Map<String, String> env, TerraformArgs args) throws Exception {
        Path containerBinary = binary.isSourceInContainer()
                ? binary.getExecutablePath()
                : toContainerPath(binary.getExecutablePath().toAbsolutePath());
        Path containerPwd = toContainerPath(pwd);

        List<String> cmd = new ArrayList<>();
        cmd.add(containerBinary.toString());
        cmd.addAll(args.get());

        if (debug) {
            log.info("exec -> {} in {}{}", String.join(" ", cmd), dockerImage, pwd);
        }

        Map<String, String> combinedEnv = new HashMap<>(baseEnv);
        combinedEnv.putAll(env);
        // default plugin cache in /tmp may not be writable. Keep it in the process' workDir
        if (null == combinedEnv.putIfAbsent("TF_PLUGIN_CACHE_DIR", "/workspace/.terraform/plugin-cache")) {
            Files.createDirectories(processWorkDir.resolve(".terraform/plugin-cache"));
        }

        if (debug) {
            log.info("exec -> using env: {}", combinedEnv);
        }

        TerraformDockerService.DockerContainerSpec spec = new TerraformDockerService.DockerContainerSpec()
                .image(dockerImage)
                .args(cmd)
                .debug(debug)
                .pwd(containerPwd)
                .forcePull(false) // TODO add a param to enable? if yes, then be careful to only force pull once
                .env(combinedEnv)
                .pullRetryCount(3)
                .pullRetryInterval(10);
        DockerLogCallback outLog = new DockerLogCallback(logPrefix, silent);
        DockerLogCallback errLog = new DockerLogCallback(logPrefix, silent);

        int code = dockerService.start(spec, outLog, errLog);

        return new Result(code, outLog.fullLog(), errLog.fullLog());
    }

    public TerraformArgs buildArgs(CliAction action) {
        return new TerraformArgsImpl(action, null);
    }

    public TerraformArgs buildArgs(CliAction action, Path targetDir) {
        return new TerraformArgsImpl(action, targetDir);
    }

    private class TerraformArgsImpl implements TerraformArgs {
        private final List<String> args;
        private final boolean hasChdir;

        public TerraformArgsImpl(CliAction action, Path targetDir) {
            this.args = new LinkedList<>();

            hasChdir = targetDir != null && VersionUtils.ge(Terraform.this, 0, 14, 0);
            if (hasChdir) {
                args.add(String.format("%s=%s", "-chdir", (dockerImage == null)
                        ? targetDir.toAbsolutePath().toString()
                        : toContainerPath(targetDir.toAbsolutePath()).toString()));
            }

            args.add(action.toString().toLowerCase());
        }

        public boolean hasChdir() {
            return hasChdir;
        }

        public TerraformArgs add(String opt) {
            args.add(opt);
            return this;
        }

        public TerraformArgs add(String opt, String value) {
            args.add(String.format("%s=%s", opt, value));
            return this;
        }

        public TerraformArgs add(Path path) {
            add((dockerImage == null) ? path.toAbsolutePath().toString() :
                    toContainerPath(path.toAbsolutePath()).toString());
            return this;
        }

        public TerraformArgs add(String opt, Path path) {
            return add(opt, (dockerImage == null)
                    ? path.toAbsolutePath().toString()
                    : toContainerPath(path.toAbsolutePath()).toString());
        }

        public List<String> get() {
            return args;
        }
    }

    public Path toContainerPath(Path p) {
        return Paths.get("/workspace").resolve(processWorkDir.relativize(p));
    }

    private Version getBinaryVersion() throws Exception {
        TerraformArgs args = buildArgs(CliAction.VERSION).add("-json");

        Result result = exec(Paths.get("/"), "version", !debug, Collections.emptyMap(), args);
        if (result.getCode() != 0) {
            throw new RuntimeException("Can't get terraform version. Process finished with code " + result.getCode() + ": " + result.getStdout() + result.getStderr());
        }

        String rawVersion;
        if (result.stdout.startsWith("{")) {
            rawVersion = getVersionFromJson(result.stdout);
        } else {
            rawVersion = getVersionFromString(result.stdout);
        }

        if (!debug) {
            log.info("Using terraform version '{}'", rawVersion);
        }
        Version parsedVersion = VersionUtil.parseVersion(rawVersion, null, null);
        if (parsedVersion == Version.unknownVersion()) {
            throw new RuntimeException("Can't parse version: '" + rawVersion + "'");
        }
        return parsedVersion;
    }

    private static String getVersionFromJson(String stdout) throws Exception {
        JsonNode versionObj = new ObjectMapper().readTree(stdout);
        JsonNode terraformVersion = versionObj.get("terraform_version");
        if (terraformVersion == null) {
            throw new RuntimeException("Can't get terraform version. Can't find 'terraform_version' field in result: " + stdout);
        }

        return terraformVersion.asText();
    }

    private static String getVersionFromString(String stdout) {
        int i = 0;
        while (i < stdout.length() && !Character.isDigit(stdout.charAt(i))) {
            i++;
        }
        return stdout.substring(i).trim();
    }

    private static void log(String prefix, String s) {
        System.out.print("\u001b[34mterraform\u001b[0m " + prefix + ": ");
        System.out.print(s);
        System.out.println();
    }

    private static String removeAnsiColors(String s) {
        return s.replaceAll("\u001B\\[[;\\d]*m", "");
    }

    static class DockerLogCallback {
        final StringBuilder sb;
        final String logPrefix;
        final boolean silent;

        DockerLogCallback(String logPrefix, boolean silent) {
            this.logPrefix = logPrefix;
            this.silent = silent;
            sb = new StringBuilder();
        }

        public void onLog(String line) {
            if (!silent) {
                log(logPrefix, line);
            }

            sb.append(removeAnsiColors(line))
                    .append(System.lineSeparator());
        }

        public String fullLog() {
            return sb.toString();
        }
    }

    static class StreamReader implements Callable<String> {

        private final String logPrefix;
        private final boolean silent;
        private final InputStream in;

        private StreamReader(String logPrefix, boolean silent, InputStream in) {
            this.logPrefix = logPrefix;
            this.silent = silent;
            this.in = in;
        }

        @Override
        public String call() throws Exception {
            StringBuilder sb = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    if (!silent) {
                        log(logPrefix, line);
                    }

                    sb.append(removeAnsiColors(line))
                            .append(System.lineSeparator());
                }
            }

            return sb.toString();
        }
    }

    public static class Result {

        private final int code;
        private final String stdout;
        private final String stderr;

        public Result(int code, String stdout, String stderr) {
            this.code = code;
            this.stdout = stdout;
            this.stderr = stderr;
        }

        public int getCode() {
            return code;
        }

        public String getStdout() {
            return stdout;
        }

        public String getStderr() {
            return stderr;
        }
    }
}
