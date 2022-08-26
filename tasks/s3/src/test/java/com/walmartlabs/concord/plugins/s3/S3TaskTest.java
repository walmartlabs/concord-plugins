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

import com.walmartlabs.concord.sdk.MockContext;
import com.walmartlabs.concord.sdk.Task;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.walmartlabs.concord.plugins.s3.TaskParams.ACTION_KEY;
import static com.walmartlabs.concord.plugins.s3.TaskParams.PutObjectParams;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Disabled
public class S3TaskTest {

    @Test
    @SuppressWarnings("unchecked")
    public void test() throws Exception {
        String data = "Hello!";

        Path p = Files.createTempFile("test", ".txt");
        Files.write(p, data.getBytes());

        Map<String, Object> auth = new HashMap<>();
        auth.put("accessKey", System.getenv("AWS_ACCESS_KEY"));
        auth.put("secretKey", System.getenv("AWS_SECRET_KEY"));

        Map<String, Object> args = new HashMap<>();
        args.put(com.walmartlabs.concord.sdk.Constants.Context.WORK_DIR_KEY, p.getParent().toAbsolutePath().toString());
        args.put(ACTION_KEY, TaskParams.Action.PUTOBJECT.name());
        args.put(PutObjectParams.BUCKET_NAME_KEY, "ibodrov-test");
        args.put(PutObjectParams.OBJECT_KEY, "xyz");
        args.put(PutObjectParams.SRC_KEY, p.getFileName().toString());
        args.put(PutObjectParams.ENDPOINT_KEY, System.getenv("AWS_ENDPOINT"));
        args.put(PutObjectParams.REGION_KEY, System.getenv("AWS_REGION"));
        args.put(PutObjectParams.PATH_STYLE_ACCESS_KEY, true);
        args.put(PutObjectParams.AUTH_KEY, Collections.singletonMap("basic", auth));

        MockContext ctx = new MockContext(args);

        Task t = new S3Task();
        t.execute(ctx);

        Map<String, Object> r = (Map<String, Object>) ctx.getVariable(Constants.RESULT_KEY);
        assertTrue((Boolean) r.get("ok"));

        // ---

        args.put(Constants.ACTION_KEY, TaskParams.Action.GETOBJECT.name());

        ctx = new MockContext(args);
        t.execute(ctx);

        r = (Map<String, Object>) ctx.getVariable(Constants.RESULT_KEY);
        assertTrue((Boolean) r.get("ok"));

        String storedObject = (String) r.get("path");
        String s = new String(Files.readAllBytes(p.getParent().resolve(storedObject)));
        assertEquals(data, s);
    }
}
