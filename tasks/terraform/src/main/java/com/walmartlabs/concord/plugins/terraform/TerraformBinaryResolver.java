package com.walmartlabs.concord.plugins.terraform;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2020 Walmart Inc., Concord Authors
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
import com.walmartlabs.concord.sdk.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class TerraformBinaryResolver {

    private static final Logger log = LoggerFactory.getLogger(TerraformBinaryResolver.class);

    private static final String DEFAULT_TERRAFORM_VERSION = "0.12.5";
    private static final String DEFAULT_TOOL_URL_TEMPLATE = "https://releases.hashicorp.com/terraform/%s/terraform_%s_%s_amd64.zip";

    private final Map<String, Object> cfg;
    private final Path workDir;
    private final boolean debug;
    private final BinaryDownloader binaryDownloader;

    public TerraformBinaryResolver(Map<String, Object> cfg, Path workDir, boolean debug, BinaryDownloader binaryDownloader) {
        this.cfg = cfg;
        this.workDir = workDir;
        this.debug = debug;
        this.binaryDownloader = binaryDownloader;
    }

    /**
     * Initialize the Terraform binary.
     * <p>
     * The method tries the following:
     * - downloads the binary's archive if "toolUrl" is specified
     * - the local ${workDir}/.terraform/terraform
     * - a "terraform" binary in $PATH (unless "ignoreLocalBinary" is set to "true")
     * - downloads the binary using the standard URL
     * <p>
     * During the init we might download the version of Terraform specified by the user if defined, otherwise
     * we will download the default version. Terraform URLs look like the following:
     * <p>
     * https://releases.hashicorp.com/terraform/0.12.5/terraform_0.12.5_linux_amd64.zip
     * https://releases.hashicorp.com/terraform/0.11.2/terraform_0.11.2_linux_amd64.zip
     * <p>
     * So we can generalize to:
     * <p>
     * https://releases.hashicorp.com/terraform/${version}/terraform_${version}_linux_amd64.zip
     * <p>
     * We will also allow the user to specify the full URL if they want to download the tool zip from
     * and internal repository manager or other internally managed host.
     *
     * @return
     * @throws Exception
     */
    public Path resolve() throws Exception {
        Path dstDir = workDir.resolve(".terraform");
        if (!Files.exists(dstDir)) {
            Files.createDirectories(dstDir);
        }

        // try ${workDir}/.terraform/terraform first
        Path binaryInWorkDir = dstDir.resolve("terraform");
        if (Files.exists(binaryInWorkDir)) {
            if (debug) {
                log.info("init -> using the existing binary {}", workDir.relativize(binaryInWorkDir));
            }
            return binaryInWorkDir;
        }

        // try toolUrl first
        String toolUrl = userToolUrl(cfg);
        if (toolUrl != null) {
            downloadAndUnpack(toolUrl, dstDir);
        }

        boolean ignoreLocalBinary = MapUtils.getBoolean(cfg, TaskConstants.IGNORE_LOCAL_BINARY_KEY, false);
        if (!ignoreLocalBinary) {
            // try $PATH next
            Path p = findBinaryInPath();
            if (p != null) {
                if (debug) {
                    log.info("init -> using the existing binary {}", p);
                }
                return p;
            }
        }

        // fallback: try to download the tool from a standard URL
        toolUrl = defaultToolUrl(cfg);
        downloadAndUnpack(toolUrl, dstDir);

        if (debug) {
            log.info("init -> extracting the binary into {}", workDir.relativize(dstDir));
        }

        return binaryInWorkDir;
    }

    private void downloadAndUnpack(String toolUrl, Path dst) throws IOException {
        Path p = binaryDownloader.download(toolUrl);
        try (InputStream in = Files.newInputStream(p)) {
            IOUtils.unzip(in, dst);
        }
    }

    /**
     * Tries to run {@code which terraform} to determine whether the binary is present in $PATH or not.
     */
    private Path findBinaryInPath() throws InterruptedException {
        try {
            Process proc = new ProcessBuilder().command("which", "terraform")
                    .start();

            int code = proc.waitFor();
            if (code != 0) {
                if (debug) {
                    log.info("findBinaryInPath -> non-zero exit code: {}", code);
                }
                return null;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                String s = reader.readLine();
                return Paths.get(s.trim());
            }
        } catch (IOException e) {
            if (debug) {
                log.warn("findBinaryInPath -> error: {}", e.getMessage());
            }
            return null;
        }
    }

    private static String userToolUrl(Map<String, Object> cfg) {
        String toolUrl = MapUtils.getString(cfg, TaskConstants.TOOL_URL_KEY);
        if (toolUrl != null && !toolUrl.isEmpty()) {
            // the user has explicitly specified a URL from where to download the tool.
            return toolUrl;
        }
        return null;
    }

    private static String defaultToolUrl(Map<String, Object> cfg) {
        String tfOs;
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("mac")) {
            tfOs = "darwin";
        } else if (os.contains("nux")) {
            tfOs = "linux";
        } else if (os.contains("win")) {
            tfOs = "windows";
        } else if (os.contains("sunos")) {
            tfOs = "solaris";
        } else {
            throw new IllegalArgumentException("Your operating system is not supported: " + os);
        }

        // check to see if the user has specified a version of the tool to use, if not use the default version.
        String toolVersion = MapUtils.getString(cfg, TaskConstants.TOOL_VERSION_KEY, DEFAULT_TERRAFORM_VERSION);
        return String.format(DEFAULT_TOOL_URL_TEMPLATE, toolVersion, toolVersion, tfOs);
    }

    public interface BinaryDownloader {

        Path download(String url) throws IOException;
    }
}
