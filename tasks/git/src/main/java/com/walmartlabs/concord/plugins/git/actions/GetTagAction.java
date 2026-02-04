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
import com.walmartlabs.concord.sdk.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;

// https://docs.github.com/en/rest/git/tags?apiVersion=2022-11-28#get-a-tag
public class GetTagAction extends GitHubTaskAction<GitHubTaskParams.GetTag> {

    private final static Logger log = LoggerFactory.getLogger(GetTagAction.class);

    @Override
    public Map<String, Object> execute(UUID txId, GitHubApiInfo apiInfo, boolean dryRunMode, GitHubTaskParams.GetTag input) {
        if (input.tagSha() == null && input.tagName() == null) {
            throw new IllegalArgumentException("Either 'tagSha' or 'tagName' must be provided");
        }

        var client = new GitHubClient(txId, apiInfo);
        try {
            var targetSha = input.tagSha();
            if (targetSha == null) {
                log.info("Getting tag by name '{}' from {}/{}", input.tagName(), input.org(), input.repo());
                var ref = new GetRefAction().execute(txId, apiInfo, dryRunMode, new GitHubTaskParams.GetRef(input.org(), input.repo(), "tags/" + input.tagName(), input.failIfNotFound()));
                if (ref.isEmpty()) {
                    log.info("Tag '{}' not found in '{}/{}'", input.tagName(), input.org(), input.repo());
                    return Map.of();
                }

                var object = MapUtils.<String, Object>getMap(MapUtils.getMap(ref, "ref", Map.of()), "object", Map.of());
                var type = MapUtils.assertString(object, "type");
                if (!"tag".equals(type)) {
                    throw new UserDefinedException("Not an annotated tag: " + type + ", sha: " + targetSha);
                }
                targetSha = MapUtils.assertString(object, "sha");
            }

            log.info("Getting tag by SHA '{}' from {}/{}", targetSha, input.org(), input.repo());
            var tag = client.singleObjectResult("GET", "/repos/" + input.org() + "/" + input.repo() + "/git/tags/" + targetSha, null);
            return Map.of("tag", tag);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            if (e instanceof GitHubApiException gae && gae.getStatusCode() == 404 && !input.failIfNotFound()) {
                return Map.of();
            }
            var tag = input.tagName() != null ? input.tagName() : input.tagSha();
            throw new RuntimeException("Error while getting tag '" + tag + "' for '" +
                    input.org() + "/" + input.repo() + "': " + e.getMessage());
        }
    }
}
