package com.walmartlabs.concord.plugins.git;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2018 Wal-Mart Store, Inc.
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.walmartlabs.concord.sdk.MapUtils;
import org.eclipse.egit.github.core.*;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.RequestException;
import org.eclipse.egit.github.core.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

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
    private static final String GITHUB_FORKTARGETORG = "targetOrg";

    private static final String STATUS_CHECK_STATE = "state";
    private static final String STATUS_CHECK_TARGET_URL = "targetUrl";
    private static final String STATUS_CHECK_DESCRIPTION = "description";
    private static final String STATUS_CHECK_CONTEXT = "context";
    private static final String GITHUB_PR_STATE = "state";

    private static final List<String> GITHUB_VALID_PR_STATES = Arrays.asList("all", "open", "closed");
    private static final Set<String> STATUS_CHECK_STATES = new HashSet<>(Arrays.asList("error", "failure", "pending", "success"));

    private static final int STATUS_GITHUB_REPO_NOT_FOUND = 404;

    public Map<String, Object> execute(Map<String, Object> in, Map<String, Object> defaults) {
        Action action = getAction(in);
        String gitHubUri = getUrl(defaults, in, API_URL_KEY);
        log.info("Starting '{}' action...", action);
        log.info("Using GitHub apiUrl {}", gitHubUri);

        switch (action) {
            case CREATEPR: {
                return createPR(in, gitHubUri);
            }
            case COMMENTPR: {
                return commentPR(in, gitHubUri);
            }
            case MERGEPR: {
                return mergePR(in, gitHubUri);
            }
            case CLOSEPR: {
                return closePR(in, gitHubUri);
            }
            case CREATETAG: {
                return createTag(in, gitHubUri);
            }
            case DELETETAG: {
                return deleteTag(in, gitHubUri);
            }
            case DELETEBRANCH: {
                return deleteBranch(in, gitHubUri);
            }
            case MERGE: {
                return merge(in, gitHubUri);
            }
            case GETCOMMIT: {
                return getCommit(in, gitHubUri);
            }
            case ADDSTATUS: {
                return addStatus(in, gitHubUri);
            }
            case GETSTATUSES: {
                return getStatuses(in, gitHubUri);
            }
            case FORKREPO: {
                return forkRepo(in, gitHubUri);
            }
            case GETBRANCHLIST: {
                return getBranchList(in, gitHubUri);
            }
            case GETPRLIST: {
                return getPRList(in, gitHubUri);
            }
            case GETTAGLIST: {
                return getTagList(in, gitHubUri);
            }
            case GETLATESTSHA: {
                return getLatestSHA(in, gitHubUri);
            }
            case CREATEREPO: {
                return createRepo(in, gitHubUri);
            }
            case DELETEREPO: {
                return deleteRepo(in, gitHubUri);
            }
            default:
                throw new IllegalArgumentException("Unsupported action type: " + action);
        }
    }

    private static Map<String, Object> createPR(Map<String, Object> in, String gitHubUri) {
        String gitHubAccessToken = assertString(in, GITHUB_ACCESSTOKEN);
        String gitHubOrgName = assertString(in, GITHUB_ORGNAME);
        String gitHubRepoName = assertString(in, GITHUB_REPONAME);
        String gitHubPRTitle = assertString(in, GITHUB_PRTITLE);
        String gitHubPRBody = assertString(in, GITHUB_PRBODY);
        String gitHubPRHead = assertString(in, GITHUB_PRHEAD);
        String gitHubPRBase = assertString(in, GITHUB_PRBASE);

        GitHubClient client = GitHubClient.createClient(gitHubUri);
        try {
            //Connect to GitHub
            client.setOAuth2Token(gitHubAccessToken);
            IRepositoryIdProvider repo = RepositoryId.create(gitHubOrgName, gitHubRepoName);

            //Create Pull Request
            PullRequestService prService = new PullRequestService(client);

            PullRequest pr = new PullRequest();
            pr.setTitle(gitHubPRTitle);
            pr.setBody(gitHubPRBody);
            pr.setHead(new PullRequestMarker().setLabel(gitHubPRHead));
            pr.setBase(new PullRequestMarker().setLabel(gitHubPRBase));

            PullRequest result = prService.createPullRequest(repo, pr);
            if (result != null) {
                log.info("Created PR# {}", result.getNumber());
                return Collections.singletonMap("prId", result.getNumber());
            }
            return Collections.emptyMap();
        } catch (IOException e) {
            throw new RuntimeException("Cannot create a pull request: " + e.getMessage());
        }
    }

    private static Map<String, Object> commentPR(Map<String, Object> in, String gitHubUri) {
        String gitHubAccessToken = assertString(in, GITHUB_ACCESSTOKEN);
        String gitHubOrgName = assertString(in, GITHUB_ORGNAME);
        String gitHubRepoName = assertString(in, GITHUB_REPONAME);
        String gitHubPRComment = assertString(in, GITHUB_PRCOMMENT);
        int gitHubPRID = assertInt(in, GITHUB_PRID);

        GitHubClient client = GitHubClient.createClient(gitHubUri);
        client.setOAuth2Token(gitHubAccessToken);
        IRepositoryIdProvider repo = RepositoryId.create(gitHubOrgName, gitHubRepoName);

        IssueService issueService = new IssueService(client);
        PullRequestService prService = new PullRequestService(client);

        try {
            log.info("Using PR# {}", gitHubPRID);
            PullRequest pullRequest = prService.getPullRequest(repo, gitHubPRID);
            issueService.createComment(repo, Integer.toString(pullRequest.getNumber()), gitHubPRComment);
            log.info("Commented on PR# {}", gitHubPRID);

            return Collections.emptyMap();
        } catch (IOException e) {
            throw new RuntimeException("Cannot comment on the pull request: " + e.getMessage());
        }
    }

    private static Map<String, Object> mergePR(Map<String, Object> in, String gitHubUri) {
        String gitHubAccessToken = assertString(in, GITHUB_ACCESSTOKEN);
        String gitHubOrgName = assertString(in, GITHUB_ORGNAME);
        String gitHubRepoName = assertString(in, GITHUB_REPONAME);
        int gitHubPRID = assertInt(in, GITHUB_PRID);
        String commitMessage = getString(in, GITHUB_MERGECOMMITMSG, "GitHub PR Merge");

        GitHubClient client = GitHubClient.createClient(gitHubUri);
        client.setOAuth2Token(gitHubAccessToken);
        IRepositoryIdProvider repo = RepositoryId.create(gitHubOrgName, gitHubRepoName);

        PullRequestService prService = new PullRequestService(client);
        try {
            log.info("Using PR# {}", gitHubPRID);
            prService.merge(repo, gitHubPRID, commitMessage);
            log.info("Merged PR# {}", gitHubPRID);

            return Collections.emptyMap();
        } catch (IOException e) {
            throw new RuntimeException("Cannot merge the pull request: " + e.getMessage());
        }
    }

    private static Map<String, Object> closePR(Map<String, Object> in, String gitHubUri) {
        String gitHubAccessToken = assertString(in, GITHUB_ACCESSTOKEN);
        String gitHubOrgName = assertString(in, GITHUB_ORGNAME);
        String gitHubRepoName = assertString(in, GITHUB_REPONAME);
        int gitHubPRID = assertInt(in, GITHUB_PRID);

        GitHubClient client = GitHubClient.createClient(gitHubUri);
        client.setOAuth2Token(gitHubAccessToken);
        IRepositoryIdProvider repo = RepositoryId.create(gitHubOrgName, gitHubRepoName);

        PullRequestService prService = new PullRequestService(client);
        try {
            log.info("Using PR# {}", gitHubPRID);
            PullRequest pullRequest = prService.getPullRequest(repo, gitHubPRID);
            pullRequest.setState("closed");
            pullRequest.setClosedAt(Calendar.getInstance().getTime());
            prService.editPullRequest(repo, pullRequest);
            log.info("Closed PR# {}", gitHubPRID);

            return Collections.emptyMap();
        } catch (IOException e) {
            throw new RuntimeException("Cannot close the pull request: " + e.getMessage());
        }
    }

    private static Map<String, Object> merge(Map<String, Object> in, String gitHubUri) {
        String gitHubAccessToken = assertString(in, GITHUB_ACCESSTOKEN);
        String gitHubOrgName = assertString(in, GITHUB_ORGNAME);
        String gitHubRepoName = assertString(in, GITHUB_REPONAME);
        String head = assertString(in, GITHUB_MERGEHEAD);
        String base = assertString(in, GITHUB_MERGEBASE);
        String commitMessage = assertString(in, GITHUB_MERGECOMMITMSG);

        GitHubClient client = GitHubClient.createClient(gitHubUri);
        //Connect to GitHub
        client.setOAuth2Token(gitHubAccessToken);
        IRepositoryIdProvider repo = RepositoryId.create(gitHubOrgName, gitHubRepoName);

        //Build the uri
        StringBuilder uri = new StringBuilder("/repos");
        uri.append('/').append(repo);
        uri.append("/merges");

        //Get Input params for merge
        Map<String, String> params = new HashMap<>();
        params.put("base", base);
        params.put("head", head);
        params.put("commit_message", commitMessage);
        try {
            log.info("Using head '{}'", head);
            client.postStream(uri.toString(), params);
            log.info("Merged '{}' with '{}'", head, base);

            return Collections.emptyMap();
        } catch (IOException e) {
            throw new RuntimeException("Cannot merge 'base' and 'head': " + e.getMessage());
        }
    }

    private static Map<String, Object> getCommit(Map<String, Object> in, String gitHubUri) {
        String gitHubAccessToken = assertString(in, GITHUB_ACCESSTOKEN);
        String gitHubOrgName = assertString(in, GITHUB_ORGNAME);
        String gitHubRepoName = assertString(in, GITHUB_REPONAME);
        String gitHubCommitSha = assertString(in, GITHUB_COMMIT_SHA);

        GitHubClient client = GitHubClient.createClient(gitHubUri);
        client.setOAuth2Token(gitHubAccessToken);
        IRepositoryIdProvider repo = RepositoryId.create(gitHubOrgName, gitHubRepoName);

        CommitService commitService = new CommitService(client);

        try {
            log.info("Getting commit {}", gitHubCommitSha);
            RepositoryCommit repositoryCommit = commitService.getCommit(repo, gitHubCommitSha);

            Object data = new ObjectMapper().convertValue(repositoryCommit, Map.class);
            return Collections.singletonMap("result", makeResult(data));
        } catch (IOException e) {
            throw new RuntimeException("Failed to get commit data: " + e.getMessage());
        }
    }

    private static Map<String, Object> createTag(Map<String, Object> in, String gitHubUri) {
        String gitHubAccessToken = assertString(in, GITHUB_ACCESSTOKEN);
        String gitHubOrgName = assertString(in, GITHUB_ORGNAME);
        String gitHubRepoName = assertString(in, GITHUB_REPONAME);
        String gitHubTagVersion = assertString(in, GITHUB_TAGVERSION);
        String gitHubTagMessage = assertString(in, GITHUB_TAGMESSAGE);
        String gitHubTaggerUID = assertString(in, GITHUB_TAGGERUID);
        String gitHubTaggerEMAIL = assertString(in, GITHUB_TAGGEREMAIL);
        String gitHubBranchSHA = assertString(in, GITHUB_COMMIT_SHA);

        //Initiate the client
        GitHubClient client = GitHubClient.createClient(gitHubUri);
        //Connect to GitHub
        client.setOAuth2Token(gitHubAccessToken);
        IRepositoryIdProvider repo = RepositoryId.create(gitHubOrgName, gitHubRepoName);

        //Get Required Information to Create Tag.
        TypedResource typedResource = new TypedResource();
        typedResource.setType("commit");
        typedResource.setSha(gitHubBranchSHA);

        CommitUser commitUser = new CommitUser();
        commitUser.setName(gitHubTaggerUID);
        commitUser.setEmail(gitHubTaggerEMAIL);
        commitUser.setDate(new Date());

        Tag tag = new Tag();
        tag.setTag(gitHubTagVersion);
        tag.setMessage(gitHubTagMessage);
        tag.setObject(typedResource);
        tag.setTagger(commitUser);

        DataService dataService = new DataService(client);
        try {
            //Create Tag
            dataService.createTag(repo, tag);
            log.info("Successfully created TAG for the commit #{}", gitHubBranchSHA);
        } catch (IOException e) {
            throw new RuntimeException("Can't create a tag: " + e.getMessage());
        }

        Reference tagRef = new Reference();
        tagRef.setRef(GITHUB_TAG_REFS.concat(gitHubTagVersion));
        tagRef.setObject(typedResource);

        try {
            //Create Tag Reference
            dataService.createReference(repo, tagRef);
            log.info("Successfully Created TAG Reference {}", tagRef.getRef());

            return Collections.emptyMap();
        } catch (IOException e) {
            throw new RuntimeException("Can't create a tag reference: " + e.getMessage());
        }
    }

    private static Map<String, Object> deleteTag(Map<String, Object> in, String gitHubUri) {
        String gitHubAccessToken = assertString(in, GITHUB_ACCESSTOKEN);
        String gitHubOrgName = assertString(in, GITHUB_ORGNAME);
        String gitHubRepoName = assertString(in, GITHUB_REPONAME);
        String gitHubTagName = assertString(in, GITHUB_TAGNAME);

        //Initiate the client
        GitHubClient client = GitHubClient.createClient(gitHubUri);
        //Connect to GitHub
        client.setOAuth2Token(gitHubAccessToken);
        IRepositoryIdProvider repo = RepositoryId.create(gitHubOrgName, gitHubRepoName);

        Tag tag = new Tag();
        tag.setTag(gitHubTagName);

        DataService dataService = new DataService(client);
        try {
            //delete Tag
            dataService.deleteTag(repo, tag);
            log.info("Successfully deleted TAG '{}' from '{}/{}'", gitHubTagName, gitHubOrgName, gitHubRepoName);

            return Collections.emptyMap();
        } catch (IOException e) {
            throw new RuntimeException("Cannot delete tag '" + gitHubTagName + "': " + e.getMessage());
        }
    }

    private static Map<String, Object> deleteBranch(Map<String, Object> in, String gitHubUri) {
        String gitHubAccessToken = assertString(in, GITHUB_ACCESSTOKEN);
        String gitHubOrgName = assertString(in, GITHUB_ORGNAME);
        String gitHubRepoName = assertString(in, GITHUB_REPONAME);
        String gitHubBranchName = assertString(in, GITHUB_BRANCH);

        //Initiate the client
        GitHubClient client = GitHubClient.createClient(gitHubUri);
        //Connect to GitHub
        client.setOAuth2Token(gitHubAccessToken);
        IRepositoryIdProvider repo = RepositoryId.create(gitHubOrgName, gitHubRepoName);

        DataService dataService = new DataService(client);
        try {
            //delete branch
            dataService.deleteBranch(repo, gitHubBranchName);
            log.info("Deleted Branch '{}' from '{}/{}'", gitHubBranchName, gitHubOrgName, gitHubRepoName);

            return Collections.emptyMap();
        } catch (IOException e) {
            throw new RuntimeException("Cannot Delete Branch '" + gitHubBranchName + "': " + e.getMessage());
        }
    }

    private static Map<String, Object> addStatus(Map<String, Object> in, String gitHubUri) {
        String gitHubAccessToken = assertString(in, GITHUB_ACCESSTOKEN);
        String gitHubOrgName = assertString(in, GITHUB_ORGNAME);
        String gitHubRepoName = assertString(in, GITHUB_REPONAME);
        String commitSha = assertString(in, GITHUB_COMMIT_SHA);

        String state = assertStatusState(in);
        String targetUrl = getString(in, STATUS_CHECK_TARGET_URL, null);
        String description = getString(in, STATUS_CHECK_DESCRIPTION, null);
        String context = getString(in, STATUS_CHECK_CONTEXT, "default");

        log.info("Creating status check ({}) for {}/{} repo with sha '{}'",
                state, gitHubOrgName, gitHubRepoName, commitSha);

        GitHubClient client = GitHubClient.createClient(gitHubUri);
        client.setOAuth2Token(gitHubAccessToken);
        IRepositoryIdProvider repo = RepositoryId.create(gitHubOrgName, gitHubRepoName);

        CommitService commitService = new CommitService(client);

        CommitStatus commitStatus = new CommitStatus();

        // error, failure, pending, or success
        commitStatus.setState(state);

        // Label that identifies the status
        commitStatus.setContext(context);

        // Link to test
        commitStatus.setTargetUrl(targetUrl);

        commitStatus.setDescription(description);

        try {
            commitService.createStatus(repo, commitSha, commitStatus);
            log.info("Status check created");

            return Collections.emptyMap();
        } catch (Exception e) {
            throw new RuntimeException("Cannot create a status check request: " + e.getMessage());
        }
    }

    private static Map<String, Object> getStatuses(Map<String, Object> in, String gitHubUri) {
        String gitHubAccessToken = assertString(in, GITHUB_ACCESSTOKEN);
        String gitHubOrgName = assertString(in, GITHUB_ORGNAME);
        String gitHubRepoName = assertString(in, GITHUB_REPONAME);
        String commitSha = assertString(in, GITHUB_COMMIT_SHA);

        ObjectMapper objectMapper = new ObjectMapper();
        GitHubClient client = GitHubClient.createClient(gitHubUri);
        client.setOAuth2Token(gitHubAccessToken);
        IRepositoryIdProvider repo = RepositoryId.create(gitHubOrgName, gitHubRepoName);

        CommitService commitService = new CommitService(client);
        List<CommitStatus> statuses;
        try {
            log.info("Getting status for commit {}", commitSha);
            statuses = commitService.getStatuses(repo, commitSha);
            return Collections.singletonMap("commitStatuses", objectMapper.convertValue(statuses, Object.class));
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot get status:" + e);
        }
    }

    private static Map<String, Object> forkRepo(Map<String, Object> in, String gitHubUri) {
        String gitHubAccessToken = assertString(in, GITHUB_ACCESSTOKEN);
        String gitHubOrgName = assertString(in, GITHUB_ORGNAME);
        String gitHubRepoName = assertString(in, GITHUB_REPONAME);
        String targetOrg = getString(in, GITHUB_FORKTARGETORG);

        GitHubClient client = GitHubClient.createClient(gitHubUri);

        try {
            //Connect to GitHub
            client.setOAuth2Token(gitHubAccessToken);
            IRepositoryIdProvider repo = RepositoryId.create(gitHubOrgName, gitHubRepoName);

            //Fork a Git Repo
            RepositoryService repoService = new RepositoryService(client);
            if (targetOrg != null && !targetOrg.isEmpty()) {
                log.info("Forking '{}/{}' into '{}' org...", gitHubOrgName, gitHubRepoName, targetOrg);
                repoService.forkRepository(repo, targetOrg);
                log.info("Fork action completed");
            } else {
                log.info("Forking '{}/{}' into your personal repo...", gitHubOrgName, gitHubRepoName);
                repoService.forkRepository(repo);
                log.info("Fork action completed");
            }

            return Collections.emptyMap();
        } catch (Exception e) {
            throw new RuntimeException("Error occured during fork: " + e.getMessage());
        }
    }

    private static Map<String, Object> getBranchList(Map<String, Object> in, String gitHubUri) {
        String gitHubAccessToken = assertString(in, GITHUB_ACCESSTOKEN);
        String gitHubOrgName = assertString(in, GITHUB_ORGNAME);
        String gitHubRepoName = assertString(in, GITHUB_REPONAME);

        GitHubClient client = GitHubClient.createClient(gitHubUri);

        try {
            //Connect to GitHub
            client.setOAuth2Token(gitHubAccessToken);
            IRepositoryIdProvider repo = RepositoryId.create(gitHubOrgName, gitHubRepoName);

            //Getting branch list
            RepositoryService repoService = new RepositoryService(client);

            Map<String, Object> result = Collections.emptyMap();

            log.info("Getting branch list from '{}/{}'...", gitHubOrgName, gitHubRepoName);
            List<RepositoryBranch> list = repoService.getBranches(repo);
            if (list != null && !list.isEmpty()) {
                List<String> branchList = list.stream().map(RepositoryBranch::getName).collect(Collectors.toList());
                log.info("List of Branches:  '{}'", branchList);
                result = Collections.singletonMap("branchList", branchList);
            }
            log.info("'getBranchList' action completed");
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Error occured while getting branch list: " + e.getMessage());
        }
    }

    private static Map<String, Object> getTagList(Map<String, Object> in, String gitHubUri) {
        String gitHubAccessToken = assertString(in, GITHUB_ACCESSTOKEN);
        String gitHubOrgName = assertString(in, GITHUB_ORGNAME);
        String gitHubRepoName = assertString(in, GITHUB_REPONAME);

        GitHubClient client = GitHubClient.createClient(gitHubUri);

        try {
            //Connect to GitHub
            client.setOAuth2Token(gitHubAccessToken);
            IRepositoryIdProvider repo = RepositoryId.create(gitHubOrgName, gitHubRepoName);

            //Getting tag list
            RepositoryService repoService = new RepositoryService(client);

            Map<String, Object> result = Collections.emptyMap();

            log.info("Getting tag list from '{}/{}'...", gitHubOrgName, gitHubRepoName);
            List<RepositoryTag> list = repoService.getTags(repo);
            if (list != null && !list.isEmpty()) {
                List<String> tagList = list.stream().map(RepositoryTag::getName).collect(Collectors.toList());
                log.info("List of Tags: '{}'", tagList);
                result = Collections.singletonMap("tagList", tagList);
            }
            log.info("'getTagList' action completed");
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Error occured while getting tag list: " + e.getMessage());
        }
    }

    private static Map<String, Object> getPRList(Map<String, Object> in, String gitHubUri) {
        String gitHubAccessToken = assertString(in, GITHUB_ACCESSTOKEN);
        String gitHubOrgName = assertString(in, GITHUB_ORGNAME);
        String gitHubRepoName = assertString(in, GITHUB_REPONAME);
        String state = getString(in, GITHUB_PR_STATE, "open").toLowerCase();

        if (!GITHUB_VALID_PR_STATES.contains(state)) {
            throw new IllegalArgumentException("Invalid PR state '" + state +
                    "'. Allowed values are only 'all', 'open', 'closed'.");
        }

        GitHubClient client = GitHubClient.createClient(gitHubUri);

        try {
            //Connect to GitHub
            client.setOAuth2Token(gitHubAccessToken);
            IRepositoryIdProvider repo = RepositoryId.create(gitHubOrgName, gitHubRepoName);

            //Getting PR list
            PullRequestService prService = new PullRequestService(client);

            log.info("Getting '{}' PRs from '{}/{}'...", state, gitHubOrgName, gitHubRepoName);
            List<PullRequest> list = prService.getPullRequests(repo, state);
            if (list == null) {
                list = Collections.emptyList();
            }

            log.info("Got total of {} PRs.", list.size());

            ObjectMapper om = new ObjectMapper();
            return Collections.singletonMap("prList", om.convertValue(list, Object.class));
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while getting PR list: " + e.getMessage());
        }
    }

    private static Map<String, Object> createRepo(Map<String, Object> in, String gitHubUri) {
        String gitHubAccessToken = assertString(in, GITHUB_ACCESSTOKEN);
        String gitHubOrgName = assertString(in, GITHUB_ORGNAME);
        String gitHubRepoName = assertString(in, GITHUB_REPONAME);

        GitHubClient client = GitHubClient.createClient(gitHubUri);

        log.info("Creating repository " + gitHubRepoName + " in " + gitHubOrgName + " organization.");

        try {
            client.setOAuth2Token(gitHubAccessToken);

            RepositoryService repositoryService = new RepositoryService(client);
            Repository repo = getRepository(repositoryService, gitHubOrgName, gitHubRepoName);

            if (repo == null) {
                log.debug("Repository " + gitHubRepoName + " does not exist in " + gitHubOrgName +
                        " organization. " + "Proceeding with repo creation");

                Repository newRepo = new Repository();
                newRepo.setName(gitHubRepoName);
                repo = repositoryService.createRepository(gitHubOrgName, newRepo);

                log.info("Repository " + gitHubRepoName + " created successfully in " +
                        gitHubOrgName + " organization.");
            } else {
                log.warn("Repository " + repo.generateId() + " already exists. Skipping creation ...");
            }

            Map<String, Object> result = new HashMap<>();
            result.put("cloneUrl", repo.getCloneUrl());
            result.put("scmUrl", repo.getUrl());
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Error occured while creating repository: ", e);
        }
    }

    private static Map<String, Object> deleteRepo(Map<String, Object> in, String gitHubUri) {
        String gitHubAccessToken = assertString(in, GITHUB_ACCESSTOKEN);
        String gitHubOrgName = assertString(in, GITHUB_ORGNAME);
        String gitHubRepoName = assertString(in, GITHUB_REPONAME);

        GitHubClient client = GitHubClient.createClient(gitHubUri);

        log.info("Deleting repository " + gitHubRepoName + " from " + gitHubOrgName + " organization.");

        try {
            client.setOAuth2Token(gitHubAccessToken);

            RepositoryService repositoryService = new RepositoryService(client);
            Repository repo = getRepository(repositoryService, gitHubOrgName, gitHubRepoName);

            if (repo == null) {
                log.warn("Repository " + gitHubOrgName + "/" + gitHubRepoName + " does not exist. " +
                        "Looks like it is already deleted. Skipping deletion ...");
            } else {
                log.debug("Repository " + gitHubRepoName + " exists in " + gitHubOrgName +
                        " organization. " + "Proceeding with repo deletion");

                repositoryService.deleteRepository(repo);
                log.info("Repository " + gitHubRepoName + " deleted successfully from " +
                        gitHubOrgName + " organization.");
            }

            return Collections.emptyMap();
        } catch (Exception e) {
            throw new RuntimeException("Error occured while deleting repository: ", e);
        }
    }

    private static Map<String, Object> getLatestSHA(Map<String, Object> in, String gitHubUri) {
        String gitHubAccessToken = assertString(in, GITHUB_ACCESSTOKEN);
        String gitHubOrgName = assertString(in, GITHUB_ORGNAME);
        String gitHubRepoName = assertString(in, GITHUB_REPONAME);
        String gitHubBranchName = getString(in, GITHUB_BRANCH, "master");

        GitHubClient client = GitHubClient.createClient(gitHubUri);

        try {
            //Connect to GitHub
            client.setOAuth2Token(gitHubAccessToken);
            IRepositoryIdProvider repo = RepositoryId.create(gitHubOrgName, gitHubRepoName);

            // get SHA of latest commit
            RepositoryService repoService = new RepositoryService(client);
            log.info("Getting latest commit SHA for '{}/{}/{}'...", gitHubOrgName, gitHubRepoName, gitHubBranchName);
            List<RepositoryBranch> branches = repoService.getBranches(repo);

            String latestCommitSHA = branches.stream()
                    .filter(b -> b.getName().equals(gitHubBranchName))
                    .findFirst()
                    .map(b -> b.getCommit().getSha())
                    .orElseThrow(() -> new RuntimeException("Branch " + "'" + gitHubBranchName + "'" + " not found"));

            log.info("Latest commit SHA: '{}'", latestCommitSHA);

            return Collections.singletonMap("latestCommitSHA", latestCommitSHA);
        } catch (Exception e) {
            throw new RuntimeException("Error occured while getting latest commit SHA: " + e.getMessage());
        }
    }

    private static Repository getRepository(RepositoryService repositoryService, String githubOrgName,
                                            String gitHubRepoName) throws Exception {
        Repository repository = null;

        try {
            repository = repositoryService.getRepository(githubOrgName, gitHubRepoName);
        } catch (RequestException rexp) {
            // If the status code of the request exception does not point to repository not being found,
            // throw the exception, as we don't want to handle other errors.
            // If the status code is 404, set repository to null (set by default).

            if (rexp.getStatus() != STATUS_GITHUB_REPO_NOT_FOUND) {
                throw rexp;
            }
        }

        return repository;
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

    private static Map<String, Object> makeResult(Object data) {
        Map<String, Object> m = new HashMap<>();
        m.put("ok", true);
        m.put("data", data);
        return m;
    }

    public enum Action {
        CREATEPR,
        COMMENTPR,
        MERGEPR,
        CLOSEPR,
        MERGE,
        CREATETAG,
        DELETETAG,
        DELETEBRANCH,
        GETCOMMIT,
        ADDSTATUS,
        GETSTATUSES,
        FORKREPO,
        GETBRANCHLIST,
        GETPRLIST,
        GETTAGLIST,
        GETLATESTSHA,
        CREATEREPO,
        DELETEREPO
    }
}
