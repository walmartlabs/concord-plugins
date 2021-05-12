package com.walmartlabs.concord.plugins.git;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 Wal-Mart Store, Inc.
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

import com.walmartlabs.concord.common.IOUtils;
import com.walmartlabs.concord.common.secret.KeyPair;
import com.walmartlabs.concord.common.secret.UsernamePassword;
import com.walmartlabs.concord.sdk.MapUtils;
import com.walmartlabs.concord.sdk.Secret;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.MergeResult.MergeStatus;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.RemoteRefUpdate.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static com.walmartlabs.concord.plugins.git.Utils.getBoolean;
import static com.walmartlabs.concord.sdk.MapUtils.assertString;
import static com.walmartlabs.concord.sdk.MapUtils.getString;
import static org.eclipse.jgit.lib.Constants.R_HEADS;
import static org.eclipse.jgit.lib.Constants.R_REMOTES;

public class GitTask {

    private static final Logger log = LoggerFactory.getLogger(GitTask.class);

    public static final String ACTION_KEY = "action";
    public static final String GIT_AUTH_KEY = "auth";
    public static final String GIT_BASE_BRANCH = "baseBranch";
    public static final String GIT_BASE_REF = "baseRef";
    public static final String GIT_BASIC_KEY = "basic";
    public static final String GIT_ALLOW_EMPTY_COMMIT = "allowEmptyCommit";
    public static final String GIT_COMMIT_MSG = "commitMessage";
    public static final String GIT_COMMITTER_EMAIL = "commitEmail";
    public static final String GIT_COMMITTER_USERNAME = "commitUsername";
    public static final String GIT_DESTINATION_BRANCH = "destinationBranch";
    public static final String GIT_INIT_BRANCH = "initBranch";
    public static final String GIT_NEW_BRANCH_NAME = "newBranch";
    public static final String GIT_PASSWORD = "password";
    public static final String GIT_PRIVATE_KEY = "privateKey";
    public static final String GIT_PUSH_CHANGES_TO_ORIGIN = "pushChanges";
    public static final String GIT_PUSH_NEW_BRANCH_TO_ORIGIN = "pushBranch";
    public static final String GIT_SOURCE_BRANCH = "sourceBranch";
    public static final String GIT_TOKEN = "token";
    public static final String GIT_URL = "url";
    public static final String GIT_USER_NAME = "username";
    public static final String GIT_WORKING_DIR = "workingDir";
    public static final String REFS_REMOTES = "refs/remotes/origin/";
    public static final String GIT_PULL_REMOTE_BRANCH = "remoteBranch";
    public static final String IGNORE_ERRORS_KEY = "ignoreErrors";
    public static final String CHANGE_LIST_KEY = "changeList";
    public static final String STATUS_KEY = "status";
    public static final String OK_KEY = "ok";
    public static final String ERROR_KEY = "error";

    private final GitSecretService secretService;
    private final Path processWorkDir;

    public GitTask(GitSecretService secretService, Path processWorkDir) {
        this.secretService = secretService;
        this.processWorkDir = processWorkDir;
    }

    public Map<String, Object> execute(Map<String, Object> in) throws Exception {
        Action action = getAction(in);
        log.info("Starting '{}' action...", action);

        switch (action) {
            case INIT: {
                return doInit(in);
            }
            case CLONE: {
                return doClone(in);
            }
            case CREATEBRANCH: {
                return doCreateNewBranch(in);
            }
            case MERGE: {
                return doMergeNewBranch(in);
            }
            case COMMIT: {
                return doCommit(in);
            }
            case PULL: {
                return doPull(in);
            }
            default:
                throw new IllegalArgumentException("Unsupported action type: " + action);
        }
    }

