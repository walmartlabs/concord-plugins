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

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class JsonPathTaskCommon {

    private static final String RESULT_KEY = "result";

    private final Path workDir;

    public JsonPathTaskCommon() {
        this.workDir = null;
    }

    public JsonPathTaskCommon(Path workDir) {
        this.workDir = workDir;
    }

    public Object read(Object v, String jsonPath) {
        return jsonPath().parse(v).read(jsonPath);
    }

    public Object readJson(String s, String jsonPath) {
        return jsonPath().parse(s).read(jsonPath);
    }

    public Object readFile(Object v, String jsonPath) throws IOException {
        if (v instanceof String) {
            Path p = assertRelative(Paths.get((String) v));
            File jsonFile = workDir.resolve(p).toFile();
            return jsonPath().parse(jsonFile).read(jsonPath);
        } else if (v instanceof File) {
            return jsonPath().parse((File) v).read(jsonPath);
        } else if (v instanceof Path) {
            Path p = assertRelative((Path) v);
            File jsonFile = workDir.resolve(p).toFile();
            return jsonPath().parse(jsonFile).read(jsonPath);
        } else {
            throw new IllegalArgumentException("Expected a path to a JSON file, got: " + v);
        }
    }

    public Object execute(TaskParams in) throws Exception {
        Object result;
        switch (in.action()) {
            case READ: {
                result = read(in.src(), in.jsonPath());
                break;
            }
            case READFILE: {
                result = readFile(in.src(), in.jsonPath());
                break;
            }
            case READJSON: {
                result = readJson((String) in.src(), in.jsonPath());
                break;
            }
            default:
                throw new IllegalArgumentException("Unsupported action: " + in.action());
        }

        return result;
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
}
