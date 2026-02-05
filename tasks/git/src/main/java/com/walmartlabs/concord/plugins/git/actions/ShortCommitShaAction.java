package com.walmartlabs.concord.plugins.git.actions;

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

import com.walmartlabs.concord.plugins.git.GitHubTaskAction;
import com.walmartlabs.concord.plugins.git.GitHubTaskParams;
import com.walmartlabs.concord.plugins.git.client.GitHubApiException;
import com.walmartlabs.concord.plugins.git.client.GitHubClient;
import com.walmartlabs.concord.plugins.git.model.GitHubApiInfo;
import com.walmartlabs.concord.runtime.v2.sdk.UserDefinedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;

public class ShortCommitShaAction extends GitHubTaskAction<GitHubTaskParams.GetShortCommitSha> {

    private final static Logger log = LoggerFactory.getLogger(ShortCommitShaAction.class);

    @Override
    public Map<String, Object> execute(UUID txId, GitHubApiInfo apiInfo, boolean dryRunMode, GitHubTaskParams.GetShortCommitSha input) {
        int minLen = input.minLength() > 0 ? input.minLength() : 7;

        var client = new GitHubClient(txId, apiInfo);
        try {
            var commit = getCommit(client, input.org(), input.repo(), input.sha());
            if (commit == null) {
                log.error("❌ Commit '{}' not found in '{}/{}'", input.sha(), input.org(), input.repo());
                throw new UserDefinedException("Commit not found in '" + input.org() + "/" + input.repo() + "'");
            }

            for (int len = minLen; len <= 40; len++) {
                var prefix = input.sha().substring(0, len);

                commit = getCommit(client, input.org(), input.repo(), prefix);
                if (commit != null && input.sha().equals(commit.get("sha"))) {

                    log.info("✅ Short SHA for '{}' commit is '{}' in '{}/{}'",
                            input.sha(), prefix, input.org(), input.repo());

                    return Map.of("shortSha", prefix);
                }
            }

            log.error("❌ Could not derive unique short SHA for '{}' commit in '{}/{}'",
                    input.sha(), input.org(), input.repo());

            throw new UserDefinedException("Could not derive unique short SHA for '" + input.sha() + "' commit");
        } catch (UserDefinedException e) {
            throw e;
        } catch (Exception e) {
            log.error("❌ Error while getting short SHA for '{}' commit in '{}/{}': {}",
                    input.sha(), input.org(), input.repo(), e.getMessage());

           throw new RuntimeException("Failed to get short SHA for '" + input.sha() + "' commit: "  + e.getMessage());
        }
    }

    private static Map<String, Object> getCommit(GitHubClient client, String owner, String repo, String sha) throws Exception{
        try {
            var c = client.singleObjectResult("GET", "/repos/" + owner + "/" + repo + "/commits/" + sha, null);
            return c == null || c.isEmpty() ? null : c;
        } catch (GitHubApiException e) {
            if (e.getStatusCode() == 404 || e.getStatusCode() == 422) {
                return null;
            }
            throw e;
        }
    }
}