    private Map<String, Object> doInit(Map<String, Object> in) {
        String uri = assertString(in, GIT_URL);
        String initBranch = assertString(in, GIT_INIT_BRANCH);
        String dst = getDest(in);
        Path dstDir = processWorkDir.resolve(dst);

        log.info("Initializing repository in '{}' with url '{}' and branch '{}'...", dst, uri, initBranch);

        try (Git git = Git.init()
                .setInitialBranch(initBranch)
                .setDirectory(dstDir.toFile()).call()) {
            StoredConfig config = git.getRepository().getConfig();
            config.setString("remote", "origin", "url", uri);
            config.setString("remote", "origin", "fetch", "+" + R_HEADS + "*:" + R_REMOTES + "origin" + "/*");
            config.save();

            return toResult(true, ResultStatus.SUCCESS, null, Collections.emptySet());
        } catch (Exception e) {
            String error = "Exception occurred while initializing the git repo\n" + e.getMessage();
            if (!isIgnoreErrors(in)) {
                throw new IllegalArgumentException(error, e);
            }

            return toResult(false, ResultStatus.FAILURE, error, Collections.emptySet());
        }
    }

    private Map<String, Object> doClone(Map<String, Object> in) throws Exception {
        String uri = assertString(in, GIT_URL);
        String baseBranch = getString(in, GIT_BASE_BRANCH, "master");
        String dst = getDest(in);
        Path dstDir = processWorkDir.resolve(dst);
        if (Files.exists(dstDir)) {
            throw new IllegalStateException("Destination directory '" + dst + "' already exists");
        }

        Secret secret = getSecret(in);

        GitClient client = GitClientFactory.create(in);

        log.info("Cloning {} to {}...", uri, dstDir);
        try {
            client.cloneRepo(uri, baseBranch, secret, dstDir);
            return toResult(true, ResultStatus.SUCCESS, "", Collections.emptySet());
        } catch (Exception e) {
            return handleError("Error while cloning the repository", e, in, dstDir, secret);
        }
    }

    private Map<String, Object> doPull(Map<String, Object> in) throws Exception {
        Path dstDir = prepareTargetDirectory(in);
        String remoteBranch = assertString(in, GIT_PULL_REMOTE_BRANCH);

        Secret secret = getSecret(in);
        TransportConfigCallback transportCallback = JGitClient.createTransportConfigCallback(secret);

        // TODO: Research if there is a way to pass uri. For now defaulting it to a name
        String remote = "origin";

        try (Git git = Git.open(dstDir.toFile())) {
            PullCommand pullCommand = git.pull();
            log.info("Pulling changes from remote '{}/{}'...", remote, remoteBranch);
            PullResult result = pullCommand.setRemote(remote)
                    .setRemoteBranchName(remoteBranch)
                    .setTransportConfigCallback(transportCallback)
                    .call();

            String fetchResult = result.getFetchResult().getMessages();
            MergeResult mergeResult = result.getMergeResult();
            MergeStatus mergeStatus = mergeResult.getMergeStatus();

            if (result.isSuccessful()) {
                if (!fetchResult.isEmpty()) {
                    log.info("Fetch result: '{}'", fetchResult);
                }

                log.info("Merge result: '{}'", mergeResult.toString());
                log.info("Merge status: '{}'", mergeStatus.toString().toUpperCase());

                switch (mergeStatus) {
                    case FAST_FORWARD:
                    case FAST_FORWARD_SQUASHED:
                    case MERGED_SQUASHED:
                    case MERGED: {
                        log.info("Pulled changes from remote '{}/{}'.", remote, remoteBranch);
                        break;
                    }
                    case ALREADY_UP_TO_DATE: {
                        log.info("Everything up-to-date. Nothing to pull from remote '{}/{}'.", remote, remoteBranch);
                        break;
                    }
                }
            } else {
                switch (mergeStatus) {
                    case ABORTED:
                    case NOT_SUPPORTED:
                    case CHECKOUT_CONFLICT:
                    case CONFLICTING:
                    case FAILED: {
                        if (!fetchResult.isEmpty()) {
                            log.error("Fetch result: '{}'", result.getFetchResult().getMessages());
                        }
                        log.error("Merge result: '{}'", mergeResult.toString());
                        log.error("Merge status: '{}'", mergeStatus.toString().toUpperCase());
                        throw new IllegalArgumentException("Git pull action failed. Please fix the above errors before retrying...");
                    }
                }
            }

            return Collections.emptyMap();
        } catch (Exception e) {
            return handleError("Error occurred during git pull action", e, in, dstDir, secret);
        }
    }

