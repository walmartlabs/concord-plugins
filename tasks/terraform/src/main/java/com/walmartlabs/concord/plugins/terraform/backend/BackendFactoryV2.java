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
import com.walmartlabs.concord.client2.ApiClient;
import com.walmartlabs.concord.plugins.terraform.TaskConstants;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.LockService;
import com.walmartlabs.concord.sdk.MapUtils;

import java.nio.file.Path;
import java.util.Map;

public class BackendFactoryV2 extends BackendFactory {

    private final Context ctx;
    private final ApiClient apiClient;
    private final LockService lockService;

    public BackendFactoryV2(Context ctx, ApiClient apiClient, LockService lockService) {
        this.ctx = ctx;
        this.apiClient = apiClient;
        this.lockService = lockService;
    }

    @Override
    protected Backend createRemoteBackend(String backendId, boolean debug, Map<String, Object> cfg, ObjectMapper objectMapper) {
        Map<String, Object> backend = MapUtils.getMap(cfg, TaskConstants.BACKEND_KEY, null);
        Path workDir = ctx.workingDirectory();
        return new RemoteBackend(backendId, debug, MapUtils.getMap(backend, backendId, null), objectMapper, workDir);
    }

    @Override
    protected Backend createConcordBackend(boolean debug, Map<String, Object> cfg, ObjectMapper objectMapper) {
        return new ConcordV2Backend(ctx, cfg, apiClient, debug, lockService, objectMapper);
    }
}
