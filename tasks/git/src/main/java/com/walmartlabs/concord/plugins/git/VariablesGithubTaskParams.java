package com.walmartlabs.concord.plugins.git;

import com.walmartlabs.concord.runtime.v2.sdk.MapBackedVariables;
import com.walmartlabs.concord.runtime.v2.sdk.Variables;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static com.walmartlabs.concord.plugins.git.GitHubTaskParams.*;

public final class VariablesGithubTaskParams {

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
