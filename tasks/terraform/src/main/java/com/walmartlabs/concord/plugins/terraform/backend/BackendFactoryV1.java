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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class BackendFactoryV1 extends BackendFactory {

    private final Context ctx;
    private final LockService lockService;
    private final ObjectStorage objectStorage;

    public BackendFactoryV1(Context ctx, LockService lockService, ObjectStorage objectStorage) {
        this.ctx = ctx;
        this.lockService = lockService;
        this.objectStorage = objectStorage;
    }

    @Override
    protected Backend createRemoteBackend(String backendId, boolean debug, Map<String, Object> cfg, ObjectMapper objectMapper) {
        Map<String, Object> backend = MapUtils.getMap(cfg, TaskConstants.BACKEND_KEY, null);
        Path workDir = Paths.get(ContextUtils.assertString(ctx, com.walmartlabs.concord.sdk.Constants.Context.WORK_DIR_KEY));
        return new RemoteBackend(backendId, debug, MapUtils.getMap(backend, backendId, null), objectMapper, workDir);
    }

    @Override
    protected Backend createConcordBackend(boolean debug, Map<String, Object> cfg, ObjectMapper objectMapper) {
        return new ConcordV1Backend(ctx, debug, lockService, objectStorage, objectMapper);
    }
}
