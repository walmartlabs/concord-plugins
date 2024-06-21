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
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.Task;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import com.walmartlabs.concord.runtime.v2.sdk.Variables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ecr.EcrClient;
import software.amazon.awssdk.services.ecr.model.DescribeImagesRequest;
import software.amazon.awssdk.services.ecr.model.ImageDetail;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Map;

import static java.util.Objects.requireNonNull;

@Named("awsEcr")
public class EcrTask implements Task {

    private static final Logger log = LoggerFactory.getLogger(EcrTask.class);

    private final Context context;
    private final ObjectMapper objectMapper;

    @Inject
    public EcrTask(Context context, ObjectMapper objectMapper) {
        this.context = context;
        this.objectMapper = requireNonNull(objectMapper).copy()
                .registerModule(new JavaTimeModule())
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
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
        var maxResults = input.getInt("maxResults", 100);
        var debug = input.getBoolean("debug", context.processConfiguration().debug());

        if (debug) {
            log.info("Using region={}, maxResults={}", region, maxResults);
        }

        try (var client = EcrClient.builder()
                .region(region)
                .build()) {

            if (debug) {
                log.info("Describing images in repository '{}'", repositoryName);
            }

            var request = DescribeImagesRequest.builder()
                    .repositoryName(repositoryName)
                    .maxResults(maxResults)
                    .build();

            var data = client.describeImagesPaginator(request).stream()
                    .flatMap(response -> response.imageDetails().stream())
                    .map(ImageDetail::toBuilder)
                    .map(b -> (Map<?, ?>) objectMapper.convertValue(b, Map.class))
                    .toList();

            if (debug) {
                log.info("Done: {}", data.size());
            }

            return TaskResult.success().values(Map.of("imageDetails", data));
        }
    }

    private static Region assertRegion(Variables input, String key) {
        String region = input.assertString(key);
        return Region.of(region);
    }
}
