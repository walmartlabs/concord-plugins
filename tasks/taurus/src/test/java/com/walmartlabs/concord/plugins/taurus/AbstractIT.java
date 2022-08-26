package com.walmartlabs.concord.plugins.taurus;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2022 Walmart Inc., Concord Authors
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

import com.walmartlabs.concord.plugins.taurus.docker.DockerService;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public abstract class AbstractIT {

    private Path workDir;
    private DockerService dockerService;

    @BeforeEach
    public void setup() throws IOException {
        this.workDir = Files.createTempDirectory("test");
        this.dockerService = new DockerService(workDir, Collections.emptyList());
    }

    protected static Optional<String> envToOptional(String varName) {
        String envVal = System.getenv(varName);

        if (envVal != null && !envVal.isEmpty()) {
            return Optional.of(envVal);
        }

        return Optional.empty();
    }

    protected Path workDir() {
        return this.workDir;
    }

    protected DockerService dockerService() {
        return this.dockerService;
    }

    protected void prepareScenario() throws IOException, URISyntaxException {
        Path scenarioFile = Paths.get(TaurusTaskV2IT.class.getResource("test.yml").toURI());
        Files.copy(scenarioFile, workDir.resolve("test.yml"));
    }

    protected Map<String, Object> getArgs() {
        Map<String, Object> args = new HashMap<>();
        args.put("useFakeHome", false);
        args.put("action", "run");
        args.put(com.walmartlabs.concord.sdk.Constants.Context.WORK_DIR_KEY, workDir.toString());
        args.put("configs", Arrays.asList(
                "test.yml",
                Collections.singletonMap("scenarios",
                        Collections.singletonMap("quick-test",
                                Collections.singletonMap("variables",
                                        Collections.singletonMap("apiToTest", envToOptional("TAURUS_TEST_API_ENDPOINT").orElse("http://localhost:8001/api/v1/server/ping")))))
        ));

        return args;
    }
}
