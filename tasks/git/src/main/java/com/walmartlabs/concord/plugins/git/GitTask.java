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

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.walmartlabs.concord.common.IOUtils;
import com.walmartlabs.concord.sdk.Constants;
import com.walmartlabs.concord.sdk.Context;
import com.walmartlabs.concord.sdk.SecretService;
import com.walmartlabs.concord.sdk.Task;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.MergeResult.MergeStatus;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.*;
import org.eclipse.jgit.transport.RemoteRefUpdate.Status;
import org.eclipse.jgit.util.FS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static com.walmartlabs.concord.plugins.git.Utils.getBoolean;
import static com.walmartlabs.concord.sdk.ContextUtils.assertString;
import static com.walmartlabs.concord.sdk.ContextUtils.getString;

@Named("git")
public class GitTask implements Task {

    private static final Logger log = LoggerFactory.getLogger(GitTask.class);

    private static final String ACTION_KEY = "action";
    private static final String GIT_AUTH_KEY = "auth";
    private static final String GIT_BASE_BRANCH = "baseBranch";
    private static final String GIT_BASIC_KEY = "basic";
    private static final String GIT_COMMIT_MSG = "commitMessage";
    private static final String GIT_COMMITTER_EMAIL = "commitEmail";
    private static final String GIT_COMMITTER_USERNAME = "commitUsername";
    private static final String GIT_DESTINATION_BRANCH = "destinationBranch";
    private static final String GIT_KEY_PATH = "keyPath";
    private static final String GIT_NEW_BRANCH_NAME = "newBranch";
    private static final String GIT_PASSWORD = "password";
    private static final String GIT_PRIVATE_KEY = "privateKey";
    private static final String GIT_PUSH_CHANGES_TO_ORIGIN = "pushChanges";
    private static final String GIT_PUSH_NEW_BRANCH_TO_ORIGIN = "pushBranch";
    private static final String GIT_SOURCE_BRANCH = "sourceBranch";
    private static final String GIT_TOKEN = "token";
    private static final String GIT_URL = "url";
    private static final String GIT_USER_NAME = "username";
    private static final String GIT_WORKING_DIR = "workingDir";
    private static final String REFS_REMOTES = "refs/remotes/origin/";
    private static final String GIT_PULL_REMOTE_BRANCH = "remoteBranch";

    private static final String OUT_KEY = "out";
    private static final String IGNORE_ERRORS_KEY = "ignoreErrors";
    private static final String DEFAULT_OUT_VAR_KEY = "result";
    private static final String CHANGE_LIST_KEY = "changeList";
    private static final String STATUS_KEY = "status";
    private static final String OK_KEY = "ok";
    private static final String ERROR_KEY = "error";

    private final SecretService secretService;

    @Inject
    public GitTask(SecretService secretService) {
        this.secretService = secretService;
    }

    @Override
    public void execute(Context ctx) throws Exception {
        Action action = getAction(ctx);
        log.info("Starting '{}' action...", action);

        switch (action) {
            case CLONE: {
                doClone(ctx);
                break;
            }
            case CREATEBRANCH: {
                doCreateNewBranch(ctx);
                break;
            }
            case MERGE: {
                doMergeNewBranch(ctx);
                break;
            }
            case COMMIT: {
                doCommit(ctx);
                break;
            }
            case PULL: {
                doPull(ctx);
                break;
            }
            default:
                throw new IllegalArgumentException("Unsupported action type: " + action);
        }
    }

    private void doClone(Context ctx) throws Exception {
        String uri = assertString(ctx, GIT_URL);

        String baseBranch = getString(ctx, GIT_BASE_BRANCH, null);
        if (baseBranch == null) {
            baseBranch = "master";
        }

        Path dstDir = prepareTargetDirectory(ctx);
        Map<String, String> transportCfg = getTransportConfig(ctx);
        TransportConfigCallback transportCallback = createTransportConfigCallback(transportCfg);

        log.info("Cloning {} to {}...", uri, dstDir);
        try {
            cloneRepo(uri, baseBranch, dstDir, transportCallback);
            setOutVariable(ctx, true, ResultStatus.SUCCESS, "", Collections.emptySet());
        } catch (Exception e) {
            String error = "Error while cloning the repository.\n" + e.getMessage();

            handleError(error, e, ctx, dstDir);
        } finally {
            cleanUp(transportCfg.get(GIT_KEY_PATH));
        }
    }