    private Map<String, Object> doCommit(Map<String, Object> in) throws Exception {
        Path dstDir = prepareTargetDirectory(in);
        String baseBranch = assertString(in, GIT_BASE_BRANCH);
        String commitMessage = assertString(in, GIT_COMMIT_MSG);
        String committerUId = assertString(in, GIT_COMMITTER_USERNAME);
        String committerEmail = assertString(in, GIT_COMMITTER_EMAIL);
        boolean pushChangesToOrigin = getBoolean(in, GIT_PUSH_CHANGES_TO_ORIGIN, false);
        boolean ignoreErrors = isIgnoreErrors(in);
        boolean allowEmptyCommit = getBoolean(in, GIT_ALLOW_EMPTY_COMMIT, false);

        TransportConfigCallback transportCallback = JGitClient.createTransportConfigCallback(getSecret(in));

        try (Git git = Git.open(dstDir.toFile())) {
            log.info("Scanning folder for changes.");
            git.add().addFilepattern(".").call();
            git.add().setUpdate(true).addFilepattern(".").call();
            org.eclipse.jgit.api.Status status = git.status().call();
            if (status.getUncommittedChanges().isEmpty() && !allowEmptyCommit) {
                log.warn("No changes detected on your local git repo.Skipping git commit and git push actions.");
                return toResult(true, ResultStatus.NO_CHANGES, "", Collections.emptySet());
            }

            Map<String, Object> commitResult;
            log.info("Changes detected in the following files: " + status.getUncommittedChanges());
            CommitCommand commitCommand = git.commit()
                    .setSign(false)
                    .setAllowEmpty(allowEmptyCommit)
                    .setMessage(commitMessage)
                    .setCommitter(committerUId, committerEmail);
            try {
                commitCommand.call();
                log.info("Committer userid and email are '{}', '{}'", committerUId, committerEmail);
                commitResult = toResult(true, ResultStatus.SUCCESS, "", status.getUncommittedChanges());
            } catch (Exception e) {
                String error = "Problem committing changes.\n" + e.getMessage();
                if (!ignoreErrors) {
                    throw new IllegalArgumentException(error, e);
                }
                return toResult(false, ResultStatus.FAILURE, error, status.getUncommittedChanges());
            }

            if (!pushChangesToOrigin) {
                log.warn("Skipping push operation as 'pushChanges' parameter is set to 'false' by default. If you want to push the changes to origin, set it to 'true' in your git commit action");
                return commitResult;
            }

            PushCommand cmd = git.push();

            Iterable<PushResult> results = cmd.setTransportConfigCallback(transportCallback)
                    .setRemote("origin")
                    .add(baseBranch)
                    .call();

            //Build the refName of baseBranch
            String refName = "refs/heads/" + baseBranch;

            for (PushResult result : results) {
                RemoteRefUpdate ref = result.getRemoteUpdate(refName);
                if (ref == null) {
                    throw new IllegalArgumentException("Got invalid input for '" + GIT_BASE_BRANCH + "' param: '" + baseBranch +
                            "'.\n hint: 1) Check if the input params passed for 'commit' action are valid.\n" +
                            " hint: 2) 'baseBranch' parameter should be plain branch name.\n" +
                            " hint: 3) Make sure you are pushing the changes to the same 'baseBranch', that's checked out as part of your 'clone' operation");
                }

                Status pushStatus = ref.getStatus();
                switch (pushStatus) {
                    case REJECTED_NODELETE:
                    case NON_EXISTING:
                    case NOT_ATTEMPTED:
                    case REJECTED_OTHER_REASON:
                    case REJECTED_REMOTE_CHANGED: {
                        String error = "Please fix the issues before pushing again.Git Push action failed with status code:" + pushStatus;
                        if (!ignoreErrors) {
                            throw new IllegalArgumentException(error);
                        }

                        return toResult(false, ResultStatus.FAILURE, error, status.getUncommittedChanges());
                    }
                    case REJECTED_NONFASTFORWARD: {
                        String error = "failed to push some refs to origin'\n" +
                                "hint: Updates were rejected because the remote contains work that you do\n" +
                                "hint: not have locally. This is usually caused by another repository pushing\n" +
                                "hint: to the same ref. You may want to first integrate the remote changes\n" +
                                "hint: (e.g., 'git pull ...') before pushing again. Git Push action failed with status code:" + pushStatus;
                        if (!ignoreErrors) {
                            throw new IllegalArgumentException(error);
                        }

                        return toResult(false, ResultStatus.FAILURE, error, status.getUncommittedChanges());
                    }
                    case UP_TO_DATE: {
                        String error = "Everything up-to-date. Nothing to push to origin. Status Code:" + pushStatus;
                        if (!ignoreErrors) {
                            throw new IllegalArgumentException(error);
                        }

                        return toResult(false, ResultStatus.FAILURE, error, status.getUncommittedChanges());
                    }
                    case OK: {
                        log.info("Successfully pushed the changes to origin");
                        return toResult(true, ResultStatus.SUCCESS, "", status.getUncommittedChanges());
                    }
                }
            }

            return Collections.emptyMap();
        } catch (Exception e) {
            String error = "Exception occurred while accessing the git repo or while pushing the changes to origin\n" + e.getMessage();
            if (!ignoreErrors) {
                throw new IllegalArgumentException(error, e);
            }

            return toResult(false, ResultStatus.FAILURE, error, Collections.emptySet());
        }
    }

