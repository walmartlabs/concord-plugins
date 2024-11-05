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
import com.walmartlabs.concord.runtime.v2.sdk.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ecr.EcrClient;
import software.amazon.awssdk.services.ecr.model.*;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;

@Named("awsEcr")
@DryRunReady
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
        } if ("delete-images".equals(action)) {
            return deleteImage(input);
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


    private TaskResult deleteImage(Variables input) {
        var region = assertRegion(input, "region");
        var repositoryName = input.assertString("repositoryName");
        var imageIds = assertImageIds(input);
        var debug = input.getBoolean("debug", context.processConfiguration().debug());

        if (context.processConfiguration().dryRun()) {
            log.info("Dry-run mode enabled: Skipping image deletion");
            return TaskResult.success();
        }

        try (var client = EcrClient.builder()
                .region(region)
                .build()) {

            List<ImageFailure> failures = new ArrayList<>();
            for (var ids : partitions(imageIds, 100)) {
                var request = BatchDeleteImageRequest.builder()
                        .repositoryName(repositoryName)
                        .imageIds(ids)
                        .build();

                var response = client.batchDeleteImage(request);
                if (response.hasFailures()) {
                    failures.addAll(response.failures());
                }

                if (debug) {
                    log.info("Processed {}/{}, failures: {}", ids.size(), imageIds.size(), failures.size());
                }
            }

            if (!failures.isEmpty()) {
                return TaskResult.fail("Failures in response")
                        .values(Map.of("failures", serialize(failures)));
            }

            return TaskResult.success();
        }
    }

    private static List<Map<String, Object>> serialize(List<ImageFailure> failures) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (var failure : failures) {
            result.add(Map.of("imageId", serialize(failure.imageId()),
                    "failureCode", failure.failureCode(),
                    "failureReason", failure.failureReason()));
        }

        return result;
    }

    private static String serialize(ImageIdentifier imageIdentifier) {
        if (imageIdentifier == null) {
            return null;
        }
        if (imageIdentifier.imageTag() != null) {
            return imageIdentifier.imageTag();
        }
        return imageIdentifier.imageDigest();
    }

    private static Region assertRegion(Variables input, String key) {
        String region = input.assertString(key);
        return Region.of(region);
    }

    private static List<ImageIdentifier> assertImageIds(Variables input) {
        String imageTag = input.getString("imageTag");
        if (imageTag != null) {
            return List.of(ImageIdentifier.builder().imageTag(imageTag).build());
        }

        List<String> imageTags = input.getList("imageTags", List.of());
        if (!imageTags.isEmpty()) {
            return imageTags.stream().map(i -> ImageIdentifier.builder().imageTag(i).build()).toList();
        }

        throw new IllegalArgumentException("Missing 'imageTags' or 'imageTags' in the input variable");
    }

    private static <T> List<List<T>> partitions(List<T> list, int size) {
        List<List<T>> parts = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            parts.add(new ArrayList<>(
                    list.subList(i, Math.min(list.size(), i + size)))
            );
        }
        return parts;
    }
}
