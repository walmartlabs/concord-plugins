package com.walmartlabs.concord.plugins.jsonpath;

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

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.ParseContext;
import com.walmartlabs.concord.sdk.Context;
import com.walmartlabs.concord.sdk.ContextUtils;
import com.walmartlabs.concord.sdk.InjectVariable;
import com.walmartlabs.concord.sdk.Task;

import javax.inject.Named;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Named("jsonPath")
public class JsonPathTask implements Task {

    private static final String ACTION_KEY = "action";
    private static final String SRC_KEY = "src";
    private static final String PATH_KEY = "path";
    private static final String RESULT_KEY = "result";

    public Object read(Object v, String jsonPath) {
        return jsonPath().parse(v).read(jsonPath);
    }

    public Object readJson(String s, String jsonPath) {
        return jsonPath().parse(s).read(jsonPath);
    }

    public Object readFile(@InjectVariable("context") Context ctx, Object v, String jsonPath) throws IOException {
        Path baseDir = ContextUtils.getWorkDir(ctx);

        if (v instanceof String) {
            Path p = assertRelative(Paths.get((String) v));
            File jsonFile = baseDir.resolve(p).toFile();
            return jsonPath().parse(jsonFile).read(jsonPath);
        } else if (v instanceof File) {
            return jsonPath().parse((File) v).read(jsonPath);
        } else if (v instanceof Path) {
            Path p = assertRelative((Path) v);
            File jsonFile = baseDir.resolve(p).toFile();
            return jsonPath().parse(jsonFile).read(jsonPath);
        } else {
            throw new IllegalArgumentException("Expected a path to a JSON file, got: " + v);
        }
    }

    @Override
    public void execute(Context ctx) throws Exception {
        Action action = getAction(ctx);
        String jsonPath = ContextUtils.assertString(ctx, PATH_KEY);

        Object src = ctx.getVariable(SRC_KEY);
        Object result;

        switch (action) {
            case READ: {
                result = read(src, jsonPath);
                break;
            }
            case READFILE: {
                result = readFile(ctx, src, jsonPath);
                break;
            }
            case READJSON: {
                result = readJson((String) src, jsonPath);
                break;
            }
            default:
                throw new IllegalArgumentException("Unsupported action: " + action);
        }

        ctx.setVariable(RESULT_KEY, result);
    }

    private static Path assertRelative(Path p) {
        if (p.isAbsolute()) {
            throw new IllegalArgumentException("Expected a relative file path, got: " + p);
        }

        return p;
    }

    private static ParseContext jsonPath() {
        Configuration cfg = Configuration.defaultConfiguration()
                .addOptions(Option.DEFAULT_PATH_LEAF_TO_NULL);

        return JsonPath.using(cfg);
    }

    private static Action getAction(Context ctx) {
        Object v = ctx.getVariable(ACTION_KEY);
        if (!(v instanceof String)) {
            throw new IllegalArgumentException("Expected an action, got: " + v);
        }

        return Action.valueOf(((String) v).toUpperCase());
    }

    public enum Action {
        READ,
        READJSON,
        READFILE
    }
}
