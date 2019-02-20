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
import com.walmartlabs.concord.sdk.Context;
import com.walmartlabs.concord.sdk.InjectVariable;
import com.walmartlabs.concord.sdk.Task;
import org.eclipse.egit.github.core.*;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.CommitService;
import org.eclipse.egit.github.core.service.DataService;
import org.eclipse.egit.github.core.service.IssueService;
import org.eclipse.egit.github.core.service.PullRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import java.io.IOException;
import java.util.*;

import static com.walmartlabs.concord.plugins.git.Utils.getUrl;
import static com.walmartlabs.concord.sdk.ContextUtils.*;

/**
 * Created by ppendha on 5/22/18.
 */
@Named("github")
public class GitHubTask implements Task {

    private static final Logger log = LoggerFactory.getLogger(GitHubTask.class);

    private static final String API_URL_KEY = "apiUrl";
    private static final String ACTION_KEY = "action";
    private static final String GITHUB_ACCESSTOKEN = "accessToken";
    private static final String GITHUB_ORGNAME = "org";
    private static final String GITHUB_REPONAME = "repo";
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
    private static final String GITHUB_COMMIT_SHA = "commitSHA";
    private static final String GITHUB_TAG_REFS = "refs/tags/";
    private static final String GITHUB_MERGEHEAD = "head";
    private static final String GITHUB_MERGEBASE = "base";
    private static final String GITHUB_MERGECOMMITMSG = "commitMessage";

    private static final String STATUS_CHECK_STATE = "state";
    private static final String STATUS_CHECK_TARGET_URL = "targetUrl";
    private static final String STATUS_CHECK_DESCRIPTION = "description";
    private static final String STATUS_CHECK_CONTEXT = "context";


    @InjectVariable("githubParams")
    private Map<String, Object> defaults;

    @Override
    public void execute(Context ctx) throws Exception {
        Action action = getAction(ctx);
        String gitHubUri = getUrl(defaults, ctx, API_URL_KEY);
        log.info("Starting '{}' action...", action);
        log.info("Using GitHub apiUrl {}", gitHubUri);

        switch (action) {
            case CREATEPR: {
                createPR(ctx, gitHubUri);
                break;
            }
            case COMMENTPR: {
                commentPR(ctx, gitHubUri);
                break;
            }
            case MERGEPR: {
                mergePR(ctx, gitHubUri);
                break;
            }
            case CLOSEPR: {
                closePR(ctx, gitHubUri);
                break;
            }
            case CREATETAG: {
                createTag(ctx, gitHubUri);
                break;
            }
            case MERGE: {
                merge(ctx, gitHubUri);
                break;
            }
            case GETCOMMIT: {
                getCommit(ctx, gitHubUri);
                break;
            }
            case ADDSTATUS: {
                addStatus(ctx, gitHubUri);
                break;
            }
            default:
                throw new IllegalArgumentException("Unsupported action type: " + action);
        }
    }

