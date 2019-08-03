package com.walmartlabs.concord.plugins.packer.actions;

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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class Action {

    private static final Logger log = LoggerFactory.getLogger(Action.class);

    protected static Path createVarsFile(Path dir, ObjectMapper objectMapper, Map<String, Object> m) throws IOException {
        if (m == null || m.isEmpty()) {
            return null;
        }

        Path p = Files.createTempFile(dir, ".vars", ".json");
        try (OutputStream out = Files.newOutputStream(p, StandardOpenOption.TRUNCATE_EXISTING)) {
            objectMapper.writeValue(out, m);
        }

        return p;
    }
}
