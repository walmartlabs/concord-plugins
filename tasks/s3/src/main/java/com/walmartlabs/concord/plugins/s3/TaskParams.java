package com.walmartlabs.concord.plugins.s3;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2020 Walmart Inc., Concord Authors
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

import com.walmartlabs.concord.runtime.v2.sdk.MapBackedVariables;
import com.walmartlabs.concord.runtime.v2.sdk.Variables;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class TaskParams {

    public static TaskParams of(Variables input, Map<String, Object> defaults) {
        Variables variables = merge(input, defaults);

        Action action = new TaskParams(variables).action();
        switch (action) {
            case PUTOBJECT: {
                return new PutObjectParams(variables);
            }
            case GETOBJECT: {
                return new GetObjectParams(variables);
            }
            default:
                throw new IllegalArgumentException("Unsupported action type: " + action);
        }
    }

    public static final String ACTION_KEY = "action";
    public static final String IGNORE_ERRORS_KEY = "ignoreErrors";
    public static final String PATH_STYLE_ACCESS_KEY = "pathStyleAccess";
    public static final String REGION_KEY = "region";
    public static final String ENDPOINT_KEY = "endpoint";
    public static final String AUTH_KEY = "auth";

    protected final Variables variables;

    public TaskParams(Variables variables) {
        this.variables = variables;
    }

    public Action action() {
        String action = variables.assertString(ACTION_KEY);
        try {
            return Action.valueOf(action.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Unknown action: '" + action + "'. Available actions: " + Arrays.toString(Action.values()));
        }
    }

    public boolean ignoreErrors() {
        return variables.getBoolean(IGNORE_ERRORS_KEY, false);
    }

    public boolean pathStyleAccess() {
        return variables.getBoolean(PATH_STYLE_ACCESS_KEY, false);
    }

    public String region() {
        return variables.assertString(REGION_KEY);
    }

    public String endpoint() {
        return variables.getString(ENDPOINT_KEY);
    }

    public Map<String, Object> auth() {
        return variables.getMap(AUTH_KEY, Collections.emptyMap());
    }

    public static class GetObjectParams extends TaskParams {

        public static final String OBJECT_KEY = "objectKey";
        public static final String DEST_KEY = "dest";
        public static final String BUCKET_NAME_KEY = "bucketName";

        public GetObjectParams(Variables variables) {
            super(variables);
        }

        public String key() {
            return variables.assertString(OBJECT_KEY);
        }

        public String dst(String defaultValue) {
            return variables.getString(DEST_KEY, defaultValue);
        }

        public String bucketName() {
            return variables.assertString(BUCKET_NAME_KEY);
        }
    }

    public static class PutObjectParams extends TaskParams {

        public static final String AUTO_CREATE_BUCKET_KEY = "autoCreateBucket";
        public static final String SRC_KEY = "src";
        public static final String BUCKET_NAME_KEY = "bucketName";
        public static final String OBJECT_KEY = "objectKey";

        public PutObjectParams(Variables variables) {
            super(variables);
        }

        public String src() {
            return variables.assertString(SRC_KEY);
        }

        public String bucketName() {
            return variables.assertString(BUCKET_NAME_KEY);
        }

        public String key() {
            return variables.assertString(OBJECT_KEY);
        }

        public boolean autoCreateBucket() {
            return variables.getBoolean(AUTO_CREATE_BUCKET_KEY, false);
        }
    }

    private static Variables merge(Variables variables, Map<String, Object> defaults) {
        Map<String, Object> variablesMap = new HashMap<>(defaults != null ? defaults : Collections.emptyMap());
        variablesMap.putAll(variables.toMap());
        return new MapBackedVariables(variablesMap);
    }

    public enum Action {
        PUTOBJECT,
        GETOBJECT
    }
}
