package com.walmartlabs.concord.plugins.terraform.backend;

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
import com.walmartlabs.concord.sdk.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ConcordV1Backend implements Backend {

    private static final Logger log = LoggerFactory.getLogger(ConcordV1Backend.class);

    private final Context ctx;
    private final boolean debug;
    private final LockService lockService;
    private final ObjectStorage objectStorage;
    private final ObjectMapper objectMapper;

    public ConcordV1Backend(Context ctx, boolean debug, LockService lockService, ObjectStorage objectStorage, ObjectMapper objectMapper) {
        this.ctx = ctx;
        this.debug = debug;
        this.lockService = lockService;
        this.objectStorage = objectStorage;
        this.objectMapper = objectMapper;
    }

    public String getId() {
        return "concord";
    }

    @Override
    public void lock() throws Exception {
        lockService.projectLock(ctx, getStateId(ctx));
    }

    @Override
    public void unlock() throws Exception {
        lockService.projectUnlock(ctx, getStateId(ctx));
    }

    @Override
    public void init(Path tfDir) throws Exception {
        String stateId = getStateId(ctx);
        if (debug) {
            log.info("init -> using state ID: {}", stateId);
        }

        String address = objectStorage.createBucket(ctx, stateId).address();
        if (debug) {
            log.info("init -> using state URL: {}", address);
        }

        Map<String, Object> params = new HashMap<>();
        params.put("address", address);
        params.put("username", ContextUtils.getSessionToken(ctx));
        params.put("password", "");

        Map<String, Object> cfg = Collections.singletonMap("terraform",
                Collections.singletonMap("backend",
                        Collections.singletonMap("http", params)));

        Path configFile = tfDir.resolve("concord_override.tf.json").toAbsolutePath();

        Path parentDir = configFile.getParent();
        if (parentDir != null && !Files.exists(parentDir)) {
            Path relative = ContextUtils.getWorkDir(ctx).relativize(parentDir);
            throw new IllegalArgumentException("Can't create the backend configuration, the directory doesn't exist: " + relative);
        }

        try (OutputStream out = Files.newOutputStream(configFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            objectMapper.writeValue(out, cfg);
        }

        if (debug) {
            log.info("init -> created backend configuration file in {}", configFile.toAbsolutePath().toString());
        }
    }

    private static String getStateId(Context ctx) {
        String s = ContextUtils.getString(ctx, TaskConstants.STATE_ID_KEY);
        if (s != null) {
            return s;
        }

        ProjectInfo projectInfo = ContextUtils.getProjectInfo(ctx);
        if (projectInfo == null || projectInfo.name() == null) {
            throw new IllegalArgumentException("Can't determine '" + TaskConstants.STATE_ID_KEY + "': the process is not running in a project");
        }

        s = "tfState-" + projectInfo.name();

        RepositoryInfo repositoryInfo = ContextUtils.getRepositoryInfo(ctx);
        if (repositoryInfo != null && repositoryInfo.name() != null) {
            s += "-" + repositoryInfo.name();
        }

        return s;
    }
}
