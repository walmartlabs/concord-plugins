package com.walmartlabs.concord.plugins.jenkins;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2018 Walmart Inc.
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


import com.walmartlabs.concord.plugins.jenkins.model.BuildInfo;
import com.walmartlabs.concord.plugins.jenkins.model.Executable;
import com.walmartlabs.concord.plugins.jenkins.model.QueueItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class JenkinsTaskCommon {

    private static final Logger log = LoggerFactory.getLogger(JenkinsTaskCommon.class);

    private static final String FILE_PATH_PREFIX = "@";
    private static final int WAIT_DELAY = 15000;

    public Map<String, Object> execute(JenkinsConfiguration cfg) throws Exception {
        if (cfg.isDebug()) {
            log.info("Starting Jenkins job '{}' with parameters '{}' on {}",
                    cfg.getJobName(), cfg.getParameters(), cfg.getBaseUrl());
        } else {
            log.info("Starting Jenkins job '{}' on {}", cfg.getJobName(), cfg.getBaseUrl());
        }

        Map<String, String> simpleParams = new HashMap<>();
        Map<String, File> fileParams = new HashMap<>();
        categorizeParams(cfg.getParameters(), simpleParams, fileParams);

        JenkinsClient client = new JenkinsClient(cfg);
        String queueLink = client.build(cfg.getJobName(), simpleParams, fileParams);

        long startTimeMs = System.currentTimeMillis();
        log.info("Waiting for the start of the job, queue link {}", queueLink);
        Executable executable = waitBuildStart(cfg, client, queueLink, startTimeMs);

        BuildInfo buildInfo = client.getBuildInfo(executable);
        log.info("Jenkins job status '{}' (still building={})", buildInfo.getResult(), buildInfo.isBuilding());

        if (!isFinalStatus(buildInfo.getResult()) && cfg.isSync()) {
            log.info("Waiting for completion of the build...");
            buildInfo = waitForCompletion(cfg, client, executable, startTimeMs);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("status", buildInfo.getResult());
        result.put("buildNumber", executable.getNumber());
        result.put("isSuccess", "SUCCESS".equalsIgnoreCase(buildInfo.getResult()));
        return result;
    }

    private static Executable waitBuildStart(JenkinsConfiguration cfg, JenkinsClient client, String queueLink, long startTimeMs) throws Exception {
        while (!Thread.currentThread().isInterrupted()) {
            QueueItem queueItem = client.getQueueItem(queueLink);

            if (queueItem.isCancelled()) {
                log.info("Jenkins job cancelled");
                throw new RuntimeException("Job cancelled");
            }

            if (queueItem.getExecutable() != null) {
                return queueItem.getExecutable();
            }

            if (queueItem.getWhy() != null) {
                log.info("Jenkins job in queue, reason: '{}'", queueItem.getWhy());
            }

            assertJobTimeout(cfg, startTimeMs);

            sleep(WAIT_DELAY);
        }

        throw new RuntimeException("Job build was interrupted");
    }

    private static BuildInfo waitForCompletion(JenkinsConfiguration cfg, JenkinsClient client, Executable executable, long startTimeMs) throws Exception {
        while (!Thread.currentThread().isInterrupted()) {
            BuildInfo buildInfo = client.getBuildInfo(executable);

            if (buildInfo.isBuilding()) {
                log.info("Jenkins job is building, next check after {} ms", WAIT_DELAY);
            }

            if (isFinalStatus(buildInfo.getResult())) {
                return buildInfo;
            }

            assertJobTimeout(cfg, startTimeMs);

            Thread.sleep(WAIT_DELAY);
        }

        throw new RuntimeException("Job wait was interrupted");
    }

    private static void categorizeParams(Map<String, Object> params,
                                         Map<String, String> simpleParams,
                                         Map<String, File> fileParams) {

        for (Map.Entry<String, Object> e : params.entrySet()) {
            if (String.valueOf(e.getValue()).startsWith(FILE_PATH_PREFIX)) {
                String fileName = String.valueOf(e.getValue()).substring(1);
                File f = new File(fileName);
                if (!f.exists()) {
                    throw new RuntimeException("File does not exists: " + fileName);
                }
                fileParams.put(e.getKey(), f);
            } else {
                simpleParams.put(e.getKey(), String.valueOf(e.getValue()));
            }
        }
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void assertJobTimeout(JenkinsConfiguration cfg, long startTimeMs) {
        long timeout = cfg.getJobTimeout();
        if (timeout <= 0) {
            return;
        }

        long currentTimeMs = System.currentTimeMillis();
        if (currentTimeMs - startTimeMs >= timeout * 1000) {
            throw new RuntimeException("Timeout waiting for the job");
        }
    }


    private static boolean isFinalStatus(String s) {
        if (s == null) {
            return false;
        }

        return s.equalsIgnoreCase("CANCELLED")
                || s.equalsIgnoreCase("SUCCESS")
                || s.equalsIgnoreCase("UNSTABLE")
                || s.equalsIgnoreCase("FAILURE")
                || s.equalsIgnoreCase("ABORTED");
    }
}
