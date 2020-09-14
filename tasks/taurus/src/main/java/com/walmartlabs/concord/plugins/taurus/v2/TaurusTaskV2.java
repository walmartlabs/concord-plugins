package com.walmartlabs.concord.plugins.taurus.v2;

import com.walmartlabs.concord.plugins.taurus.TaskParams;
import com.walmartlabs.concord.plugins.taurus.Taurus;
import com.walmartlabs.concord.plugins.taurus.TaurusTaskCommon;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.Task;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import com.walmartlabs.concord.runtime.v2.sdk.Variables;

import javax.inject.Inject;
import javax.inject.Named;

@Named("taurus")
public class TaurusTaskV2 implements Task {

    private final Context context;
    private final TaurusTaskCommon delegate;

    @Inject
    public TaurusTaskV2(Context context) {
        this.context = context;
        this.delegate = new TaurusTaskCommon(context.workingDirectory());
    }

    @Override
    public TaskResult execute(Variables input) throws Exception {
        Taurus.Result result = delegate.execute(TaskParams.of(input, context.defaultVariables().toMap()));

        return new TaskResult(result.isOk(), result.getError())
                .value("code", result.getCode())
                .value("stdout", result.getStdout())
                .value("stderr", result.getStderr());
    }
}
