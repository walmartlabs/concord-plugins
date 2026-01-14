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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;

// https://docs.github.com/en/rest/git/refs?apiVersion=2022-11-28#create-a-reference
public class CreateBranchAction extends GitHubTaskAction<GitHubTaskParams.CreateBranch> {

    private final static Logger log = LoggerFactory.getLogger(CreateBranchAction.class);

    @Override
    public Map<String, Object> execute(UUID txId, GitHubApiInfo apiInfo, boolean dryRunMode, GitHubTaskParams.CreateBranch input) {
        if (dryRunMode) {
            log.info("Dry-run mode enabled: Skipping branch creation");
            return Map.of();
        }

        var client = new GitHubClient(txId, apiInfo);
        try {
            var exists = branchExists(client, input.org(), input.repo(), input.branchName());
            if (exists) {
                return Map.of("alreadyExists", true);
            }

            var body = Map.of("ref", "refs/heads/" + input.branchName(),
                    "sha", input.sha());

            client.singleObjectResult("POST", "/repos/" + input.org() + "/" + input.repo() + "/git/refs", body);
            return Map.of();
        } catch (Exception e) {
           throw new RuntimeException("Error while creating new branch '" + input.branchName() + "' for sha '" +
                   input.sha() + " in '" + input.org() + "/" + input.repo() + "': " + e.getMessage());
        }
    }

    private static boolean branchExists(GitHubClient client, String owner, String repo, String branch) throws Exception {
        try {
            client.singleObjectResult(
                    "GET",
                    "/repos/" + owner + "/" + repo + "/git/ref/heads/" + branch,
                    null
            );
            return true;
        } catch (GitHubApiException e) {
            if (e.getStatusCode() == 404 ) {
                return false;
            }
            throw e;
        }
    }
}
