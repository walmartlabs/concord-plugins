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

import java.util.Set;
import java.util.regex.Pattern;

public sealed interface GitHubTaskParams {

    record GetShortCommitSha(
            String org,
            String repo,
            String sha,
            int minLength
    ) implements GitHubTaskParams {
    }

    record ListCommits(
            String org,
            String repo,
            String shaOrBranch,
            String since,
            String fromSha,
            String toSha,
            int pageSize,
            int searchDepth,
            Pattern filter
    ) implements GitHubTaskParams {
    }

    record CreateBranch(
            String org,
            String repo,
            String branchName,
            String sha
    ) implements GitHubTaskParams {
    }

    record CreatePr(
            String org,
            String repo,
            String title,
            String body,
            String destBranch,
            String srcBranch,
            Set<String> labels
    ) implements GitHubTaskParams {
    }

    record GetRef(
            String org,
            String repo,
            String ref,
            boolean failIfNotFound
    ) implements GitHubTaskParams {
    }

    record GetTag(
            String org,
            String repo,
            String tagSha,
            String tagName,
            boolean failIfNotFound
    ) implements GitHubTaskParams {
    }

    record CreateRepositoryDispatch(
            String org,
            String repo,
            String eventType,
            java.util.Map<String, Object> clientPayload
    ) implements GitHubTaskParams {
    }

    record ListWebhooks(
            String org,
            String repo
    ) implements GitHubTaskParams {
    }
}
