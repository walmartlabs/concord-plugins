package com.walmartlabs.concord.plugins.jira;

public class JiraCredentials {

    private final String username;
    private final String password;

    public JiraCredentials(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String username() {
        return username;
    }

    public String password() {
        return password;
    }
}