    private Map<String, Object> doCreateNewBranch(Map<String, Object> in) throws Exception {
        String uri = assertString(in, GIT_URL);

        String baseRef = getString(in, GIT_BASE_REF, getString(in, GIT_BASE_BRANCH, "master"));

        //New Branch variables
        String newBranchName = assertString(in, GIT_NEW_BRANCH_NAME);
        boolean pushNewBranchToOrigin = getBoolean(in, GIT_PUSH_NEW_BRANCH_TO_ORIGIN, false);

        Path dstDir = prepareTargetDirectory(in);

        log.info("Cloning {} to {}...", uri, dstDir);

        Secret secret = getSecret(in);
        GitClient client = GitClientFactory.create(in);
        try {
            client.cloneRepo(uri, baseRef, secret, dstDir);
        } catch (Exception e) {
            return handleError("Error while cloning the repository", e, in, dstDir, secret);
        }

        try (Git git = Git.open(dstDir.toFile())) {
            git.checkout().setCreateBranch(true).setName(newBranchName).call();
            log.info("Created new branch '{}' from '{}'", newBranchName, baseRef);
            //Push created Branch to remote Origin on user input
            if (pushNewBranchToOrigin) {
                TransportConfigCallback transportCallback = JGitClient.createTransportConfigCallback(secret);

                PushCommand cmd = git.push();
                cmd.setTransportConfigCallback(transportCallback)
                        .setRemote("origin")
                        .setRefSpecs(new RefSpec(newBranchName))
                        .call();
                log.info("Pushed '{}' to the remote's origin", newBranchName);
            } else {
                log.warn("Skipping push operation as 'pushBranch' parameter is set to 'false' by default. If you want to push your new branch to origin, set it to 'true' in your git createBranch action");
            }
            return toResult(true, ResultStatus.SUCCESS, "", Collections.emptySet());
        } catch (Exception e) {
            return handleError( "Error while creating the branch", e, in, dstDir, secret);
        }
    }

