package com.walmartlabs.concord.plugins.git;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2025 Walmart Inc., Concord Authors
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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static com.walmartlabs.concord.plugins.git.GitHubTaskParams.*;

public final class VariablesGithubTaskParams {

    public enum Action {
        CREATEPR,
        COMMENTPR,
        MERGEPR,
        CLOSEPR,
        GETPRCOMMITLIST,
        MERGE,
        CREATEISSUE,
        CREATETAG,
        CREATEHOOK,
        DELETETAG,
        DELETEBRANCH,
        GETCOMMIT,
        ADDSTATUS,
        GETSTATUSES,
        FORKREPO,
        GETBRANCHLIST,
        GETPR,
        GETPRLIST,
        GETPRFILES,
        GETTAGLIST,
        GETLATESTSHA,
        CREATEREPO,
        DELETEREPO,
        GETCONTENT,
        CREATEAPPTOKEN,
        GETSHORTSHA
    }

    public static Variables merge(Map<String, Object> taskDefaults, Map<String, Object> input) {
        var merged = new HashMap<String, Object>();
        Stream.of(taskDefaults, input)
                .filter(Objects::nonNull)
                .forEach(merged::putAll);

        return new MapBackedVariables(merged);
    }

    public static GetShortCommitSha getShortSha(Variables variables) {
        return new GetShortCommitSha(
                assertOrg(variables),
                assertRepo(variables),
                variables.assertString("sha"),
                variables.getInt("minLength", 7));
    }

    private static String assertOrg(Variables variables) {
        return variables.assertString("org");
    }

    private static String assertRepo(Variables variables) {
        return variables.assertString("repo");
    }
}
