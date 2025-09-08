package com.walmartlabs.concord.plugins.git;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2025 Wal-Mart Store, Inc.
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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.walmartlabs.concord.sdk.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;

import static com.walmartlabs.concord.plugins.git.Utils.getUrl;
import static com.walmartlabs.concord.sdk.MapUtils.*;

public class GitHubTask {

    private static final Logger log = LoggerFactory.getLogger(GitHubTask.class);

    private static final String API_URL_KEY = "apiUrl";
    private static final String ACTION_KEY = "action";
    private static final String GITHUB_ACCESSTOKEN = "accessToken";
    private static final String GITHUB_ORGNAME = "org";
    private static final String GITHUB_REPONAME = "repo";
    private static final String GITHUB_BRANCH = "branch";
    private static final String GITHUB_PRNUMBER = "prNumber";
    private static final String GITHUB_PRTITLE = "prTitle";
    private static final String GITHUB_PRBODY = "prBody";
    private static final String GITHUB_PRCOMMENT = "prComment";
    private static final String GITHUB_PRBASE = "prDestinationBranch";
    private static final String GITHUB_PRHEAD = "prSourceBranch";
    private static final String GITHUB_PRID = "prId";
    private static final String GITHUB_TAGVERSION = "tagVersion";
    private static final String GITHUB_TAGMESSAGE = "tagMessage";
    private static final String GITHUB_TAGGERUID = "tagAuthorName";
    private static final String GITHUB_TAGGEREMAIL = "tagAuthorEmail";
    private static final String GITHUB_TAGNAME = "tagName";
    private static final String GITHUB_COMMIT_SHA = "commitSHA";
    private static final String GITHUB_TAG_REFS = "refs/tags/";
    private static final String GITHUB_MERGEHEAD = "head";
    private static final String GITHUB_MERGEBASE = "base";
    private static final String GITHUB_MERGECOMMITMSG = "commitMessage";
    private static final String GITHUB_MERGE_METHOD = "mergeMethod";
    private static final String GITHUB_FORKTARGETORG = "targetOrg";
    private static final String GITHUB_PATH = "path";
    private static final String GITHUB_REF = "ref";
    private static final String GITHUB_HOOK_URL = "url";
    private static final String GITHUB_HOOK_EVENTS = "events";
    private static final String GITHUB_HOOK_CFG = "config";
    private static final String GITHUB_HOOK_CONTENT_TYPE = "contentType";
    private static final String GITHUB_HOOK_SECRET = "secret";
    private static final String GITHUB_HOOK_INSECURE_SSL = "insecureSsl";
    private static final String GITHUB_HOOK_REPLACE = "replace";

    private static final String STATUS_CHECK_STATE = "state";
    private static final String STATUS_CHECK_TARGET_URL = "targetUrl";
    private static final String STATUS_CHECK_DESCRIPTION = "description";
    private static final String STATUS_CHECK_CONTEXT = "context";
    private static final String GITHUB_PR_STATE = "state";

    private static final String ISSUE_BODY = "body";
    private static final String ISSUE_TITLE = "title";
    private static final String ISSUE_ASSIGNEE = "assignee";
    private static final String ISSUE_LABELS = "labels";

    private static final List<String> GITHUB_VALID_PR_STATES = Arrays.asList("all", "open", "closed");
    private static final Set<String> STATUS_CHECK_STATES = new HashSet<>(Arrays.asList("error", "failure", "pending", "success"));

    private static final int STATUS_GITHUB_REPO_NOT_FOUND = 404;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final TypeReference<Map<String, Object>> OBJECT_TYPE = new TypeReference<Map<String, Object>>() {
    };

    private static final TypeReference<List<Map<String, Object>>> LIST_OF_OBJECT_TYPE = new TypeReference<List<Map<String, Object>>>() {
    };

    private final boolean dryRunMode;
    private final HttpClient httpClient;

    public GitHubTask() {
        this(false);
    }

