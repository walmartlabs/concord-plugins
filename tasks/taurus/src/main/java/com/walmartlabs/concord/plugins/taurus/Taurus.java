package com.walmartlabs.concord.plugins.taurus;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2018 Wal-Mart Store, Inc.
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Taurus {

    private static final URL JMETER_DIST = Taurus.class.getResource(Constants.DEPENDENCY_JMETER);

    private static final URL[] JMETER_EXT_FILES = {
            Taurus.class.getResource(Constants.DEPENDENCY_CASUTG),
            Taurus.class.getResource(Constants.DEPENDENCY_PLUGINS_MGR),
            Taurus.class.getResource(Constants.DEPENDENCY_PRMCTL)
    };

    private static final URL[] JMETER_LIBS = {
            Taurus.class.getResource(Constants.DEPENDENCY_CMD_RUNNER)
    };

    private static final URL PLUGIN_MANAGER_CMD_SCRIPT = Taurus.class.getResource(Constants.PLUGIN_MANAGER_CMD);
    private static final URL DUMMY_PLUGIN_MANAGER_CMD_SCRIPT = Taurus.class.getResource(Constants.DUMMY_PLUGIN_MANAGER_CMD);

    private static final String FAKE_HOME_PATH = ".bzt/home";

    private final ExecutorService executor;
    private final Map<String, String> defaultEnv;

    public Taurus(Path workDir, boolean useFakeHome, boolean useDummyPluginManagerCmd) throws IOException {
        this.executor = Executors.newCachedThreadPool();
        this.defaultEnv = new HashMap<>();

        if (useFakeHome) {
            // set up a fake ${HOME} to avoid using the system's one
            // assumes that `bzt` is installed system-wide
            defaultEnv.put("HOME", Utils.assertDir(workDir, FAKE_HOME_PATH).toAbsolutePath().toString());
        }

        init(workDir, useDummyPluginManagerCmd);
    }

    public Result exec(List<String> args, String logPrefix) throws Exception {
        List<String> call = new LinkedList<>();
        call.add(Constants.BINARY_NAME);
        call.addAll(args);

        ProcessBuilder b = new ProcessBuilder()
                .command(call);

        b.environment().putAll(defaultEnv);

        Process p = b.start();

        Future<String> stderr = executor.submit(new StreamReader(logPrefix, p.getErrorStream()));
        Future<String> stdout = executor.submit(new StreamReader(logPrefix, p.getInputStream()));

        int code = p.waitFor();
        if (code != 0) {
            return Result.fail(code, stdout.get(), stderr.get());
        }

        return Result.ok(stdout.get(), stderr.get());
    }

    private static void init(Path workDir, boolean useDummyPluginManagerCmd) throws IOException {
        // copy and extract jmeter
        Path jmeterDir = workDir.resolve(Constants.JMETER_TMP_DIR);
        copyAndExtract(JMETER_DIST, workDir, jmeterDir);

        // copy jmeter extensions
        Path extDir = workDir.resolve(Constants.JMETER_PATH_EXT);
        for (URL url : JMETER_EXT_FILES) {
            copy(url, extDir);
        }

        // copy jmeter extra libs
        Path libDir = workDir.resolve(Constants.JMETER_PATH_LIB);
        for (URL url : JMETER_LIBS) {
            copy(url, libDir);
        }

        // setup pluginMgrCmd
        Path binDir = workDir.resolve(Constants.JMETER_PATH_BIN);

        Path p;
        if (useDummyPluginManagerCmd) {
            p = copy(DUMMY_PLUGIN_MANAGER_CMD_SCRIPT, binDir);
        } else {
            p = copy(PLUGIN_MANAGER_CMD_SCRIPT, binDir);
        }
        Files.setPosixFilePermissions(p, PosixFilePermissions.fromString("rwxr-xr-x"));
    }

    private static Path copy(URL url, Path dst) throws IOException {
        if (url == null) {
            throw new IllegalStateException("'" + url + "' not found. Check the dependencies");
        }

        try (InputStream in = url.openStream()) {
            Path p = dst.resolve(fileName(url));
            Files.copy(in, p, StandardCopyOption.REPLACE_EXISTING);
            return p;
        }
    }

    private static void copyAndExtract(URL url, Path workDir, Path dst) throws IOException {
        if (url == null) {
            throw new IllegalStateException("'" + url + "' not found. Check the dependencies");
        }

        try (InputStream in = url.openStream()) {
            // TODO replace with IOUtils#unzip(in, dst, true) when available
            Path tmp = Files.createTempFile(workDir, "file", ".zip");
            Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
            IOUtils.unzip(tmp, dst, true);
        }
    }

    private static String fileName(URL url) {
        String s = url.getFile();
        int i = s.lastIndexOf("/");
        if (i < 0 || i + 1 >= s.length()) {
            return s;
        }
        return s.substring(i + 1);
    }

    private static class StreamReader implements Callable<String> {

        private final String logPrefix;
        private final InputStream in;

        private StreamReader(String logPrefix, InputStream in) {
            this.logPrefix = logPrefix;
            this.in = in;
        }

        @Override
        public String call() throws Exception {
            StringBuilder sb = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    log(logPrefix, line);

                    sb.append(removeAnsiColors(line))
                            .append(System.lineSeparator());
                }
            }

            return sb.toString();
        }
    }

    private static String removeAnsiColors(String s) {
        return s.replaceAll("\u001B\\[[;\\d]*m", "");
    }

    private static void log(String prefix, String s) {
        System.out.print("\u001b[34mtaurus\u001b[0m " + prefix + ": ");
        System.out.print(s);
        System.out.println();
    }

    public static class Result {

        public static Result ok(String stdout, String stderr) {
            return new Result(true, 0, stdout, stderr, null);
        }

        public static Result fail(int code, String stdout, String stderr) {
            return new Result(false, code, stdout, stderr, null);
        }

        public static Result error(String error) {
            return new Result(false, -1, null, null, error);
        }

        private final boolean ok;
        private final int code;
        private final String stdout;
        private final String stderr;
        private final String error;

        private Result(boolean ok, int code, String stdout, String stderr, String error) {
            this.ok = ok;
            this.code = code;
            this.stdout = stdout;
            this.stderr = stderr;
            this.error = error;
        }

        public boolean isOk() {
            return ok;
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

        public String getError() {
            return error;
        }
    }
}
