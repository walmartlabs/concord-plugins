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

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

// https://docs.github.com/en/rest/repos/repos?apiVersion=2022-11-28#create-a-repository-dispatch-event
public class CreateRepositoryDispatchAction extends GitHubTaskAction<GitHubTaskParams.CreateRepositoryDispatch> {

    private final static Logger log = LoggerFactory.getLogger(CreateRepositoryDispatchAction.class);

    @Override
    public Map<String, Object> execute(UUID txId, GitHubApiInfo apiInfo, boolean dryRunMode, GitHubTaskParams.CreateRepositoryDispatch input) {
        if (dryRunMode) {
            log.info("Dry-run mode enabled: Skipping repository dispatch event creation");
            return Collections.emptyMap();
        }

        var client = new GitHubClient(txId, apiInfo);
        try {
            var body = Map.of(
                    "event_type", input.eventType(),
                    "client_payload", input.clientPayload()
            );

            client.voidResult("POST", "/repos/" + input.org() + "/" + input.repo() + "/dispatches", body);

            log.info("Repository dispatch event created in {}/{} with event type '{}'",
                    input.org(), input.repo(), input.eventType());

            return Map.of();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create repository dispatch event: " + e.getMessage());
        }
    }
}
