package com.walmartlabs.concord.plugins.codecoverage;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2024 Walmart Inc., Concord Authors
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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.walmartlabs.concord.runtime.v2.runner.PersistenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.stream.Stream;

@Singleton
public class StepsRecorder {

    private static final Logger log = LoggerFactory.getLogger(StepsRecorder.class);

    private static final TypeReference<List<StepInfo>> STEPS_TYPE = new TypeReference<>() {
    };

    private static final String FILE_NAME = "code-coverage-steps.yaml";

    private final PersistenceService persistenceService;
    private final ObjectMapper objectMapper;

    @Inject
    public StepsRecorder(PersistenceService persistenceService) {
        this.persistenceService = persistenceService;
        this.objectMapper = new ObjectMapper(
                new YAMLFactory()
                        .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER));
    }

    public synchronized void record(StepInfo step) {
        persistenceService.persistFile(FILE_NAME,
                out -> objectMapper.writeValue(out, List.of(step)),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    public List<StepInfo> list() {
        var result = persistenceService.loadPersistedFile(FILE_NAME, in -> objectMapper.readValue(in, STEPS_TYPE));
        if (result == null) {
            return List.of();
        }
        return result;
    }

    public synchronized void cleanup() {
        try {
            persistenceService.deletePersistedFile(FILE_NAME);
        } catch (IOException e) {
            log.warn("Can't cleanup steps from state: {}", e.getMessage());
        }
    }
}
