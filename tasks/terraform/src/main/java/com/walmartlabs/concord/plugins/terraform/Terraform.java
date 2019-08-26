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

import com.walmartlabs.concord.common.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Terraform {

    private static final Logger log = LoggerFactory.getLogger(Terraform.class);

    private final boolean debug;
    private final Path binary;
    private final Map<String, String> baseEnv;
    private final ExecutorService executor;
    private final Path toolArchive;

    /**
     * @param workDir the process' working directory. Used to store temporary files
     * @param debug   enable/disable additional debug output
     * @param baseEnv
     * @throws Exception
     */
    public Terraform(Path workDir, boolean debug, Map<String, String> baseEnv, Path toolArchive) throws Exception {
        this.debug = debug;
        this.baseEnv = baseEnv;
        this.toolArchive = toolArchive;
        this.binary = init(workDir, debug);
        this.executor = Executors.newCachedThreadPool();
    }

    public Result exec(Path pwd, String logPrefix, String... args) throws Exception {
        return exec(pwd, logPrefix, false, Collections.emptyMap(), Arrays.asList(args));
    }

    public Result exec(Path pwd, String logPrefix, boolean silent, Map<String, String> env, List<String> args) throws Exception {
        List<String> cmd = new ArrayList<>();
        cmd.add(binary.toAbsolutePath().toString());
        cmd.addAll(args);

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

    // During the init we will download the version of Terraform specified by the user if defined, otherwise
    // we will download the default version. Terraform URLs look like the following:
    //
    // https://releases.hashicorp.com/terraform/0.12.5/terraform_0.12.5_linux_amd64.zip
    // https://releases.hashicorp.com/terraform/0.11.2/terraform_0.11.2_linux_amd64.zip
    //
    // So we can generalize to:
    //
    // https://releases.hashicorp.com/terraform/${version}/terraform_${version}_linux_amd64.zip
    //
    // We will also allow the user to specify the full URL if they want to download the tool zip from
    // and internal repository manager or other internally managed host.
    //
    private Path init(Path workDir, boolean debug) throws Exception {
        Path dstDir = workDir.resolve(".terraform");
        if (!Files.exists(dstDir)) {
            Files.createDirectories(dstDir);
        }

        Path binaryDst = dstDir.resolve("terraform");
        if (Files.exists(binaryDst)) {
            if (debug) {
                log.info("init -> using the existing binary {}", workDir.relativize(binaryDst));
            }
            return binaryDst;
        }

        if (debug) {
            log.info("init -> extracting the binary into {}", workDir.relativize(dstDir));
        }

        if (toolArchive == null) {
            throw new IllegalStateException(String.format("The Terraform archive '%s' does not appear to be valid.", toolArchive));
        }
        URL zipFile = toolArchive.toFile().toURI().toURL();
        System.out.println("!!!!! toolArchive " + toolArchive);
        try (InputStream in = zipFile.openStream()) {
            System.out.println("!!!!!!!! downloading to " + dstDir);
            IOUtils.unzip(in, dstDir);
        }



        return binaryDst;
    }

    private static void log(String prefix, String s) {
        System.out.print("\u001b[34mterraform\u001b[0m " + prefix + ": ");
        System.out.print(s);
        System.out.println();
    }

    private static String removeAnsiColors(String s) {
        return s.replaceAll("\u001B\\[[;\\d]*m", "");
    }

    private static class StreamReader implements Callable<String> {

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