    private void doPull(Context ctx) throws Exception {
        Path dstDir = prepareTargetDirectory(ctx);
        String remoteBranch = assertString(ctx, GIT_PULL_REMOTE_BRANCH);

        Map<String, String> transportCfg = getTransportConfig(ctx);
        TransportConfigCallback transportCallback = createTransportConfigCallback(transportCfg);

        Git git = Git.open(dstDir.toFile());

        // TODO: Research if there is a way to pass uri. For now defaulting it to a name
        String remote = "origin";

        try {
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
        } catch (Exception e) {
            String error = "Error occurred during git pull action.\n" + e.getMessage();
            handleError(error, e, ctx, dstDir);
        }
    }

    private void doCommit(Context ctx) {
        Path dstDir = prepareTargetDirectory(ctx);
        String baseBranch = assertString(ctx, GIT_BASE_BRANCH);
        String commitMessage = assertString(ctx, GIT_COMMIT_MSG);
        String committerUId = assertString(ctx, GIT_COMMITTER_USERNAME);
        String committerEmail = assertString(ctx, GIT_COMMITTER_EMAIL);
        boolean pushChangesToOrigin = getBoolean(ctx, GIT_PUSH_CHANGES_TO_ORIGIN, false);
        boolean ignoreErrors = isIgnoreErrors(ctx);

        try (Git git = Git.open(dstDir.toFile())) {
            Map<String, String> transportCfg = getTransportConfig(ctx);
            TransportConfigCallback transportCallback = createTransportConfigCallback(transportCfg);

            log.info("Scanning folder for changes.");
            git.add().addFilepattern(".").call();
            git.add().setUpdate(true).addFilepattern(".").call();
            org.eclipse.jgit.api.Status status = git.status().call();
            if (status.getUncommittedChanges().isEmpty()) {
                log.warn("No changes detected on your local git repo.Skipping git commit and git push actions.");
                setOutVariable(ctx, true, ResultStatus.NO_CHANGES, "", Collections.emptySet());
                return;
            }

            log.info("Changes detected in the following files: " + status.getUncommittedChanges());
            CommitCommand commitCommand = git.commit()
                    .setMessage(commitMessage)
                    .setCommitter(committerUId, committerEmail);
            try {
                commitCommand.call();
                log.info("Committer userid and email are '{}', '{}'", committerUId, committerEmail);
                setOutVariable(ctx, true, ResultStatus.SUCCESS, "", status.getUncommittedChanges());
            } catch (Exception e) {
                String error = "Problem committing changes.\n" + e.getMessage();
                if (!ignoreErrors) {
                    throw new IllegalArgumentException(error, e);
                }
                setOutVariable(ctx, false, ResultStatus.FAILURE, error, status.getUncommittedChanges());
                return;
            }

            if (!pushChangesToOrigin) {
                log.warn("Skipping push operation as 'pushChanges' parameter is set to 'false' by default. If you want to push the changes to origin, set it to 'true' in your git commit action");
                return;
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

                        setOutVariable(ctx, false, ResultStatus.FAILURE, error, status.getUncommittedChanges());
                        break;
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

                        setOutVariable(ctx, false, ResultStatus.FAILURE, error, status.getUncommittedChanges());
                        break;
                    }
                    case UP_TO_DATE: {
                        String error = "Everything up-to-date. Nothing to push to origin. Status Code:" + pushStatus;
                        if (!ignoreErrors) {
                            throw new IllegalArgumentException(error);
                        }

                        setOutVariable(ctx, false, ResultStatus.FAILURE, error, status.getUncommittedChanges());
                        break;
                    }
                    case OK: {
                        log.info("Successfully pushed the changes to origin");
                        setOutVariable(ctx, true, ResultStatus.SUCCESS, "", status.getUncommittedChanges());
                        break;
                    }
                }
            }
        } catch (Exception e) {
            String error = "Exception occurred while accessing the git repo or while pushing the changes to origin\n" + e.getMessage();
            if (!ignoreErrors) {
                throw new IllegalArgumentException(error, e);
            }

            setOutVariable(ctx, false, ResultStatus.FAILURE, error, Collections.emptySet());
        }
    }

    private void doCreateNewBranch(Context ctx) throws Exception {
        String uri = assertString(ctx, GIT_URL);

        String baseBranch = getString(ctx, GIT_BASE_BRANCH, null);
        if (baseBranch == null) {
            baseBranch = "master";
            log.info("User input for 'baseBranch' parameter is not provided. The default 'master' branch is used as the base to create the new branch.");
        }

        //New Branch variables
        String newBranchName = assertString(ctx, GIT_NEW_BRANCH_NAME);
        boolean pushNewBranchToOrigin = getBoolean(ctx, GIT_PUSH_NEW_BRANCH_TO_ORIGIN, false);

        Path dstDir = prepareTargetDirectory(ctx);

        Map<String, String> transportCfg = getTransportConfig(ctx);
        TransportConfigCallback transportCallback = createTransportConfigCallback(transportCfg);

        log.info("Cloning {} to {}...", uri, dstDir);
        try {
            Git git = cloneRepo(uri, baseBranch, dstDir, transportCallback);
            git.checkout().setCreateBranch(true).setName(newBranchName).call();
            log.info("Created new branch '{}'", newBranchName);
            //Push created Branch to remote Origin on user input
            if (pushNewBranchToOrigin) {
                PushCommand cmd = git.push();
                cmd.setTransportConfigCallback(transportCallback)
                        .setRemote("origin")
                        .setRefSpecs(new RefSpec(newBranchName))
                        .call();
                log.info("Pushed '{}' to the remote's origin", newBranchName);
            } else {
                log.warn("Skipping push operation as 'pushBranch' parameter is set to 'false' by default. If you want to push your new branch to origin, set it to 'true' in your git createBranch action");
            }
            setOutVariable(ctx, true, ResultStatus.SUCCESS, "", Collections.emptySet());
        } catch (Exception e) {
            String error = "Error while cloning the repository.\n" + e.getMessage();
            handleError(error, e, ctx, dstDir);
        } finally {
            cleanUp(transportCfg.get(GIT_KEY_PATH));
        }

        log.info("Done");
    }

    private void doMergeNewBranch(Context ctx) throws Exception {
        String uri = assertString(ctx, GIT_URL);
        String sourceBranch = assertString(ctx, GIT_SOURCE_BRANCH);
        String destinationBranch = assertString(ctx, GIT_DESTINATION_BRANCH);

        Path dstDir = prepareTargetDirectory(ctx);

        Map<String, String> transportCfg = getTransportConfig(ctx);
        TransportConfigCallback transportCallback = createTransportConfigCallback(transportCfg);

        log.info("Cloning {} to {}...", uri, dstDir);
        try {
            //Merge Branch and Push to Origin if there are no conflicts
            String sourceBranch_ref = REFS_REMOTES.concat(sourceBranch);
            Git git = cloneRepo(uri, destinationBranch, dstDir, transportCallback);
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
                    if (!isIgnoreErrors(ctx)) {
                        throw new IllegalAccessException(error);
                    }
                    setOutVariable(ctx, false, ResultStatus.FAILURE, error, Collections.emptySet());
                    break;
                }

                case ALREADY_UP_TO_DATE: {
                    setOutVariable(ctx, true, ResultStatus.NO_CHANGES, "", Collections.emptySet());
                    log.info("Branch already up-to-date. No merging required");
                    break;
                }
                default: {
                    log.info("Merged '{}' with '{}'", sourceBranch, destinationBranch);
                    //Push to Origin
                    PushCommand pushCommand = git.push();
                    pushCommand.setTransportConfigCallback(transportCallback)
                            .setRemote("origin")
                            .setRefSpecs(new RefSpec(destinationBranch))
                            .call();
                    setOutVariable(ctx, true, ResultStatus.SUCCESS, "", Collections.emptySet());
                    log.info("Pushed '{}' to the remote's origin", destinationBranch);
                    break;
                }
            }
        } catch (Exception e) {
            String error = "Error while cloning a repository\n" + e.getMessage();
            handleError(error, e, ctx, dstDir);
        } finally {
            cleanUp(transportCfg.get(GIT_KEY_PATH));
        }
    }

    private Path prepareTargetDirectory(Context ctx) {
        String s = getString(ctx, GIT_WORKING_DIR, null);
        if (s == null) {
            String url = assertString(ctx, GIT_URL);

            final int slashIndex = url.lastIndexOf("/");
            final int dotGitIndex = url.lastIndexOf(".git");
            s = url.substring(slashIndex + 1, (dotGitIndex > slashIndex + 1 ? dotGitIndex : url.length() - 1));
        }

        String workDir = (String) ctx.getVariable(Constants.Context.WORK_DIR_KEY);
        Path p = Paths.get(workDir, s);
        if (!Files.exists(p)) {
            try {
                Files.createDirectories(p);
            } catch (IOException e) {
                throw new IllegalArgumentException("Can't create a directory for the repository", e);
            }
        }

        return p;
    }

    @SuppressWarnings("unchecked")
    private Path exportPrivateKey(Context ctx) throws Exception {
        Object o = ctx.getVariable(GIT_PRIVATE_KEY);
        if (o == null) {
            return null;
        }
        if (o instanceof Map) {
            Map<String, Object> m = (Map<String, Object>) o;
            String secretName = (String) m.get("secretName");
            if (secretName == null) {
                throw new IllegalArgumentException("Secret name is required to export a private key");
            }

            String orgName = (String) m.get("org");
            String pwd = (String) m.get("password");

            log.info("Using secret: {}", (orgName != null ? orgName + "/" + secretName : secretName));

            String txId = (String) ctx.getVariable(Constants.Context.TX_ID_KEY);
            String workDir = (String) ctx.getVariable(Constants.Context.WORK_DIR_KEY);

            Map<String, String> keyPair = secretService.exportKeyAsFile(ctx, txId, workDir, orgName, secretName, pwd);
            Files.delete(Paths.get(keyPair.get("public")));

            Path p = Paths.get(keyPair.get("private"));
            return p.toAbsolutePath();
        }

        throw new IllegalArgumentException("Mandatory parameter '" + GIT_PRIVATE_KEY + "' is missing");
    }

    private Git cloneRepo(String uri, String branchName, Path dst, TransportConfigCallback transportCallback) throws Exception {
        Git repo = Git.cloneRepository()
                .setURI(uri)
                .setBranch(branchName)
                .setDirectory(dst.toFile())
                .setTransportConfigCallback(transportCallback)
                .call();

        // check if the branch actually exists
        if (branchName != null) {
            repo.checkout()
                    .setName(branchName)
                    .call();
        }

        return repo;
    }

    private void cleanUp(String path) throws IOException {
        if (path != null) {
            Files.delete(Paths.get(path));
        }
    }

    private Map<String, String> getTransportConfig(Context ctx) throws Exception {
        Map<String, String> cfg = new HashMap<>();

        Path keyPath = exportPrivateKey(ctx);
        if (keyPath != null) {
            cfg.put(GIT_KEY_PATH, keyPath.toString());
            return cfg;
        }

        return getBasicAuthorization(ctx);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> getBasicAuthorization(Context ctx) {
        Map<String, Object> authMap = (Map<String, Object>) ctx.getVariable(GIT_AUTH_KEY);
        if (authMap == null) {
            return Collections.EMPTY_MAP;
        }

        Map<String, String> credentials = (Map<String, String>) authMap.get(GIT_BASIC_KEY);
        if (credentials == null) {
            return Collections.EMPTY_MAP;
        }

        String usernameOrToken;
        String passwd;

        if (credentials.get(GIT_TOKEN) != null) {
            usernameOrToken = credentials.get(GIT_TOKEN);
            passwd = "";
        } else {
            usernameOrToken = credentials.getOrDefault(GIT_USER_NAME, "");
            passwd = credentials.getOrDefault(GIT_PASSWORD, "");
        }

        Map<String, String> cfg = new HashMap<>();
        cfg.put(GIT_USER_NAME, usernameOrToken);
        cfg.put(GIT_PASSWORD, passwd);
        return cfg;
    }

    private static TransportConfigCallback createTransportConfigCallback(Map<String, String> cfg) {

        if (cfg.get(GIT_KEY_PATH) != null) {
            return createSshTransportConfigCallback(cfg.get(GIT_KEY_PATH));
        }

        if (cfg.get(GIT_USER_NAME) != null) {
            return createHttpTransportConfigCallback(cfg.get(GIT_USER_NAME), cfg.get(GIT_PASSWORD));
        }

        // empty callback
        return transport -> {
        };

    }

    private static TransportConfigCallback createSshTransportConfigCallback(String pkey) {
        return transport -> {
            if (transport instanceof SshTransport) {
                configureSshTransport((SshTransport) transport, pkey);
            } else {
                throw new IllegalArgumentException("Use SSH GIT URL ");
            }
        };
    }

    private static TransportConfigCallback createHttpTransportConfigCallback(String username, String password) {
        return transport -> {
            if (transport instanceof HttpTransport) {
                transport.setCredentialsProvider(
                        new UsernamePasswordCredentialsProvider(username, password));
            } else {
                throw new IllegalArgumentException("Use HTTP(S) GIT URL ");
            }
        };
    }

    private static void configureSshTransport(SshTransport t, String pkey) {
        SshSessionFactory f = new JschConfigSessionFactory() {
            @Override
            protected JSch createDefaultJSch(FS fs) throws JSchException {
                JSch d = super.createDefaultJSch(fs);
                d.removeAllIdentity();
                d.addIdentity(pkey);
                log.debug("configureSshTransport -> using the supplied secret");
                return d;
            }

            @Override
            protected void configure(OpenSshConfig.Host hc, Session session) {
                Properties config = new Properties();
                config.put("StrictHostKeyChecking", "no");
                session.setConfig(config);
                log.debug("configureSshTransport -> strict host key checking is disabled");
            }
        };

        t.setSshSessionFactory(f);
    }

    private static Action getAction(Context ctx) {
        Object v = ctx.getVariable(ACTION_KEY);
        if (v instanceof String) {
            String s = (String) v;
            return Action.valueOf(s.trim().toUpperCase());
        }
        throw new IllegalArgumentException("'" + ACTION_KEY + "' must be a string");
    }

    private static boolean isIgnoreErrors(Context ctx) {
        return getBoolean(ctx, IGNORE_ERRORS_KEY, false);
    }

    private static void handleError(String error, Exception e, Context ctx, Path dstDir) {
        try {
            IOUtils.deleteRecursively(dstDir);
        } catch (Exception ex) {
            log.info("cleanup -> error: " + ex.getMessage());
        }

        if (!isIgnoreErrors(ctx)) {
            throw new IllegalArgumentException(error, e);
        }

        setOutVariable(ctx, false, ResultStatus.FAILURE, error, Collections.emptySet());
    }

    private static void setOutVariable(Context ctx, boolean ok, ResultStatus resultStatus, String error, Set<String> changeList) {
        String key = (String) ctx.getVariable(OUT_KEY);
        if (key == null) {
            key = DEFAULT_OUT_VAR_KEY;
        }

        Map<String, Object> result = new HashMap<>();
        result.put(OK_KEY, ok);
        result.put(STATUS_KEY, resultStatus);
        result.put(ERROR_KEY, error);
        result.put(CHANGE_LIST_KEY, changeList);

        ctx.setVariable(key, result);
    }

    public enum Action {
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
