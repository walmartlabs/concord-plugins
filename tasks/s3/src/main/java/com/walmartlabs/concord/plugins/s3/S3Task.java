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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.walmartlabs.concord.sdk.Context;
import com.walmartlabs.concord.sdk.MapUtils;
import com.walmartlabs.concord.sdk.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static com.walmartlabs.concord.sdk.ContextUtils.*;

@Named("s3")
public class S3Task implements Task {

    private static final Logger log = LoggerFactory.getLogger(S3Task.class);

    @Override
    public void execute(Context ctx) throws Exception {
        Action action = getAction(ctx);
        boolean ignoreErrors = getBoolean(ctx, Constants.IGNORE_ERRORS_KEY, false);

        Result r;
        try {
            switch (action) {
                case PUTOBJECT: {
                    r = putObject(ctx);
                    break;
                }
                case GETOBJECT: {
                    r = getObject(ctx);
                    break;
                }
                default: {
                    throw new IllegalArgumentException("Unknown action: " + action);
                }
            }
        } catch (Exception e) {
            if (!ignoreErrors) {
                throw e;
            }

            r = new ErrorResult(e.getMessage());
        }

        ObjectMapper om = new ObjectMapper();
        ctx.setVariable("result", om.convertValue(r, Map.class));
    }

    private Result putObject(Context ctx) {
        Path baseDir = Paths.get(assertString(ctx, com.walmartlabs.concord.sdk.Constants.Context.WORK_DIR_KEY));
        String src = assertString(ctx, Constants.SRC_KEY);

        Path p = baseDir.resolve(src);
        if (!Files.exists(p)) {
            throw new IllegalArgumentException("'" + Constants.SRC_KEY + "' doesn't exist: " + p);
        }

        String bucketName = assertString(ctx, Constants.BUCKET_NAME_KEY);
        String key = assertString(ctx, Constants.OBJECT_KEY);
        log.info("Putting an object into {}/{}...", bucketName, key);

        AmazonS3 s3 = createClient(ctx);

        boolean autoCreateBucket = getBoolean(ctx, Constants.AUTO_CREATE_BUCKET_KEY, false);
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

    private Result getObject(Context ctx) throws IOException {
        Path baseDir = Paths.get(assertString(ctx, com.walmartlabs.concord.sdk.Constants.Context.WORK_DIR_KEY));
        // If a dest has been specified we will use that as the name of the local path for the object that
        // is being retrieved, otherwise we will use the key of the object
        String key = assertString(ctx, Constants.OBJECT_KEY);
        Path dst = baseDir.resolve(getString(ctx, Constants.DEST_KEY, key));
        String relativePath = baseDir.relativize(dst).toString();

        String bucketName = assertString(ctx, Constants.BUCKET_NAME_KEY);
        log.info("Getting an object from {}/{} into {}...", bucketName, key, relativePath);

        AmazonS3 s3 = createClient(ctx);

        S3Object o = s3.getObject(bucketName, key);
        try (OutputStream out = Files.newOutputStream(dst);
             InputStream in = o.getObjectContent()) {
            IOUtils.copy(in, out);
        }

        log.info("Successfully retrieved an object from {}/{} and stored as {}", bucketName, key, relativePath);

        return new GetObjectResult(baseDir.relativize(dst).toString());
    }

    private static Action getAction(Context ctx) {
        String s = assertString(ctx, Constants.ACTION_KEY).trim().toUpperCase();
        return Action.valueOf(s);
    }

    private static AmazonS3 createClient(Context ctx) {
        AmazonS3ClientBuilder clientBuilder = AmazonS3ClientBuilder.standard();

        boolean pathStyleAccess = getBoolean(ctx, Constants.PATH_STYLE_ACCESS_KEY, false);
        if (pathStyleAccess) {
            clientBuilder.enablePathStyleAccess();
        }

        configureEndpoint(clientBuilder, ctx);
        configureCredentials(clientBuilder, ctx);

        return clientBuilder.build();
    }

    private static void configureEndpoint(AmazonS3ClientBuilder b, Context ctx) {
        String region = assertString(ctx, Constants.REGION_KEY);

        String endpoint = getString(ctx, Constants.ENDPOINT_KEY);
        if (endpoint != null) {
            b.withEndpointConfiguration(new EndpointConfiguration(endpoint, region));
        } else {
            b.withRegion(region);
        }
    }

    private static void configureCredentials(AmazonS3ClientBuilder b, Context ctx) {
        Map<String, Object> m = getMap(ctx, Constants.AUTH_KEY);
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

    public enum Action {
        PUTOBJECT,
        GETOBJECT
    }
}
