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

import com.walmartlabs.concord.sdk.Context;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

public interface Backend {

    String getId();

    void lock(Context ctx) throws Exception;

    void unlock(Context ctx) throws Exception;

    /**
     * Initialized the backend.
     *
     * @param ctx   the process' context
     * @param tfDir a directory with Terraform files
     */
    void init(Context ctx, Path tfDir) throws Exception;

    /**
     * Prepares the backend-specific environment variables.
     */
    default Map<String, String> prepareEnv(Context ctx, Map<String, Object> cfg) {
        return Collections.emptyMap();
    }

    /**
     * @return if {@code true} the backend supports {@code -out} parameter
     * (can save output variables). Default is {@code true}.
     */
    default boolean supportsOutFiles() {
        return true;
    }
}
