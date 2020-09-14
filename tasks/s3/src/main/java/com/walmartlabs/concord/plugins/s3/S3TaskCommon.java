package com.walmartlabs.concord.plugins.s3;

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

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.util.IOUtils;
import com.walmartlabs.concord.sdk.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static com.walmartlabs.concord.plugins.s3.TaskParams.GetObjectParams;
import static com.walmartlabs.concord.plugins.s3.TaskParams.PutObjectParams;

public class S3TaskCommon {

    private static final Logger log = LoggerFactory.getLogger(S3TaskCommon.class);

    private final Path workDir;

    public S3TaskCommon(Path workDir) {
        this.workDir = workDir;
    }

    public Result execute(TaskParams in) throws Exception {
        Result r;
        try {
            switch (in.action()) {
                case PUTOBJECT: {
                    r = putObject((PutObjectParams)in);
                    break;
                }
                case GETOBJECT: {
                    r = getObject((GetObjectParams)in);
                    break;
                }
                default: {
                    throw new IllegalArgumentException("Unknown action: " + in.action());
                }
            }
        } catch (Exception e) {
            if (!in.ignoreErrors()) {
                throw e;
            }

            r = new ErrorResult(e.getMessage());
        }

        return r;
    }

    private Result putObject(PutObjectParams in) {
        String src = in.src();

        Path p = workDir.resolve(src);
        if (!Files.exists(p)) {
            throw new IllegalArgumentException("'" + PutObjectParams.SRC_KEY + "' doesn't exist: " + p);
        }

        String bucketName = in.bucketName();
        String key = in.key();
        log.info("Putting an object into {}/{}...", bucketName, key);

        AmazonS3 s3 = createClient(in);

        boolean autoCreateBucket = in.autoCreateBucket();
        if (autoCreateBucket) {
            if (!s3.doesBucketExistV2(bucketName)) {
                log.info("Creating the bucket: {}", bucketName);
                s3.createBucket(bucketName);
            }
        }

        com.amazonaws.services.s3.model.PutObjectResult r = s3.putObject(bucketName, key, p.toFile());
        log.info("Successfully put an object into {}/{}...", bucketName, key);

        return new PutObjectResult(r.getETag(), r.getContentMd5());
    }

    private Result getObject(GetObjectParams in) throws IOException {
        // If a dest has been specified we will use that as the name of the local path for the object that
        // is being retrieved, otherwise we will use the key of the object
        String key = in.key();
        Path dst = workDir.resolve(in.dst(key));
        String relativePath = workDir.relativize(dst).toString();

        String bucketName = in.bucketName();
        log.info("Getting an object from {}/{} into {}...", bucketName, key, relativePath);

        AmazonS3 s3 = createClient(in);

        S3Object o = s3.getObject(bucketName, key);
        try (OutputStream out = Files.newOutputStream(dst);
             InputStream is = o.getObjectContent()) {
            IOUtils.copy(is, out);
        }

        log.info("Successfully retrieved an object from {}/{} and stored as {}", bucketName, key, relativePath);

        return new GetObjectResult(workDir.relativize(dst).toString());
    }

    private static AmazonS3 createClient(TaskParams in) {
        AmazonS3ClientBuilder clientBuilder = AmazonS3ClientBuilder.standard();

        boolean pathStyleAccess = in.pathStyleAccess();
        if (pathStyleAccess) {
            clientBuilder.enablePathStyleAccess();
        }

        configureEndpoint(clientBuilder, in);
        configureCredentials(clientBuilder, in);

        return clientBuilder.build();
    }

    private static void configureEndpoint(AmazonS3ClientBuilder b, TaskParams in) {
        String region = in.region();

        String endpoint = in.endpoint();
        if (endpoint != null) {
            b.withEndpointConfiguration(new EndpointConfiguration(endpoint, region));
        } else {
            b.withRegion(region);
        }
    }

    private static void configureCredentials(AmazonS3ClientBuilder b, TaskParams in) {
        Map<String, Object> m = in.auth();
        if (m == null || m.isEmpty()) {
            return;
        }

        // TODO add support for Concord secrets

        Map<String, Object> basicAuth = MapUtils.getMap(m, "basic", null);
        if (basicAuth == null || basicAuth.isEmpty()) {
            throw new IllegalArgumentException("Missing credentials. Specify the '" + Constants.AUTH_KEY + ".basic' parameter");
        }

        String accessKey = MapUtils.assertString(basicAuth, "accessKey");
        String secretKey = MapUtils.assertString(basicAuth, "secretKey");
        AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);

        b.withCredentials(new AWSStaticCredentialsProvider(credentials));
    }

}
