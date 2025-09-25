package com.walmartlabs.concord.plugins.git;

import com.walmartlabs.concord.plugins.git.client.GitHubClient;
import com.walmartlabs.concord.plugins.git.model.GitHubApiInfo;

import java.util.Map;
import java.util.UUID;

public abstract class GitHubTaskAction<T extends GitHubTaskParams> {

    public abstract Action action();

    public abstract Map<String, Object> execute(UUID txId, GitHubApiInfo apiInfo, T input) throws Exception;

    protected GitHubClient createClient(UUID txId, GitHubApiInfo apiInfo) {
        return new GitHubClient(txId, apiInfo);
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
        GETCONTENT,
        CREATEAPPTOKEN,
        GETSHORTSHA
    }
}
