package com.walmartlabs.concord.plugins.aws;

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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.walmartlabs.concord.runtime.v2.sdk.Task;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import com.walmartlabs.concord.runtime.v2.sdk.Variables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ecr.EcrClient;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Map;

import static java.util.Objects.requireNonNull;

@Named("awsEcr")
public class EcrTask implements Task {

    private static final Logger log = LoggerFactory.getLogger(EcrTask.class);

    private final ObjectMapper objectMapper;

    @Inject
    public EcrTask(ObjectMapper objectMapper) {
        this.objectMapper = requireNonNull(objectMapper).copy()
                .registerModule(new JavaTimeModule())
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }

    @Override
    public TaskResult execute(Variables input) {
        var action = input.assertString("action");
        if ("describe-images".equals(action)) {
            return describeImages(input);
        }
        throw new IllegalArgumentException("Unsupported action: " + action);
    }

    private TaskResult describeImages(Variables input) {
        var region = assertRegion(input, "region");
        var repositoryName = input.assertString("repositoryName");
        var verbose = input.getBoolean("verbose", false);

        // create the client
        if (verbose) {
            log.info("Using region: {}", region);
        }
        var client = EcrClient.builder()
                .region(region)
                .build();

        // describe-images
        if (verbose) {
            log.info("Describing images in repository '{}'", repositoryName);
        }
        var result = client.describeImages(r -> r.repositoryName(repositoryName));
        if (verbose) {
            log.info("Done: {}", result.imageDetails().size());
        }

        // serialize result into POJOs
        var data = objectMapper.convertValue(result.toBuilder(), Map.class);
        //noinspection unchecked
        return TaskResult.success().values(data);
    }

    private static Region assertRegion(Variables input, String key) {
        String region = input.assertString(key);
        return Region.of(region);
    }
}