    public GitHubTask(boolean dryRunMode) {
        this.dryRunMode = dryRunMode;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    public Map<String, Object> execute(Map<String, Object> in, Map<String, Object> defaults) {
        Action action = getAction(in);
        String gitHubUri = getUrl(defaults, in, API_URL_KEY);
        log.info("Starting '{}' action...", action);
        log.info("Using GitHub apiUrl {}", gitHubUri);

        return switch (action) {
            case CREATEPR -> createPR(in, gitHubUri);
            case COMMENTPR -> commentPR(in, gitHubUri);
            case GETPRCOMMITLIST -> getPRCommitList(in, gitHubUri);
            case MERGEPR -> mergePR(in, gitHubUri);
            case CLOSEPR -> closePR(in, gitHubUri);
            case CREATEISSUE -> createIssue(in, gitHubUri);
            case CREATETAG -> createTag(in, gitHubUri);
            case DELETETAG -> deleteTag(in, gitHubUri);
            case DELETEBRANCH -> deleteBranch(in, gitHubUri);
            case MERGE -> merge(in, gitHubUri);
            case GETCOMMIT -> getCommit(in, gitHubUri);
            case ADDSTATUS -> addStatus(in, gitHubUri);
            case GETSTATUSES -> getStatuses(in, gitHubUri);
            case FORKREPO -> forkRepo(in, gitHubUri);
            case GETBRANCHLIST -> getBranchList(in, gitHubUri);
            case GETPR -> getPR(in, gitHubUri);
            case GETPRLIST -> getPRList(in, gitHubUri);
            case GETTAGLIST -> getTagList(in, gitHubUri);
            case GETLATESTSHA -> getLatestSHA(in, gitHubUri);
            case CREATEREPO -> createRepo(in, gitHubUri);
            case DELETEREPO -> deleteRepo(in, gitHubUri);
            case GETCONTENT -> getContent(in, gitHubUri);
            case CREATEHOOK -> createHook(in, gitHubUri);
            case GETPRFILES -> getPRFiles(in, gitHubUri);
        };
    }

    private Map<String, Object> createPR(Map<String, Object> in, String gitHubUri) {
        String gitHubAccessToken = assertString(in, GITHUB_ACCESSTOKEN);
        String gitHubOrgName = assertString(in, GITHUB_ORGNAME);
        String gitHubRepoName = assertString(in, GITHUB_REPONAME);
        String gitHubPRTitle = assertString(in, GITHUB_PRTITLE);
        String gitHubPRBody = assertString(in, GITHUB_PRBODY);
        String gitHubPRHead = assertString(in, GITHUB_PRHEAD);
        String gitHubPRBase = assertString(in, GITHUB_PRBASE);

        log.info("Creating PR in {}/{} from {} to {}", gitHubOrgName, gitHubRepoName, gitHubPRHead, gitHubPRBase);

        Map<String, Object> pr = new HashMap<>();
        pr.put("title", gitHubPRTitle);
        pr.put("body", gitHubPRBody);
        pr.put("head", gitHubPRHead);
        pr.put("base", gitHubPRBase);

        if (dryRunMode) {
            log.info("Dry-run mode enabled: Skipping PR creation");
            return Map.of("prId", 0);
        }

        try {
            String apiUrl = buildApiUrl(gitHubUri, "/repos/" + gitHubOrgName + "/" + gitHubRepoName + "/pulls");
            Map<String, Object> result = getSingleObject("POST", apiUrl, gitHubAccessToken, pr);
            if (!result.containsKey("number")) {
                throw new IllegalStateException("Expected to receive the 'number' field in the GitHub API response");
            }
            int prNumber = ((Number) result.get("number")).intValue();
            log.info("Created PR# {}", prNumber);
            return Map.of("prId", prNumber);
        } catch (Exception e) {
            throw new RuntimeException("Cannot create a pull request: " + e.getMessage());
        }
    }

    private Map<String, Object> getPRCommitList(Map<String, Object> in, String gitHubUri) {
        String gitHubAccessToken = assertString(in, GITHUB_ACCESSTOKEN);
        String gitHubOrgName = assertString(in, GITHUB_ORGNAME);
        String gitHubRepoName = assertString(in, GITHUB_REPONAME);
        int gitHubPRID = assertInt(in, GITHUB_PRID);

        log.info("Getting PR {} commits for {}/{} repo", gitHubPRID, gitHubOrgName, gitHubRepoName);
        try {
            String apiUrl = buildApiUrl(gitHubUri, "/repos/" + gitHubOrgName + "/" + gitHubRepoName + "/pulls/" + gitHubPRID + "/commits");
            List<Map<String, Object>> commits = getListOfObjects(apiUrl, gitHubAccessToken);
            log.info("Commits count: {}", commits.size());
            return Map.of("commits", commits);
        } catch (Exception e) {
            throw new RuntimeException("Cannot get PR " + gitHubPRID + " commits: " + e.getMessage());
        }
    }

    private Map<String, Object> commentPR(Map<String, Object> in, String gitHubUri) {
        String gitHubAccessToken = assertString(in, GITHUB_ACCESSTOKEN);
        String gitHubOrgName = assertString(in, GITHUB_ORGNAME);
        String gitHubRepoName = assertString(in, GITHUB_REPONAME);
        String gitHubPRComment = assertString(in, GITHUB_PRCOMMENT);
        int gitHubPRID = assertInt(in, GITHUB_PRID);

        if (dryRunMode) {
            log.info("Dry-run mode enabled: Skipping comment on PR #{} in {}/{}", gitHubPRID, gitHubOrgName, gitHubRepoName);
            return Map.of("id", 0);
        }

        log.info("Commenting PR #{} in {}/{}", gitHubPRID, gitHubOrgName, gitHubRepoName);

        try {
            Map<String, Object> comment = new HashMap<>();
            comment.put("body", gitHubPRComment);

            String apiUrl = buildApiUrl(gitHubUri, "/repos/" + gitHubOrgName + "/" + gitHubRepoName + "/issues/" + gitHubPRID + "/comments");
            Map<String, Object> response = getSingleObject("POST", apiUrl, gitHubAccessToken, comment);
            log.info("Commented on PR# {}", gitHubPRID);

            return Map.of("id", ((Number) response.get("id")).longValue());
        } catch (Exception e) {
            throw new RuntimeException("Cannot comment on the pull request: " + e.getMessage());
        }
    }

    private Map<String, Object> mergePR(Map<String, Object> in, String gitHubUri) {
        String gitHubAccessToken = assertString(in, GITHUB_ACCESSTOKEN);
        String gitHubOrgName = assertString(in, GITHUB_ORGNAME);
        String gitHubRepoName = assertString(in, GITHUB_REPONAME);
        int gitHubPRID = assertInt(in, GITHUB_PRID);
        String commitMessage = getString(in, GITHUB_MERGECOMMITMSG, "GitHub PR Merge");
        String mergeMethod = getString(in, GITHUB_MERGE_METHOD);

        if (dryRunMode) {
            log.info("Dry-run mode enabled: Skipping merge in PR #{} in {}/{}", gitHubPRID, gitHubOrgName, gitHubRepoName);
            return Map.of();
        }

        try {
            log.info("Merging PR #{} in {}/{}", gitHubPRID, gitHubOrgName, gitHubRepoName);

            Map<String, Object> body = new HashMap<>();
            body.put("commit_message", commitMessage);
            if (mergeMethod != null) {
                body.put("merge_method", mergeMethod);
            }

            String apiUrl = buildApiUrl(gitHubUri, "/repos/" + gitHubOrgName + "/" + gitHubRepoName + "/pulls/" + gitHubPRID + "/merge");
            Map<String, Object> result = getSingleObject("PUT", apiUrl, gitHubAccessToken, body);
            log.info("Merged PR# {}, sha: {}", gitHubPRID, result.get("sha"));

            return Map.of();
        } catch (Exception e) {
            throw new RuntimeException("Cannot merge the pull request: " + e.getMessage());
        }
    }

    private Map<String, Object> closePR(Map<String, Object> in, String gitHubUri) {
        String gitHubAccessToken = assertString(in, GITHUB_ACCESSTOKEN);
        String gitHubOrgName = assertString(in, GITHUB_ORGNAME);
        String gitHubRepoName = assertString(in, GITHUB_REPONAME);
        int gitHubPRID = assertInt(in, GITHUB_PRID);

        if (dryRunMode) {
            log.info("Dry-run mode enabled: Skipping closing of PR #{} in {}/{}", gitHubPRID, gitHubOrgName, gitHubRepoName);
            return Map.of();
        }

        try {
            log.info("Closing PR #{} in {}/{}", gitHubPRID, gitHubOrgName, gitHubRepoName);

            Map<String, Object> body = new HashMap<>();
            body.put("state", "closed");

            String apiUrl = buildApiUrl(gitHubUri, "/repos/" + gitHubOrgName + "/" + gitHubRepoName + "/pulls/" + gitHubPRID);
            getSingleObject("PATCH", apiUrl, gitHubAccessToken, body);
            log.info("Closed PR# {}", gitHubPRID);

            return Map.of();
        } catch (Exception e) {
            throw new RuntimeException("Cannot close the pull request: " + e.getMessage());
        }
    }

    private Map<String, Object> merge(Map<String, Object> in, String gitHubUri) {
        String gitHubAccessToken = assertString(in, GITHUB_ACCESSTOKEN);
        String gitHubOrgName = assertString(in, GITHUB_ORGNAME);
        String gitHubRepoName = assertString(in, GITHUB_REPONAME);
        String head = assertString(in, GITHUB_MERGEHEAD);
        String base = assertString(in, GITHUB_MERGEBASE);
        String commitMessage = assertString(in, GITHUB_MERGECOMMITMSG);

        if (dryRunMode) {
            log.info("Dry-run mode enabled: Skipping merging of {} in {}/{}", head, gitHubOrgName, gitHubRepoName);
            return Map.of();
        }

        log.info("Merging {} in {}/{}", head, gitHubOrgName, gitHubRepoName);

        try {
            Map<String, Object> params = new HashMap<>();
            params.put("base", base);
            params.put("head", head);
            params.put("commit_message", commitMessage);

            String apiUrl = buildApiUrl(gitHubUri, "/repos/" + gitHubOrgName + "/" + gitHubRepoName + "/merges");
            getSingleObject("POST", apiUrl, gitHubAccessToken, params);

            log.info("Merged '{}' with '{}'", head, base);

            return Map.of();
        } catch (Exception e) {
            throw new RuntimeException("Cannot merge 'base' and 'head': " + e.getMessage());
        }
    }

    private Map<String, Object> getCommit(Map<String, Object> in, String gitHubUri) {
        String gitHubAccessToken = assertString(in, GITHUB_ACCESSTOKEN);
        String gitHubOrgName = assertString(in, GITHUB_ORGNAME);
        String gitHubRepoName = assertString(in, GITHUB_REPONAME);
        String gitHubCommitSha = assertString(in, GITHUB_COMMIT_SHA);

        try {
            log.info("Getting commit '{}' in {}/{}", gitHubCommitSha, gitHubOrgName, gitHubRepoName);

            String apiUrl = buildApiUrl(gitHubUri, "/repos/" + gitHubOrgName + "/" + gitHubRepoName + "/commits/" + gitHubCommitSha);
            Map<String, Object> data = getSingleObject("GET", apiUrl, gitHubAccessToken, null);

            // get default branch
            String repoApiUrl = buildApiUrl(gitHubUri, "/repos/" + gitHubOrgName + "/" + gitHubRepoName);
            Map<String, Object> repoData = getSingleObject("GET", repoApiUrl, gitHubAccessToken, null);
            String defaultBranch = (String) repoData.get("default_branch");

            if (defaultBranch != null && !defaultBranch.trim().isEmpty()) {
                data.put("defaultBranch", defaultBranch);
            }

            return Map.of("result", makeResult(data), "commit", data);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get commit data: " + e.getMessage());
        }
    }

    private Map<String, Object> createTag(Map<String, Object> in, String gitHubUri) {
        String gitHubAccessToken = assertString(in, GITHUB_ACCESSTOKEN);
        String gitHubOrgName = assertString(in, GITHUB_ORGNAME);
        String gitHubRepoName = assertString(in, GITHUB_REPONAME);
        String gitHubTagVersion = assertString(in, GITHUB_TAGVERSION);
        String gitHubTagMessage = assertString(in, GITHUB_TAGMESSAGE);
        String gitHubTaggerUID = assertString(in, GITHUB_TAGGERUID);
        String gitHubTaggerEMAIL = assertString(in, GITHUB_TAGGEREMAIL);
        String gitHubBranchSHA = getString(in, GITHUB_COMMIT_SHA);
        String githubBranch = getString(in, GITHUB_BRANCH);

        if (isBlank(gitHubBranchSHA) && isBlank(githubBranch)) {
            throw new IllegalArgumentException("Invalid task input parameters: " + GITHUB_COMMIT_SHA + " or " + GITHUB_BRANCH + " is required");
        }

        if (isBlank(gitHubBranchSHA)) {
            gitHubBranchSHA = getLatestSHAValue(in, gitHubUri);
        }

        if (dryRunMode) {
            log.info("Dry-run mode enabled: Skipping creation of tag '{}' for commit '{}' in {}/{}", gitHubTagVersion, gitHubBranchSHA, gitHubOrgName, gitHubRepoName);
            return Map.of();
        }

        log.info("Creating tag '{}' for commit '{}' in {}/{}", gitHubTagVersion, gitHubBranchSHA, gitHubOrgName, gitHubRepoName);

        try {
            // create tag object
            Map<String, Object> tag = new HashMap<>();
            tag.put("tag", gitHubTagVersion);
            tag.put("message", gitHubTagMessage);
            tag.put("object", gitHubBranchSHA);
            tag.put("type", "commit");

            Map<String, Object> tagger = new HashMap<>();
            tagger.put("name", gitHubTaggerUID);
            tagger.put("email", gitHubTaggerEMAIL);
            tagger.put("date", Instant.now().toString());
            tag.put("tagger", tagger);

            String apiUrl = buildApiUrl(gitHubUri, "/repos/" + gitHubOrgName + "/" + gitHubRepoName + "/git/tags");
            Map<String, Object> tagResult = getSingleObject("POST", apiUrl, gitHubAccessToken, tag);
            log.info("Successfully created TAG for the commit #{}", gitHubBranchSHA);

            // create tag reference
            Map<String, Object> ref = new HashMap<>();
            ref.put("ref", GITHUB_TAG_REFS + gitHubTagVersion);
            ref.put("sha", tagResult.get("sha"));

            String refApiUrl = buildApiUrl(gitHubUri, "/repos/" + gitHubOrgName + "/" + gitHubRepoName + "/git/refs");
            getSingleObject("POST", refApiUrl, gitHubAccessToken, ref);
            log.info("Successfully Created TAG Reference {}", ref.get("ref"));

            return Map.of();
        } catch (Exception e) {
            throw new RuntimeException("Can't create a tag: " + e.getMessage());
        }
    }

    private Map<String, Object> deleteTag(Map<String, Object> in, String gitHubUri) {
        String gitHubAccessToken = assertString(in, GITHUB_ACCESSTOKEN);
        String gitHubOrgName = assertString(in, GITHUB_ORGNAME);
        String gitHubRepoName = assertString(in, GITHUB_REPONAME);
        String gitHubTagName = assertString(in, GITHUB_TAGNAME);

        if (dryRunMode) {
            log.info("Dry-run mode enabled: Skipping deletion of tag '{}' in {}/{}", gitHubTagName, gitHubOrgName, gitHubRepoName);
            return Map.of();
        }

        log.info("Deleting tag '{}' in {}/{}", gitHubTagName, gitHubOrgName, gitHubRepoName);

        try {
            String apiUrl = buildApiUrl(gitHubUri, "/repos/" + gitHubOrgName + "/" + gitHubRepoName + "/git/refs/tags/" + gitHubTagName);
            getSingleObject("DELETE", apiUrl, gitHubAccessToken, null);
            log.info("Successfully deleted TAG '{}' from '{}/{}'", gitHubTagName, gitHubOrgName, gitHubRepoName);

            return Map.of();
        } catch (Exception e) {
            throw new RuntimeException("Cannot delete tag '" + gitHubTagName + "': " + e.getMessage());
        }
    }

    private Map<String, Object> deleteBranch(Map<String, Object> in, String gitHubUri) {
        String gitHubAccessToken = assertString(in, GITHUB_ACCESSTOKEN);
        String gitHubOrgName = assertString(in, GITHUB_ORGNAME);
        String gitHubRepoName = assertString(in, GITHUB_REPONAME);
        String gitHubBranchName = assertString(in, GITHUB_BRANCH);

        if (dryRunMode) {
            log.info("Dry-run mode enabled: Skipping deletion of branch '{}' in {}/{}", gitHubBranchName, gitHubOrgName, gitHubRepoName);
            return Map.of();
        }

        log.info("Deleting branch '{}' in {}/{}", gitHubBranchName, gitHubOrgName, gitHubRepoName);

        try {
            String apiUrl = buildApiUrl(gitHubUri, "/repos/" + gitHubOrgName + "/" + gitHubRepoName + "/git/refs/heads/" + gitHubBranchName);
            getSingleObject("DELETE", apiUrl, gitHubAccessToken, null);
            log.info("Deleted Branch '{}' from '{}/{}'", gitHubBranchName, gitHubOrgName, gitHubRepoName);

            return Map.of();
        } catch (Exception e) {
            throw new RuntimeException("Cannot Delete Branch '" + gitHubBranchName + "': " + e.getMessage());
        }
    }

    private Map<String, Object> addStatus(Map<String, Object> in, String gitHubUri) {
        String gitHubAccessToken = assertString(in, GITHUB_ACCESSTOKEN);
        String gitHubOrgName = assertString(in, GITHUB_ORGNAME);
        String gitHubRepoName = assertString(in, GITHUB_REPONAME);
        String commitSha = assertString(in, GITHUB_COMMIT_SHA);

        String state = assertStatusState(in);
        String targetUrl = getString(in, STATUS_CHECK_TARGET_URL, null);
        String description = getString(in, STATUS_CHECK_DESCRIPTION, null);
        String context = getString(in, STATUS_CHECK_CONTEXT, "default");

        if (dryRunMode) {
            log.info("Dry-run mode enabled: Skipping creation of a status check ({}) in {}/{} repository with sha '{}'",
                    state, gitHubOrgName, gitHubRepoName, commitSha);
            return Map.of();
        }

        log.info("Creating a status check ({}) in {}/{} repository with sha '{}'",
                state, gitHubOrgName, gitHubRepoName, commitSha);

        try {
            Map<String, Object> status = new HashMap<>();
            status.put("state", state);
            status.put("context", context);
            if (targetUrl != null) status.put("target_url", targetUrl);
            if (description != null) status.put("description", description);

            String apiUrl = buildApiUrl(gitHubUri, "/repos/" + gitHubOrgName + "/" + gitHubRepoName + "/statuses/" + commitSha);
            getSingleObject("POST", apiUrl, gitHubAccessToken, status);

            log.info("Status check created");

            return Map.of();
        } catch (Exception e) {
            throw new RuntimeException("Cannot create a status check request: " + e.getMessage());
        }
    }

    private Map<String, Object> getStatuses(Map<String, Object> in, String gitHubUri) {
        String gitHubAccessToken = assertString(in, GITHUB_ACCESSTOKEN);
        String gitHubOrgName = assertString(in, GITHUB_ORGNAME);
        String gitHubRepoName = assertString(in, GITHUB_REPONAME);
        String commitSha = assertString(in, GITHUB_COMMIT_SHA);

        try {
            log.info("Getting status for commit {} in {}/{}", commitSha, gitHubOrgName, gitHubRepoName);
            String apiUrl = buildApiUrl(gitHubUri, "/repos/" + gitHubOrgName + "/" + gitHubRepoName + "/commits/" + commitSha + "/statuses");
            List<Map<String, Object>> statuses = getListOfObjects(apiUrl, gitHubAccessToken);
            return Map.of("commitStatuses", statuses);
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot get status:" + e);
        }
    }

    private Map<String, Object> forkRepo(Map<String, Object> in, String gitHubUri) {
        String gitHubAccessToken = assertString(in, GITHUB_ACCESSTOKEN);
        String gitHubOrgName = assertString(in, GITHUB_ORGNAME);
        String gitHubRepoName = assertString(in, GITHUB_REPONAME);
        String targetOrg = getString(in, GITHUB_FORKTARGETORG);

        if (dryRunMode) {
            log.info("Dry-run mode enabled: Skipping forking of repository {}/{}", gitHubOrgName, gitHubRepoName);
            return Map.of();
        }

        try {
            Map<String, Object> body = new HashMap<>();
            if (targetOrg != null && !targetOrg.isEmpty()) {
                body.put("organization", targetOrg);
                log.info("Forking '{}/{}' into '{}' org...", gitHubOrgName, gitHubRepoName, targetOrg);
            } else {
                log.info("Forking '{}/{}' into your personal repository...", gitHubOrgName, gitHubRepoName);
            }

            String apiUrl = buildApiUrl(gitHubUri, "/repos/" + gitHubOrgName + "/" + gitHubRepoName + "/forks");
            getSingleObject("POST", apiUrl, gitHubAccessToken, body.isEmpty() ? null : body);
            log.info("Fork action completed");

            return Map.of();
        } catch (Exception e) {
            throw new RuntimeException("Error occurred during fork: " + e.getMessage());
        }
    }

    private Map<String, Object> getBranchList(Map<String, Object> in, String gitHubUri) {
        String gitHubAccessToken = assertString(in, GITHUB_ACCESSTOKEN);
        String gitHubOrgName = assertString(in, GITHUB_ORGNAME);
        String gitHubRepoName = assertString(in, GITHUB_REPONAME);

        try {
            log.info("Getting branch list from {}/{}...", gitHubOrgName, gitHubRepoName);
            String apiUrl = buildApiUrl(gitHubUri, "/repos/" + gitHubOrgName + "/" + gitHubRepoName + "/branches");
            List<Map<String, Object>> branches = getListOfObjects(apiUrl, gitHubAccessToken);

            Map<String, Object> result = Map.of();
            if (!branches.isEmpty()) {
                List<String> branchList = branches.stream()
                        .map(b -> (String) b.get("name"))
                        .toList();
                result = Map.of("branchList", branchList);
            }

            return result;
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while getting branch list: " + e.getMessage());
        }
    }

    private Map<String, Object> getTagList(Map<String, Object> in, String gitHubUri) {
        String gitHubAccessToken = assertString(in, GITHUB_ACCESSTOKEN);
        String gitHubOrgName = assertString(in, GITHUB_ORGNAME);
        String gitHubRepoName = assertString(in, GITHUB_REPONAME);

        try {
            log.info("Getting tag list from '{}/{}'...", gitHubOrgName, gitHubRepoName);
            String apiUrl = buildApiUrl(gitHubUri, "/repos/" + gitHubOrgName + "/" + gitHubRepoName + "/tags");
            List<Map<String, Object>> tags = getListOfObjects(apiUrl, gitHubAccessToken);

            Map<String, Object> result = Map.of();
            if (!tags.isEmpty()) {
                List<String> tagList = tags.stream()
                        .map(t -> (String) t.get("name"))
                        .toList();
                result = Map.of("tagList", tagList);
            }

            return result;
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while getting tag list: " + e.getMessage());
        }
    }

    private Map<String, Object> getPR(Map<String, Object> in, String gitHubUri) {
        String gitHubAccessToken = assertString(in, GITHUB_ACCESSTOKEN);
        String gitHubOrgName = assertString(in, GITHUB_ORGNAME);
        int gitHubPRNumber = assertInt(in, GITHUB_PRNUMBER);
        String gitHubRepoName = assertString(in, GITHUB_REPONAME);

        try {
            log.info("Getting PR {} info from '{}/{}'...", gitHubPRNumber, gitHubOrgName, gitHubRepoName);
            String apiUrl = buildApiUrl(gitHubUri, "/repos/" + gitHubOrgName + "/" + gitHubRepoName + "/pulls/" + gitHubPRNumber);
            Map<String, Object> pullRequest = getSingleObject("GET", apiUrl, gitHubAccessToken, null);

            return Map.of("pr", pullRequest);
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while getting PR: " + e.getMessage());
        }
    }

    private Map<String, Object> getPRList(Map<String, Object> in, String gitHubUri) {
        String gitHubAccessToken = assertString(in, GITHUB_ACCESSTOKEN);
        String gitHubOrgName = assertString(in, GITHUB_ORGNAME);
        String gitHubRepoName = assertString(in, GITHUB_REPONAME);
        String state = getString(in, GITHUB_PR_STATE, "open").toLowerCase();

        if (!GITHUB_VALID_PR_STATES.contains(state)) {
            throw new IllegalArgumentException("Invalid PR state '" + state +
                                               "'. Allowed values are only 'all', 'open', 'closed'.");
        }

        try {
            log.info("Getting '{}' PRs from '{}/{}'...", state, gitHubOrgName, gitHubRepoName);
            String apiUrl = buildApiUrl(gitHubUri, "/repos/" + gitHubOrgName + "/" + gitHubRepoName + "/pulls?state=" + state);
            List<Map<String, Object>> list = getListOfObjects(apiUrl, gitHubAccessToken);

            return Map.of("prList", list);
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while getting PR list: " + e.getMessage());
        }
    }

    private Map<String, Object> getPRFiles(Map<String, Object> in, String gitHubUri) {
        String gitHubAccessToken = assertString(in, GITHUB_ACCESSTOKEN);
        String gitHubOrgName = assertString(in, GITHUB_ORGNAME);
        String gitHubRepoName = assertString(in, GITHUB_REPONAME);
        int gitHubPRNumber = assertInt(in, GITHUB_PRNUMBER);

        try {
            log.info("Getting PR {} files from '{}/{}'", gitHubPRNumber, gitHubOrgName, gitHubRepoName);
            String apiUrl = buildApiUrl(gitHubUri, "/repos/" + gitHubOrgName + "/" + gitHubRepoName + "/pulls/" + gitHubPRNumber + "/files");
            List<Map<String, Object>> list = getListOfObjects(apiUrl, gitHubAccessToken);

            // cleanup patch field
            list = list.stream()
                    .map(f -> {
                        Map<String, Object> copy = new HashMap<>(f);
                        copy.remove("patch");
                        return copy;
                    })
                    .toList();

            List<String> added = list.stream().filter(f -> "added".equals(f.get("status"))).map(f -> (String) f.get("filename")).toList();
            List<String> removed = list.stream().filter(f -> "removed".equals(f.get("status"))).map(f -> (String) f.get("filename")).toList();
            List<String> modified = list.stream().filter(f -> "modified".equals(f.get("status"))).map(f -> (String) f.get("filename")).toList();
            List<String> renamed = list.stream().filter(f -> "renamed".equals(f.get("status"))).map(f -> (String) f.get("filename")).toList();
            List<String> copied = list.stream().filter(f -> "copied".equals(f.get("status"))).map(f -> (String) f.get("filename")).toList();
            List<String> changed = list.stream().filter(f -> "changed".equals(f.get("status"))).map(f -> (String) f.get("filename")).toList();
            List<String> any = Stream.of(added, removed, modified, renamed, copied, changed).flatMap(Collection::stream).toList();

            Map<String, Object> result = new HashMap<>();
            result.put("prFiles", list);
            result.put("prFilesAdded", added);
            result.put("prFilesRemoved", removed);
            result.put("prFilesModified", modified);
            result.put("prFilesRenamed", renamed);
            result.put("prFilesCopied", copied);
            result.put("prFilesChanged", changed);
            result.put("prFilesAny", any);

            return result;
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while getting PR list: " + e.getMessage());
        }
    }

    private Map<String, Object> createRepo(Map<String, Object> in, String gitHubUri) {
        String gitHubAccessToken = assertString(in, GITHUB_ACCESSTOKEN);
        String gitHubOrgName = assertString(in, GITHUB_ORGNAME);
        String gitHubRepoName = assertString(in, GITHUB_REPONAME);

        if (dryRunMode) {
            log.info("Dry-run mode enabled: Skipping creation of a repository in {}/{}", gitHubOrgName, gitHubRepoName);
            return Map.of();
        }

        log.info("Creating repository '{}' in '{}' organization", gitHubRepoName, gitHubOrgName);

        try {
            // check if repo already exists
            Map<String, Object> existingRepo = null;
            try {
                String checkUrl = buildApiUrl(gitHubUri, "/repos/" + gitHubOrgName + "/" + gitHubRepoName);
                existingRepo = getSingleObject("GET", checkUrl, gitHubAccessToken, null);
            } catch (GitHubApiException e) {
                if (e.getStatusCode() != STATUS_GITHUB_REPO_NOT_FOUND) {
                    throw e;
                }
            }

            Map<String, Object> repo;
            if (existingRepo == null) {
                log.debug("Repository " + gitHubRepoName + " does not exist in " + gitHubOrgName +
                          " organization. " + "Proceeding with repo creation");

                Map<String, Object> newRepo = new HashMap<>();
                newRepo.put("name", gitHubRepoName);

                String apiUrl = buildApiUrl(gitHubUri, "/orgs/" + gitHubOrgName + "/repos");
                repo = getSingleObject("POST", apiUrl, gitHubAccessToken, newRepo);

                log.info("Repository " + gitHubRepoName + " created successfully in " +
                         gitHubOrgName + " organization.");
            } else {
                repo = existingRepo;
                log.warn("Repository " + gitHubOrgName + "/" + gitHubRepoName + " already exists. Skipping creation ...");
            }

            Map<String, Object> result = new HashMap<>();
            result.put("cloneUrl", repo.get("clone_url"));
            result.put("scmUrl", repo.get("html_url"));
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while creating repository: ", e);
        }
    }

    private Map<String, Object> deleteRepo(Map<String, Object> in, String gitHubUri) {
        String gitHubAccessToken = assertString(in, GITHUB_ACCESSTOKEN);
        String gitHubOrgName = assertString(in, GITHUB_ORGNAME);
        String gitHubRepoName = assertString(in, GITHUB_REPONAME);

        if (dryRunMode) {
            log.info("Dry-run mode enabled: Skipping deletion of repository {} in {}", gitHubRepoName, gitHubOrgName);
            return Map.of();
        }

        log.info("Deleting repository '{}' from '{}' organization", gitHubRepoName, gitHubOrgName);

        try {
            // check if repo exists
            try {
                String checkUrl = buildApiUrl(gitHubUri, "/repos/" + gitHubOrgName + "/" + gitHubRepoName);
                getSingleObject("GET", checkUrl, gitHubAccessToken, null);

                log.debug("Repository " + gitHubRepoName + " exists in " + gitHubOrgName +
                          " organization. " + "Proceeding with repo deletion");

                String apiUrl = buildApiUrl(gitHubUri, "/repos/" + gitHubOrgName + "/" + gitHubRepoName);
                getSingleObject("DELETE", apiUrl, gitHubAccessToken, null);
                log.info("Repository " + gitHubRepoName + " deleted successfully from " +
                         gitHubOrgName + " organization.");
            } catch (GitHubApiException e) {
                if (e.getStatusCode() == STATUS_GITHUB_REPO_NOT_FOUND) {
                    log.warn("Repository " + gitHubOrgName + "/" + gitHubRepoName + " does not exist. " +
                             "Looks like it is already deleted. Skipping deletion ...");
                } else {
                    throw e;
                }
            }

            return Map.of();
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while deleting repository: ", e);
        }
    }

    private Map<String, Object> getLatestSHA(Map<String, Object> in, String gitHubUri) {
        return Map.of("latestCommitSHA", getLatestSHAValue(in, gitHubUri));
    }

    @SuppressWarnings("unchecked")
    private String getLatestSHAValue(Map<String, Object> in, String gitHubUri) {
        String gitHubAccessToken = assertString(in, GITHUB_ACCESSTOKEN);
        String gitHubOrgName = assertString(in, GITHUB_ORGNAME);
        String gitHubRepoName = assertString(in, GITHUB_REPONAME);
        String gitHubBranchName = getString(in, GITHUB_BRANCH, "master");

        try {
            log.info("Getting latest commit SHA for '{}/{}/{}'...", gitHubOrgName, gitHubRepoName, gitHubBranchName);
            String apiUrl = buildApiUrl(gitHubUri, "/repos/" + gitHubOrgName + "/" + gitHubRepoName + "/branches/" + gitHubBranchName);
            Map<String, Object> branch = getSingleObject("GET", apiUrl, gitHubAccessToken, null);
            Map<String, Object> commit = (Map<String, Object>) branch.get("commit");
            String latestCommitSHA = (String) commit.get("sha");
            log.info("Latest commit SHA: '{}'", latestCommitSHA);
            return latestCommitSHA;
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while getting latest commit SHA: " + e.getMessage());
        }
    }

    private Map<String, Object> getContent(Map<String, Object> in, String gitHubUri) {
        String gitHubAccessToken = assertString(in, GITHUB_ACCESSTOKEN);
        String gitHubOrgName = assertString(in, GITHUB_ORGNAME);
        String gitHubRepoName = assertString(in, GITHUB_REPONAME);
        String gitHubRef = getString(in, GITHUB_REF);
        String gitHubPath = assertString(in, GITHUB_PATH);

        try {
            log.info("Getting '{}' file content in {}/{} repo with ref {}", gitHubPath, gitHubOrgName, gitHubRepoName, gitHubRef);
            String apiUrl = buildApiUrl(gitHubUri, "/repos/" + gitHubOrgName + "/" + gitHubRepoName + "/contents/" + gitHubPath);
            if (gitHubRef != null) {
                apiUrl += "?ref=" + gitHubRef;
            }

            Map<String, Object> response = getSingleObject("GET", apiUrl, gitHubAccessToken, null);

            Map<String, Object> item = new HashMap<>(response);
            String encoding = (String) item.get("encoding");
            String content = (String) item.get("content");

            if ("base64".equalsIgnoreCase(encoding) && content != null) {
                Base64.Decoder decoder;
                if (content.contains("\n")) {
                    decoder = Base64.getMimeDecoder();
                } else {
                    decoder = Base64.getDecoder();
                }
                item.put("content", new String(decoder.decode(content)));
            }

            return Map.of("contents", List.of(item));
        } catch (Exception e) {
            throw new RuntimeException("Cannot get content: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> createHook(Map<String, Object> in, String gitHubUri) {
        String gitHubAccessToken = assertString(in, GITHUB_ACCESSTOKEN);
        String gitHubOrgName = assertString(in, GITHUB_ORGNAME);
        String gitHubRepoName = assertString(in, GITHUB_REPONAME);
        String hookUrl = assertString(in, GITHUB_HOOK_URL).trim();

        if (dryRunMode) {
            log.info("Dry-run mode enabled: Skipping creation of a hook in {}/{}", gitHubOrgName, gitHubRepoName);
            return Map.of();
        }

        try {
            Map<String, Object> config = getMap(in, GITHUB_HOOK_CFG, Map.of());
            if (config.isEmpty()) {
                config = new HashMap<>();
                config.put("content_type", getString(in, GITHUB_HOOK_CONTENT_TYPE, "form"));
                String secret = getString(in, GITHUB_HOOK_SECRET);
                if (secret != null) config.put("secret", secret);
                config.put("insecure_ssl", getString(in, GITHUB_HOOK_INSECURE_SSL, "0"));
            }
            config.put("url", hookUrl);

            Map<String, Object> hook = new HashMap<>();
            hook.put("name", "web");
            hook.put("active", true);
            hook.put("events", assertList(in, GITHUB_HOOK_EVENTS));
            hook.put("config", config);

            if (MapUtils.getBoolean(in, GITHUB_HOOK_REPLACE, false)) {
                String listUrl = buildApiUrl(gitHubUri, "/repos/" + gitHubOrgName + "/" + gitHubRepoName + "/hooks");
                List<Map<String, Object>> hooks = getListOfObjects(listUrl, gitHubAccessToken);
                List<Map<String, Object>> existingHooks = hooks.stream()
                        .filter(h -> {
                            Map<String, Object> cfg = (Map<String, Object>) h.get("config");
                            return hookUrl.equalsIgnoreCase((String) cfg.get("url"));
                        })
                        .toList();

                for (Map<String, Object> h : existingHooks) {
                    int id = ((Number) h.get("id")).intValue();
                    String deleteUrl = buildApiUrl(gitHubUri, "/repos/" + gitHubOrgName + "/" + gitHubRepoName + "/hooks/" + id);
                    getSingleObject("DELETE", deleteUrl, gitHubAccessToken, null);
                }
            }

            String apiUrl = buildApiUrl(gitHubUri, "/repos/" + gitHubOrgName + "/" + gitHubRepoName + "/hooks");
            Map<String, Object> result = getSingleObject("POST", apiUrl, gitHubAccessToken, hook);
            log.info("Hook created id: {}", result.get("id"));
            return Map.of("hook", result);
        } catch (Exception e) {
            throw new RuntimeException("Cannot create hook: " + e.getMessage());
        }
    }

    private Map<String, Object> createIssue(Map<String, Object> in, String gitHubUri) {
        String gitHubAccessToken = assertString(in, GITHUB_ACCESSTOKEN);
        String gitHubOrgName = assertString(in, GITHUB_ORGNAME);
        String gitHubRepoName = assertString(in, GITHUB_REPONAME);

        if (dryRunMode) {
            log.info("Dry-run mode enabled: Skipping creation of an issue in {}/{}", gitHubOrgName, gitHubRepoName);
            return Map.of();
        }

        log.info("Creating issue in {}/{}", gitHubOrgName, gitHubRepoName);

        try {
            Map<String, Object> issue = new HashMap<>();
            issue.put("title", assertString(in, ISSUE_TITLE));
            String body = getString(in, ISSUE_BODY);
            if (body != null) issue.put("body", body);

            String assignee = getString(in, ISSUE_ASSIGNEE);
            if (assignee != null) issue.put("assignee", assignee);

            List<String> labels = MapUtils.getList(in, ISSUE_LABELS, List.of());
            if (!labels.isEmpty()) issue.put("labels", labels);

            String apiUrl = buildApiUrl(gitHubUri, "/repos/" + gitHubOrgName + "/" + gitHubRepoName + "/issues");
            Map<String, Object> result = getSingleObject("POST", apiUrl, gitHubAccessToken, issue);
            log.info("Issue created id: {}", result.get("id"));
            return Map.of("issue", result);
        } catch (Exception e) {
            throw new RuntimeException("Cannot create issue: " + e.getMessage());
        }
    }

    private String buildApiUrl(String baseUrl, String path) throws URISyntaxException {
        URI uri = new URI(baseUrl);
        String host = uri.getHost();
        if ("github.com".equals(host) || "gist.github.com".equals(host)) {
            host = "api.github.com";
        }

        String scheme = uri.getScheme();
        int port = uri.getPort();

        URI apiUri = new URI(scheme, null, host, port, path, null, null);
        return apiUri.toString();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getSingleObject(String method, String url, String token, Object body) throws IOException, InterruptedException {
        HttpResponse<String> response = sendRequest(method, url, token, body);
        Object parsed = parseResponseAsObject(response);
        if (parsed instanceof Map) {
            return (Map<String, Object>) parsed;
        }
        throw new RuntimeException("Expected single object but got: " + parsed.getClass().getName());
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getListOfObjects(String url, String token) throws IOException, InterruptedException {
        HttpResponse<String> response = sendRequest("GET", url, token, null);
        Object parsed = parseResponseAsList(response);
        if (parsed instanceof List) {
            return (List<Map<String, Object>>) parsed;
        }
        throw new RuntimeException("Expected list but got: " + parsed.getClass().getName());
    }

    private HttpResponse<String> sendRequest(String method, String url, String token, Object body) throws IOException, InterruptedException {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "application/vnd.github.v3+json")
                .header("Authorization", "token " + token);

        if (body != null) {
            String jsonBody = objectMapper.writeValueAsString(body);
            requestBuilder.header("Content-Type", "application/json");
            requestBuilder.method(method, BodyPublishers.ofString(jsonBody));
        } else if ("DELETE".equals(method)) {
            requestBuilder.DELETE();
        } else {
            requestBuilder.method(method, BodyPublishers.noBody());
        }

        HttpRequest request = requestBuilder.build();
        return httpClient.send(request, BodyHandlers.ofString());
    }

    private Object parseResponseAsObject(HttpResponse<String> response) throws IOException {
        handleErrorResponse(response);

        if (response.body() == null || response.body().isEmpty()) {
            return Map.of();
        }

        return objectMapper.readValue(response.body(), OBJECT_TYPE);
    }

    private Object parseResponseAsList(HttpResponse<String> response) throws IOException {
        handleErrorResponse(response);

        if (response.body() == null || response.body().isEmpty()) {
            return List.of();
        }

        return objectMapper.readValue(response.body(), LIST_OF_OBJECT_TYPE);
    }

    private void handleErrorResponse(HttpResponse<String> response) throws IOException {
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return;
        }

        String errorMessage = "GitHub API error: " + response.statusCode();
        if (response.body() != null && !response.body().isEmpty()) {
            try {
                Map<String, Object> error = objectMapper.readValue(response.body(), OBJECT_TYPE);
                errorMessage += " - " + error.get("message");
            } catch (Exception ignored) {
                errorMessage += " - " + response.body();
            }
        }
        throw new GitHubApiException(errorMessage, response.statusCode());
    }

    private static Action getAction(Map<String, Object> in) {
        String v = MapUtils.assertString(in, ACTION_KEY);
        try {
            return Action.valueOf(v.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Unknown action: '" + v + "'. Available actions: " + Arrays.toString(Action.values()));
        }
    }

    private static String assertStatusState(Map<String, Object> in) {
        String state = assertString(in, STATUS_CHECK_STATE).trim().toLowerCase();
        if (!STATUS_CHECK_STATES.contains(state)) {
            throw new IllegalArgumentException("Unknown state: " + state + ". Expected one of " + STATUS_CHECK_STATES);
        }
        return state;
    }

    @Deprecated
    private static Map<String, Object> makeResult(Object data) {
        Map<String, Object> m = new HashMap<>();
        m.put("ok", true);
        m.put("data", data);
        return m;
    }

    private static boolean isBlank(String str) {
        return str == null || str.isBlank();
    }

    public enum Action {
        CREATEPR,
        COMMENTPR,
        MERGEPR,
        CLOSEPR,
        GETPRCOMMITLIST,
        MERGE,
        CREATEISSUE,
        CREATETAG,
        CREATEHOOK,
        DELETETAG,
        DELETEBRANCH,
        GETCOMMIT,
        ADDSTATUS,
        GETSTATUSES,
        FORKREPO,
        GETBRANCHLIST,
        GETPR,
        GETPRLIST,
        GETPRFILES,
        GETTAGLIST,
        GETLATESTSHA,
        CREATEREPO,
        DELETEREPO,
        GETCONTENT
    }

    private static class GitHubApiException extends IOException {

        private final int statusCode;

        public GitHubApiException(String message, int statusCode) {
            super(message);
            this.statusCode = statusCode;
        }

        public int getStatusCode() {
            return statusCode;
        }
    }
}