    private static void createPR(Context ctx, String gitHubUri) throws Exception {
        String gitHubAccessToken = assertString(ctx, GITHUB_ACCESSTOKEN);
        String gitHubOrgName = assertString(ctx, GITHUB_ORGNAME);
        String gitHubRepoName = assertString(ctx, GITHUB_REPONAME);
        String gitHubPRTitle = assertString(ctx, GITHUB_PRTITLE);
        String gitHubPRBody = assertString(ctx, GITHUB_PRBODY);
        String gitHubPRHead = assertString(ctx, GITHUB_PRHEAD);
        String gitHubPRBase = assertString(ctx, GITHUB_PRBASE);

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
                ctx.setVariable("prId", result.getNumber());
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot create a pull request: " + e.getMessage());
        }
    }

    private static void commentPR(Context ctx, String gitHubUri) throws Exception {
        String gitHubAccessToken = assertString(ctx, GITHUB_ACCESSTOKEN);
        String gitHubOrgName = assertString(ctx, GITHUB_ORGNAME);
        String gitHubRepoName = assertString(ctx, GITHUB_REPONAME);
        String gitHubPRComment = assertString(ctx, GITHUB_PRCOMMENT);
        int gitHubPRID = assertInt(ctx, GITHUB_PRID);

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
        } catch (IOException e) {
            throw new RuntimeException("Cannot comment on the pull request: " + e.getMessage());
        }
    }

    private static void mergePR(Context ctx, String gitHubUri) throws Exception {
        String gitHubAccessToken = assertString(ctx, GITHUB_ACCESSTOKEN);
        String gitHubOrgName = assertString(ctx, GITHUB_ORGNAME);
        String gitHubRepoName = assertString(ctx, GITHUB_REPONAME);
        int gitHubPRID = assertInt(ctx, GITHUB_PRID);

        GitHubClient client = GitHubClient.createClient(gitHubUri);
        client.setOAuth2Token(gitHubAccessToken);
        IRepositoryIdProvider repo = RepositoryId.create(gitHubOrgName, gitHubRepoName);

        PullRequestService prService = new PullRequestService(client);
        try {
            log.info("Using PR# {}", gitHubPRID);
            prService.merge(repo, gitHubPRID, "GitHub PR Merge");
            log.info("Merged PR# {}", gitHubPRID);
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot merge the pull request: " + e.getMessage());
        }
    }

    private static void closePR(Context ctx, String gitHubUri) throws Exception {
        String gitHubAccessToken = assertString(ctx, GITHUB_ACCESSTOKEN);
        String gitHubOrgName = assertString(ctx, GITHUB_ORGNAME);
        String gitHubRepoName = assertString(ctx, GITHUB_REPONAME);
        int gitHubPRID = assertInt(ctx, GITHUB_PRID);

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
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot close the pull request: " + e.getMessage());
        }

    }

    private static void merge(Context ctx, String gitHubUri) throws Exception {
        String gitHubAccessToken = assertString(ctx, GITHUB_ACCESSTOKEN);
        String gitHubOrgName = assertString(ctx, GITHUB_ORGNAME);
        String gitHubRepoName = assertString(ctx, GITHUB_REPONAME);
        String head = assertString(ctx, GITHUB_MERGEHEAD);
        String base = assertString(ctx, GITHUB_MERGEBASE);
        String commitMessage = assertString(ctx, GITHUB_MERGECOMMITMSG);

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
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot merge 'base' and 'head': " + e.getMessage());
        }
    }

    private static void getCommit(Context ctx, String gitHubUri) throws Exception {
        String gitHubAccessToken = assertString(ctx, GITHUB_ACCESSTOKEN);
        String gitHubOrgName = assertString(ctx, GITHUB_ORGNAME);
        String gitHubRepoName = assertString(ctx, GITHUB_REPONAME);
        String gitHubCommitSha = assertString(ctx, GITHUB_COMMIT_SHA);

        GitHubClient client = GitHubClient.createClient(gitHubUri);
        client.setOAuth2Token(gitHubAccessToken);
        IRepositoryIdProvider repo = RepositoryId.create(gitHubOrgName, gitHubRepoName);

        CommitService commitService = new CommitService(client);

        try {
            log.info("Getting commit {}", gitHubCommitSha);
            RepositoryCommit repositoryCommit = commitService.getCommit(repo, gitHubCommitSha);

            Object data = new ObjectMapper().convertValue(repositoryCommit, Map.class);
            ctx.setVariable("result", makeResult(data));
        } catch (IOException e) {
            throw new RuntimeException("Failed to get commit data: " + e.getMessage());
        }
    }

    private static void createTag(Context ctx, String gitHubUri) throws Exception {
        String gitHubAccessToken = assertString(ctx, GITHUB_ACCESSTOKEN);
        String gitHubOrgName = assertString(ctx, GITHUB_ORGNAME);
        String gitHubRepoName = assertString(ctx, GITHUB_REPONAME);
        String gitHubTagVersion = assertString(ctx, GITHUB_TAGVERSION);
        String gitHubTagMessage = assertString(ctx, GITHUB_TAGMESSAGE);
        String gitHubTaggerUID = assertString(ctx, GITHUB_TAGGERUID);
        String gitHubTaggerEMAIL = assertString(ctx, GITHUB_TAGGEREMAIL);
        String gitHubBranchSHA = assertString(ctx, GITHUB_COMMIT_SHA);

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
            throw new IllegalArgumentException("Can't create a tag: " + e.getMessage());
        }

        Reference tagRef = new Reference();
        tagRef.setRef(GITHUB_TAG_REFS.concat(gitHubTagVersion));
        tagRef.setObject(typedResource);

        try {
            //Create Tag Reference
            dataService.createReference(repo, tagRef);
            log.info("Successfully Created TAG Reference {}", tagRef.getRef());
        } catch (IOException e) {
            throw new IllegalArgumentException("Can't create a tag reference: " + e.getMessage());
        }
    }

    private static void addStatus(Context ctx, String gitHubUri) {
        String gitHubAccessToken = assertString(ctx, GITHUB_ACCESSTOKEN);
        String gitHubOrgName = assertString(ctx, GITHUB_ORGNAME);
        String gitHubRepoName = assertString(ctx, GITHUB_REPONAME);
        String commitSha = assertString(ctx, GITHUB_COMMIT_SHA);

        String state = assertStatusState(ctx);
        String targetUrl = getString(ctx, STATUS_CHECK_TARGET_URL, null);
        String description = getString(ctx, STATUS_CHECK_DESCRIPTION, null);
        String context = getString(ctx, STATUS_CHECK_CONTEXT, "default");

        log.info("Creating status check ({}) for {}/{} repo with sha '{}'",
                state, gitHubOrgName, gitHubRepoName, commitSha);

        GitHubClient client = GitHubClient.createClient(gitHubUri);
        try {
            client.setOAuth2Token(gitHubAccessToken);

            IRepositoryIdProvider repo = RepositoryId.create(gitHubOrgName, gitHubRepoName);

            String id = repo.generateId();
            String uri = "/repos/" + id + "/statuses/" + commitSha;

            Map<String, String> params = new HashMap<>();
            params.put("state", state.toLowerCase());
            params.put("target_url", targetUrl);
            params.put("description", description);
            params.put("context", context);

            client.post(uri, params, null);

            log.info("Status check created");
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot create a status check request: " + e.getMessage());
        }
    }

    private static Action getAction(Context ctx) {
        Object v = ctx.getVariable(ACTION_KEY);
        if (v instanceof String) {
            String s = (String) v;
            return Action.valueOf(s.trim().toUpperCase());
        }
        throw new IllegalArgumentException("'" + ACTION_KEY + "' must be a string");
    }

    private static Set<String> STATUS_CHECK_STATES = new HashSet<>(Arrays.asList("error", "failure", "pending", "success"));

    private static String assertStatusState(Context ctx) {
        String state = assertString(ctx, STATUS_CHECK_STATE).trim().toLowerCase();
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
        GETCOMMIT,
        ADDSTATUS
    }
}