    private Map<String, Object> doMergeNewBranch(Map<String, Object> in) throws Exception {
        String uri = assertString(in, GIT_URL);
        String sourceBranch = assertString(in, GIT_SOURCE_BRANCH);
        String destinationBranch = assertString(in, GIT_DESTINATION_BRANCH);
        Path dstDir = prepareTargetDirectory(in);

        log.info("Cloning {} to {}...", uri, dstDir);

        Secret secret = getSecret(in);

        GitClient client = GitClientFactory.create(in);
        try {
            client.cloneRepo(uri, destinationBranch, secret, dstDir);
        } catch (Exception e) {
            return handleError("Error while cloning the repository", e, in, dstDir, secret);
        }

        try (Git git = Git.open(dstDir.toFile())){
            //Merge Branch and Push to Origin if there are no conflicts
            String sourceBranch_ref = REFS_REMOTES.concat(sourceBranch);
            Repository repo = git.getRepository();

            MergeCommand cmd = git.merge();
            cmd.include(repo.findRef(sourceBranch_ref)); //Get Reference of From Branch
            MergeResult res = cmd.call();

            MergeResult.MergeStatus mergeStatus = res.getMergeStatus();
            switch (mergeStatus) {
                //Throw Exception if there are conflicts when merging
                case CONFLICTING: {
                    repo.writeMergeCommitMsg(null);
                    repo.writeMergeHeads(null);
                    Git.wrap(repo).reset().setMode(ResetCommand.ResetType.HARD).call();

                    String error = "Automatic merge failed and aborted because of conflicts. Fix the conflicts and commit the result before merging";
                    if (!isIgnoreErrors(in)) {
                        throw new IllegalAccessException(error);
                    }
                    return toResult(false, ResultStatus.FAILURE, error, Collections.emptySet());
                }

                case ALREADY_UP_TO_DATE: {
                    log.info("Branch already up-to-date. No merging required");
                    return toResult(true, ResultStatus.NO_CHANGES, "", Collections.emptySet());
                }
                default: {
                    log.info("Merged '{}' with '{}'", sourceBranch, destinationBranch);
                    //Push to Origin
                    TransportConfigCallback transportCallback = JGitClient.createTransportConfigCallback(secret);

                    PushCommand pushCommand = git.push();
                    pushCommand.setTransportConfigCallback(transportCallback)
                            .setRemote("origin")
                            .setRefSpecs(new RefSpec(destinationBranch))
                            .call();
                    log.info("Pushed '{}' to the remote's origin", destinationBranch);
                    return toResult(true, ResultStatus.SUCCESS, "", Collections.emptySet());
                }
            }
        } catch (Exception e) {
            return handleError("Error while merging a repository", e, in, dstDir, secret);
        }
    }

    private static String getDest(Map<String, Object> in) {
        String s = getString(in, GIT_WORKING_DIR, null);
        if (s != null) {
            return s;
        }

        String url = assertString(in, GIT_URL);

        int slashIndex = url.lastIndexOf("/");
        int dotGitIndex = url.lastIndexOf(".git");
        return url.substring(slashIndex + 1, (dotGitIndex > slashIndex + 1 ? dotGitIndex : url.length() - 1));
    }

    private Path prepareTargetDirectory(Map<String, Object> in) {
        Path p = processWorkDir.resolve(getDest(in));
        if (!Files.exists(p)) {
            try {
                Files.createDirectories(p);
            } catch (IOException e) {
                throw new IllegalArgumentException("Can't create a directory for the repository", e);
            }
        }

        return p;
    }

