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
import com.walmartlabs.concord.sdk.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.Map;

public class SupportedBackend implements Backend {

    private static final Logger log = LoggerFactory.getLogger(SupportedBackend.class);

    private final boolean debug;
    private final String id;
    private final Map<String, Object> backendParameters;
    private final ObjectMapper objectMapper;

    public SupportedBackend(String id, boolean debug, Map<String, Object> backendParameters, ObjectMapper objectMapper) {
        this.debug = debug;
        this.id = id;
        this.backendParameters = backendParameters;
        this.objectMapper = objectMapper;
    }

    public String getId() {
        return id;
    }

    @Override
    public boolean supportsOutFiles() {
        // the "remote" backend doesn't support "out" files
        return !Constants.BACKEND_REMOTE_KEY.equals(id);
    }

    @Override
    public void lock(Context ctx) throws Exception {
        // do nothing
    }

    @Override
    public void unlock(Context ctx) throws Exception {
        // do nothing
    }

    @Override
    public void init(Context ctx, Path tfDir) throws Exception {
        //
        // Write out a JSON file to instruct Terraform to override the backend
        //
        // {
        //   "terraform": {
        //     "backend": {
        //       "s3": {
        //         "bucket": "bucket-value",
        //         "dynamodb_table": "dynamodb_table-value",
        //         "encrypt": "encrypt-value",
        //         "region": "region-value",
        //         "key": "key-value"
        //       }
        //     }
        //   }
        // }
        //
        Map<String, Object> cfg = Collections.singletonMap("terraform",
                Collections.singletonMap("backend",
                        Collections.singletonMap(id, backendParameters)));

        Path p = tfDir.resolve("concord_override.tf.json");
        try (OutputStream out = Files.newOutputStream(p, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            objectMapper.writeValue(out, cfg);
        }

        if (debug) {
            log.info("init -> created backendId configuration file in {}", p.toAbsolutePath().toString());
        }
    }
}
