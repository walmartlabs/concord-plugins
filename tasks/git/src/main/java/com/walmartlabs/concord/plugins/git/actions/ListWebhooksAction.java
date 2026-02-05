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

public class ListWebhooksAction extends GitHubTaskAction<GitHubTaskParams.ListWebhooks> {

    private final static Logger log = LoggerFactory.getLogger(ListWebhooksAction.class);

    @Override
    public Map<String, Object> execute(UUID txId, GitHubApiInfo apiInfo, boolean dryRunMode, GitHubTaskParams.ListWebhooks input) {
        var client = new GitHubClient(txId, apiInfo);
        try {
            var webhooks = client.singleArrayResult("GET", "/repos/" + input.org() + "/" + input.repo() + "/hooks", null);

            log.info("✅ Found {} webhooks in '{}/{}'", webhooks.size(), input.org(), input.repo());

            return Map.of("webhooks", webhooks);
        } catch (Exception e) {
            log.error("❌ Error while listing webhooks in '{}/{}': {}", input.org(), input.repo(), e.getMessage());
            throw new RuntimeException("Failed to list webhooks: " + e.getMessage());
        }
    }
}