    private Path exportPrivateKey(Map<String, Object> in) throws Exception {
        Map<String, Object> m = MapUtils.getMap(in, GIT_PRIVATE_KEY, null);
        if (m == null) {
            return null;
        }

        String secretName = MapUtils.getString(m, "secretName");
        if (secretName == null) {
            throw new IllegalArgumentException("Secret name is required to export a private key");
        }

        String orgName = getString(m, "org");
        String pwd = getString(m, "password");

        log.info("Using secret: {}", (orgName != null ? orgName + "/" + secretName : secretName));

        Path privateKey = secretService.exportPrivateKeyAsFile(orgName, secretName, pwd);

        return privateKey.toAbsolutePath();
    }

    private Secret getSecret(Map<String, Object> in) throws Exception {
        Path keyPath = exportPrivateKey(in);

        if (keyPath != null) {
            byte[] privateKey = Files.readAllBytes(keyPath);
            Files.delete(keyPath);
            return new KeyPair(null, privateKey);
        }

        return getBasicAuthorization(in);
    }

    private static Secret getBasicAuthorization(Map<String, Object> in) {
        Map<String, Object> authMap = MapUtils.getMap(in, GIT_AUTH_KEY, Collections.emptyMap());
        if (authMap.isEmpty()) {
            return null;
        }

        Map<String, Object> credentials = MapUtils.getMap(authMap, GIT_BASIC_KEY, Collections.emptyMap());
        if (credentials.isEmpty()) {
            return null;
        }

        if (credentials.containsKey(GIT_TOKEN)) {
            return new TokenSecret(assertString(credentials, GIT_TOKEN));
        }

        if (credentials.containsKey(GIT_USER_NAME)) {

            return new UsernamePassword(assertString(credentials, GIT_USER_NAME), getString(credentials, GIT_PASSWORD, "").toCharArray());
        }

        return null;
    }

    private static boolean isIgnoreErrors(Map<String, Object> in) {
        return getBoolean(in, IGNORE_ERRORS_KEY, false);
    }

    private static Map<String, Object> handleError(String errorPrefix, Exception e, Map<String, Object> in, Path dstDir, Secret secret) {
        try {
            IOUtils.deleteRecursively(dstDir);
        } catch (Exception ex) {
            log.info("cleanup -> error: " + ex.getMessage());
        }

        String errorMessage = hideSensitiveData(errorPrefix + ": " + e.getMessage(), secret);
        if (!isIgnoreErrors(in)) {
            log.error("{}: {}", errorPrefix, hideSensitiveData(stacktraceToString(e), secret));
            throw new RuntimeException(errorMessage);
        }

        return toResult(false, ResultStatus.FAILURE, errorMessage, Collections.emptySet());
    }

    private static String stacktraceToString(Throwable t) {
        StringWriter w = new StringWriter();
        t.printStackTrace(new PrintWriter(w));
        return w.toString();
    }

    private static String hideSensitiveData(String s, Secret secret) {
        if (s == null) {
            return null;
        }
        if (secret instanceof UsernamePassword) {
            char[] password = ((UsernamePassword) secret).getPassword();
            if (password != null) {
                s = s.replaceAll(new String(password), "***");
            }
        } else if (secret instanceof TokenSecret) {
            String token = ((TokenSecret) secret).getToken();
            s = s.replaceAll(token, "***");
        }
        return s;
    }

    private static Map<String, Object> toResult(boolean ok, ResultStatus resultStatus, String error, Set<String> changeList) {
        Map<String, Object> result = new HashMap<>();
        result.put(OK_KEY, ok);
        result.put(STATUS_KEY, resultStatus);
        result.put(ERROR_KEY, error);
        result.put(CHANGE_LIST_KEY, changeList);
        return result;
    }

    private static Action getAction(Map<String, Object> in) {
        String v = MapUtils.assertString(in, ACTION_KEY);
        try {
            return Action.valueOf(v.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Unknown action: '" + v + "'. Available actions: " + Arrays.toString(Action.values()));
        }
    }

    public enum Action {
        INIT,
        CLONE,
        CREATEBRANCH,
        MERGE,
        COMMIT,
        PULL
    }

    public enum ResultStatus {
        SUCCESS,
        FAILURE,
        NO_CHANGES
    }
}
