package com.walmartlabs.concord.plugins.s3;

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

import com.adobe.testing.s3mock.testcontainers.S3MockContainer;
import com.walmartlabs.concord.plugins.s3.v2.S3TaskV2;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.MapBackedVariables;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.walmartlabs.concord.plugins.s3.TaskParams.ACTION_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@Testcontainers
class S3TaskV2Test {
    private static final String TEST_BUCKET = "my-bucket";

    // Container will be started before each test method and stopped after
    @Container
    private final S3MockContainer s3Mock =
            new S3MockContainer(System.getenv("S3_MOCK_IMAGE_VERSION"))
                    .withInitialBuckets(TEST_BUCKET);

    @Test
    void localMockTestV2() throws Exception {
        String data = "Hello!";

        Path p = Files.createTempFile("test", ".txt");
        Files.write(p, data.getBytes());

        Map<String, Object> auth = new HashMap<>();
        auth.put("accessKey", "my-access-key");
        auth.put("secretKey", "my-secret-key");

        Map<String, Object> args = new HashMap<>();
        args.put(TaskParams.PutObjectParams.BUCKET_NAME_KEY, TEST_BUCKET);
        args.put(TaskParams.PutObjectParams.OBJECT_KEY, "xyz");
        args.put(TaskParams.PutObjectParams.SRC_KEY, p.getFileName().toString());
        args.put(TaskParams.PutObjectParams.ENDPOINT_KEY, s3Mock.getHttpEndpoint());
        args.put(TaskParams.PutObjectParams.REGION_KEY, "us-west-2");
        args.put(TaskParams.PutObjectParams.PATH_STYLE_ACCESS_KEY, true);
        args.put(TaskParams.PutObjectParams.AUTH_KEY, Collections.singletonMap("basic", auth));

        Context context = Mockito.mock(Context.class);
        
        when(context.variables()).thenReturn(new MapBackedVariables(Collections.emptyMap()));
        when(context.defaultVariables()).thenReturn(new MapBackedVariables(Collections.emptyMap()));
        when(context.workingDirectory()).thenReturn(p.getParent().toAbsolutePath());

        // --- put object

        args.put(ACTION_KEY, TaskParams.Action.PUTOBJECT.name());
        S3TaskV2 t = new S3TaskV2(context);

        TaskResult.SimpleResult result = (TaskResult.SimpleResult) t.execute(new MapBackedVariables(args));

        assertTrue(result.ok());

        // --- get object

        args.put(Constants.ACTION_KEY, TaskParams.Action.GETOBJECT.name());
        result = (TaskResult.SimpleResult) t.execute(new MapBackedVariables(args));

        assertTrue(result.ok());

        String storedObject = (String) result.values().get("path");
        String s = new String(Files.readAllBytes(p.getParent().resolve(storedObject)));
        assertEquals(data, s);
    }
}
