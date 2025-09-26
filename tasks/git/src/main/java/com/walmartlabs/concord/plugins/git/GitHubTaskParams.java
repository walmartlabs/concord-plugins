package com.walmartlabs.concord.plugins.git;

public sealed interface GitHubTaskParams {

    record GetShortCommitSha(
            String org,
            String repo,
            String sha,
            int minLength
    ) implements GitHubTaskParams {
    }
}
