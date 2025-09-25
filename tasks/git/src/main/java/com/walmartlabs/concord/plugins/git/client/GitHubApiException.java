package com.walmartlabs.concord.plugins.git.client;

import java.io.IOException;

public class GitHubApiException extends IOException {

    private final int statusCode;

    public GitHubApiException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
