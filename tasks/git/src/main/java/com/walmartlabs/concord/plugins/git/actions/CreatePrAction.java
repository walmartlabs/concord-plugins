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
import com.walmartlabs.concord.plugins.git.client.GitHubClient;
import com.walmartlabs.concord.plugins.git.model.GitHubApiInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;

// https://docs.github.com/en/rest/pulls/pulls?apiVersion=2022-11-28#create-a-pull-request
public class CreatePrAction extends GitHubTaskAction<GitHubTaskParams.CreatePr> {

    private final static Logger log = LoggerFactory.getLogger(CreatePrAction.class);

    @Override
    public Map<String, Object> execute(UUID txId, GitHubApiInfo apiInfo, boolean dryRunMode, GitHubTaskParams.CreatePr input) {
        if (dryRunMode) {
            log.info("Dry-run mode enabled: Skipping PR creation");
            return Map.of("prId", 0); // let's return some `fake` ID
        }

        log.info("Creating PR in {}/{} from {} to {}",
                input.org(), input.repo(), input.srcBranch(), input.destBranch());

        var client = new GitHubClient(txId, apiInfo);
        try {
            var body = Map.of(
                    "title", input.title(),
                    "body", input.body(),
                    "head", input.srcBranch(),
                    "base", input.destBranch()
            );

            var result = client.singleObjectResult("POST", "/repos/" + input.org() + "/" + input.repo() + "/pulls", body);
            var prId = result.get("number");

            log.info("✅ PR Created, id: '{}'", prId);

            if (!input.labels().isEmpty()) {
                client.singleArrayResult("POST", "/repos/" + input.org() + "/" + input.repo() + "/issues/" + prId + "/labels",
                        Map.of("labels", input.labels()));
            }

            return Map.of("prId", prId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create pull request: " + e.getMessage());
        }
    }
}
