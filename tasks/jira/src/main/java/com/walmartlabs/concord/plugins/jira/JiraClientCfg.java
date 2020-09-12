package com.walmartlabs.concord.plugins.jira;

public interface JiraClientCfg {

    default long connectTimeout() {
        return 30L;
    }

    default long readTimeout() {
        return 30L;
    }

    default long writeTimeout() {
        return 30L;
    }
}
