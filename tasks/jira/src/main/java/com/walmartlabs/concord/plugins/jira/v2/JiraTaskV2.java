package com.walmartlabs.concord.plugins.jira.v2;

import com.walmartlabs.concord.plugins.jira.JiraCredentials;
import com.walmartlabs.concord.plugins.jira.JiraTaskCommon;
import com.walmartlabs.concord.plugins.jira.TaskParams;
import com.walmartlabs.concord.runtime.v2.sdk.*;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Map;


@Named("jira")
public class JiraTaskV2 implements Task {

    private final Context context;
    private final JiraTaskCommon delegate;

    @Inject
    public JiraTaskV2(Context context) {
        this.context = context;
        this.delegate = new JiraTaskCommon((orgName, secretName, password) -> {
            SecretService.UsernamePassword up = context.secretService().exportCredentials(orgName, secretName, password);
            return new JiraCredentials(up.username(), up.password());
        });
    }

    @Override
    public TaskResult execute(Variables input) {
        Map<String, Object> result = delegate.execute(TaskParams.of(input, context.defaultVariables().toMap()));
        return TaskResult.success()
                .values(result);
    }
}
