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

// https://docs.github.com/en/rest/git/refs?apiVersion=2022-11-28#get-a-reference
public class GetRefAction extends GitHubTaskAction<GitHubTaskParams.GetRef> {

    private final static Logger log = LoggerFactory.getLogger(GetRefAction.class);

    @Override
    public Map<String, Object> execute(UUID txId, GitHubApiInfo apiInfo, boolean dryRunMode, GitHubTaskParams.GetRef input) {
        var client = new GitHubClient(txId, apiInfo);
        try {
            var ref = input.ref();

            log.info("Getting reference '{}' for '{}/{}'", ref, input.org(), input.repo());

            var refObject = client.singleObjectResult("GET", "/repos/" + input.org() + "/" + input.repo() + "/git/ref/" + ref, null);
            return Map.of("ref", refObject);
        } catch (Exception e) {
            if (e instanceof GitHubApiException gae && gae.getStatusCode() == 404 && !input.failIfNotFound()) {
                return Map.of();
            }
            throw new RuntimeException("Error while getting reference '" + input.ref() + "' for '" +
                    input.org() + "/" + input.repo() + "': " + e.getMessage());
        }
    }
}
