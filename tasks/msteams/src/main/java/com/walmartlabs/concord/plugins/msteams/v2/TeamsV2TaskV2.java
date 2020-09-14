package com.walmartlabs.concord.plugins.msteams.v2;

import com.walmartlabs.concord.plugins.msteams.*;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.Task;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import com.walmartlabs.concord.runtime.v2.sdk.Variables;

import javax.inject.Inject;
import javax.inject.Named;

@Named("msteamsV2")
public class TeamsV2TaskV2 implements Task {

    private final Context context;

    private final TeamsV2TaskCommon delegate = new TeamsV2TaskCommon();

    @Inject
    public TeamsV2TaskV2(Context context) {
        this.context = context;
    }

    @Override
    public TaskResult execute(Variables input) {
        Result r = delegate.execute(TeamsV2TaskParams.of(input, context.defaultVariables().toMap()));

        TaskResult result = new TaskResult(r.isOk(), r.getError())
                .value("data", r.getData());

        if (r.getActivityId() != null && r.getConversationId().contains(r.getActivityId())) {
            result.value("conversationId", r.getConversationId())
                    .value("activityId", r.getActivityId());
        }

        return result;
    }
}
