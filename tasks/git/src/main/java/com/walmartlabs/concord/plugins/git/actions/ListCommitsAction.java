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

import com.walmartlabs.concord.common.ConfigurationUtils;
import com.walmartlabs.concord.plugins.git.GitHubTaskAction;
import com.walmartlabs.concord.plugins.git.GitHubTaskParams;
import com.walmartlabs.concord.plugins.git.client.GitHubClient;
import com.walmartlabs.concord.plugins.git.model.GitHubApiInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

public class ListCommitsAction extends GitHubTaskAction<GitHubTaskParams.ListCommits> {

    private final static Logger log = LoggerFactory.getLogger(ListCommitsAction.class);

    @Override
    public Map<String, Object> execute(UUID txId, GitHubApiInfo apiInfo, boolean dryRunMode, GitHubTaskParams.ListCommits input) {
        if (input.toSha().equals(input.fromSha())) {
            return Map.of(
                    "commits", List.of(),
                    "filterMatches", List.of(),
                    "searchDepthReached", false);
        }

        var client = new GitHubClient(txId, apiInfo);
        try {
            var parameters = new HashMap<String, String>();
            if (input.shaOrBranch() != null) {
                parameters.put("sha", input.shaOrBranch());
            }
            if (input.since() != null) {
                parameters.put("since", input.since());
            }

            var resultCommits = new ArrayList<Map<String, Object>>();
            var filterMatches = new ArrayList<String>();

            var counted = new AtomicInteger(0);
            var started = new AtomicBoolean(false);
            var shouldStop = new AtomicBoolean(false);

            client.forEachPage("/repos/" + input.org() + "/" + input.repo() + "/commits", parameters, input.pageSize(), commits -> {
                for (var commit : commits) {
                    var sha = commit.get("sha");

                    if (input.toSha().equals(sha)) {
                        started.set(true);
                    }
                    if (input.fromSha().equals(sha) || counted.get() >= input.searchDepth()) {
                        shouldStop.set(true);
                        break;
                    }

                    if (started.get()) {
                        processCommit(commit, input.filter(), resultCommits, filterMatches);
                    }
                    counted.incrementAndGet();
                }

                return !shouldStop.get();
            });

            log.info("✅ Loaded {} commits in '{}/{}' since '{}': ('{}', '{}']",
                    resultCommits.size(), input.org(), input.repo(), input.since(), input.fromSha(), input.toSha());

            return Map.of(
                    "commits", resultCommits,
                    "filterMatches", filterMatches,
                    "searchDepthReached", counted.get() >= input.searchDepth());
        } catch (Exception e) {
            log.error("❌ Failed to list commits '{}/{}' since '{}'", input.org(), input.repo(), input.since(), e);
            throw new RuntimeException(" Failed to list commits: "  + e.getMessage());
        }
    }

    private static void processCommit(Map<String, Object> commit, Pattern filter, List<Map<String, Object>> commits, List<String> filterMatches) {
        if (filter == null) {
            commits.add(commit);
            return;
        }

        var message = (String)ConfigurationUtils.get(commit, "commit", "message");
        if (message == null) {
            return;
        }

        var matcher = filter.matcher(message);
        if (matcher.matches()) {
            commits.add(commit);
            if (matcher.groupCount() > 0) {
                var match = matcher.group(1);
                if (match != null) {
                    filterMatches.add(match.trim());
                }
            }
        }
    }
}
