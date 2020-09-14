package com.walmartlabs.concord.plugins.msteams.v2;

import com.walmartlabs.concord.plugins.msteams.Result;
import com.walmartlabs.concord.plugins.msteams.TeamsTaskCommon;
import com.walmartlabs.concord.plugins.msteams.TeamsTaskParams;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.Task;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import com.walmartlabs.concord.runtime.v2.sdk.Variables;

import javax.inject.Inject;
import javax.inject.Named;

@Named("msteams")
public class TeamsTaskV2 implements Task {

    private final Context context;

    private final TeamsTaskCommon delegate = new TeamsTaskCommon();

    @Inject
    public TeamsTaskV2(Context context) {
        this.context = context;
    }

    @Override
    public TaskResult execute(Variables input) {
        Result r = delegate.execute(TeamsTaskParams.of(input, context.defaultVariables().toMap()));

        return new TaskResult(r.isOk(), r.getError())
                .value("data", r.getData());
    }
}
