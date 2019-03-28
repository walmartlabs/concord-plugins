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
import com.walmartlabs.concord.sdk.Context;
import com.walmartlabs.concord.sdk.InjectVariable;
import com.walmartlabs.concord.sdk.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Named("jenkins")
public class JenkinsTask implements Task {

    private static final Logger log = LoggerFactory.getLogger(JenkinsTask.class);

    private static final String FILE_PATH_PREFIX = "@";
    private static final String OUT_VARIABLE_KEY = "jenkinsJob";
    private static final int WAIT_DELAY = 15000;

    @InjectVariable("jenkinsParams")
    private Map<String, Object> defaults;

    public void execute(Context ctx) throws Exception {
        JenkinsConfiguration cfg = createCfg(ctx);

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

        long jobStartedAt = System.currentTimeMillis();
        log.info("Waiting for the start of the job, queue link {}", queueLink);
        Executable executable = waitBuildStart(cfg, client, queueLink, jobStartedAt);

        BuildInfo buildInfo = client.getBuildInfo(executable);
        log.info("Jenkins job status '{}' (still building={})", buildInfo.getResult(), buildInfo.isBuilding());

        if (!isFinalStatus(buildInfo.getResult()) && cfg.isSync()) {
            log.info("Waiting for completion of the build...");
            buildInfo = waitForCompletion(cfg, client, executable, jobStartedAt);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("status", buildInfo.getResult());
        result.put("buildNumber", executable.getNumber());
        result.put("isSuccess", "SUCCESS".equalsIgnoreCase(buildInfo.getResult()));

        ctx.setVariable(OUT_VARIABLE_KEY, result);
        log.info("Job info is saved as '{}' variable: {}", OUT_VARIABLE_KEY, result);
    }

    private static Executable waitBuildStart(JenkinsConfiguration cfg, JenkinsClient client, String queueLink, long startTime) throws Exception {
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

            assertJobTimeout(cfg, startTime);

            sleep(WAIT_DELAY);
        }

        throw new RuntimeException("Job build was interrupted");
    }

    private static BuildInfo waitForCompletion(JenkinsConfiguration cfg, JenkinsClient client, Executable executable, long jobStartedAt) throws Exception {
        while (!Thread.currentThread().isInterrupted()) {
            BuildInfo buildInfo = client.getBuildInfo(executable);

            if (buildInfo.isBuilding()) {
                log.info("Jenkins job is building, next check after {} ms", WAIT_DELAY);
            }

            if (isFinalStatus(buildInfo.getResult())) {
                return buildInfo;
            }

            assertJobTimeout(cfg, jobStartedAt);

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

    private JenkinsConfiguration createCfg(Context ctx) {
        Map<String, Object> m = new HashMap<>(defaults != null ? defaults : Collections.emptyMap());

        for (String k : Constants.ALL_IN_PARAMS) {
            put(m, k, ctx);
        }

        return new JenkinsConfiguration(m);
    }

    private static void put(Map<String, Object> m, String k, Context ctx) {
        Object v = ctx.getVariable(k);
        if (v == null) {
            return;
        }

        m.put(k, v);
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void assertJobTimeout(JenkinsConfiguration config, long startTime) {
        if (config.getJobTimeout() <= 0) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - startTime >= config.getJobTimeout()) {
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
