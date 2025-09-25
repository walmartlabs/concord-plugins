package com.walmartlabs.concord.plugins.git;

public interface GitHubTaskParams {

    record GetShortCommitSha(
            String org,
            String repo,
            String sha,
            int minLength
    ) implements GitHubTaskParams {
    }
}
