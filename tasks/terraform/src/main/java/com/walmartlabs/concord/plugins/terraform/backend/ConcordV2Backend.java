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
import com.walmartlabs.concord.ApiClient;
import com.walmartlabs.concord.client.JsonStoreApi;
import com.walmartlabs.concord.client.JsonStoreRequest;
import com.walmartlabs.concord.plugins.terraform.TaskConstants;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.LockService;
import com.walmartlabs.concord.runtime.v2.sdk.ProjectInfo;
import com.walmartlabs.concord.sdk.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ConcordV2Backend implements Backend {

    private static final Logger log = LoggerFactory.getLogger(ConcordV2Backend.class);

    private final Context ctx;
    private final Map<String, Object> cfg;
    private final ApiClient apiClient;
    private final boolean debug;
    private final LockService lockService;
    private final ObjectMapper objectMapper;

    public ConcordV2Backend(Context ctx,
                            Map<String, Object> cfg,
                            ApiClient apiClient,
                            boolean debug,
                            LockService lockService,
                            ObjectMapper objectMapper) {

        this.ctx = ctx;
        this.cfg = cfg;
        this.apiClient = apiClient;
        this.debug = debug;
        this.lockService = lockService;
        this.objectMapper = objectMapper;
    }

    public String getId() {
        return "concord";
    }

    @Override
    public void lock() throws Exception {
        lockService.projectLock(getStateId(ctx, cfg));
    }

    @Override
    public void unlock() throws Exception {
        lockService.projectUnlock(getStateId(ctx, cfg));
    }

    @Override
    public void init(Path tfDir) throws Exception {
        String stateId = getStateId(ctx, cfg);
        if (debug) {
            log.info("init -> using state ID: {}", stateId);
        }

        String orgName = getCurrentOrgName(ctx);

        JsonStoreApi jsonStoreApi = new JsonStoreApi(apiClient);
        jsonStoreApi.createOrUpdate(orgName, new JsonStoreRequest().setName(stateId));

        String address = String.format("%s/api/v1/org/%s/jsonstore/%s/item/state", apiClient.getBasePath(), orgName, stateId);
        if (debug) {
            log.info("init -> using state URL: {}", address);
        }

        Map<String, Object> params = new HashMap<>();
        params.put("address", address);
        params.put("username", ctx.processConfiguration().processInfo().sessionToken());
        params.put("password", "");

        Map<String, Object> cfg = Collections.singletonMap("terraform",
                Collections.singletonMap("backend",
                        Collections.singletonMap("http", params)));

        Path configFile = tfDir.resolve("concord_override.tf.json").toAbsolutePath();

        Path parentDir = configFile.getParent();
        if (!Files.exists(parentDir)) {
            Path relative = ctx.workingDirectory().relativize(parentDir);
            throw new IllegalArgumentException("Can't create the backend configuration, the directory doesn't exist: " + relative);
        }

        try (OutputStream out = Files.newOutputStream(configFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            objectMapper.writeValue(out, cfg);
        }

        if (debug) {
            log.info("init -> created backend configuration file in {}", configFile.toAbsolutePath().toString());
        }
    }

    private static String getStateId(Context ctx, Map<String, Object> cfg) {
        String s = MapUtils.getString(cfg, TaskConstants.STATE_ID_KEY);
        if (s != null) {
            return s;
        }

        ProjectInfo projectInfo = ctx.processConfiguration().projectInfo();
        if (projectInfo == null || projectInfo.projectName() == null) {
            throw new IllegalArgumentException("Can't determine '" + TaskConstants.STATE_ID_KEY + "'. The 'concord' backend can only be used for processes running in a project.");
        }

        s = "tfState-" + projectInfo.projectName();

        String repoName = projectInfo.repoName();
        if (repoName != null) {
            s += "-" + repoName;
        }

        return s;
    }

    private static String getCurrentOrgName(Context ctx) {
        ProjectInfo projectInfo = ctx.processConfiguration().projectInfo();
        if (projectInfo == null || projectInfo.orgName() == null) {
            throw new IllegalArgumentException("Can't determine the current organization. The 'concord' backend can only be used for processes running in a project.");
        }
        return projectInfo.orgName();
    }
}
