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
import com.walmartlabs.concord.plugins.terraform.Constants;
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

public class ConcordBackend implements Backend {

    private static final Logger log = LoggerFactory.getLogger(ConcordBackend.class);

    private final boolean debug;
    private final LockService lockService;
    private final ObjectStorage objectStorage;
    private final ObjectMapper objectMapper;

    public ConcordBackend(boolean debug, LockService lockService, ObjectStorage objectStorage, ObjectMapper objectMapper) {
        this.debug = debug;
        this.lockService = lockService;
        this.objectStorage = objectStorage;
        this.objectMapper = objectMapper;
    }

    public String getId() {
        return "concord";
    }

    @Override
    public void lock(Context ctx) throws Exception {
        lockService.projectLock(ctx, getStateId(ctx));
    }

    @Override
    public void unlock(Context ctx) throws Exception {
        lockService.projectUnlock(ctx, getStateId(ctx));
    }

    @Override
    public void init(Context ctx, Path tfDir) throws Exception {
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

        Path p = tfDir.resolve("concord_override.tf.json");
        try (OutputStream out = Files.newOutputStream(p, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            objectMapper.writeValue(out, cfg);
        }

        if (debug) {
            log.info("init -> created backend configuration file in {}", p.toAbsolutePath().toString());
        }
    }

    private static String getStateId(Context ctx) {
        String s = ContextUtils.getString(ctx, Constants.STATE_ID_KEY);
        if (s != null) {
            return s;
        }

        ProjectInfo projectInfo = ContextUtils.getProjectInfo(ctx);
        if (projectInfo == null || projectInfo.name() == null) {
            throw new IllegalArgumentException("Can't determine '" + Constants.STATE_ID_KEY + "': the process is not running in a project");
        }

        s = "tfState-" + projectInfo.name();

        RepositoryInfo repositoryInfo = ContextUtils.getRepositoryInfo(ctx);
        if (repositoryInfo != null && repositoryInfo.name() != null) {
            s += "-" + repositoryInfo.name();
        }

        return s;
    }
}
