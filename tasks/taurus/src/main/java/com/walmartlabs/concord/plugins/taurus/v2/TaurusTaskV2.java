package com.walmartlabs.concord.plugins.taurus.v2;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2020 Walmart Inc., Concord Authors
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

import com.walmartlabs.concord.plugins.taurus.*;
import com.walmartlabs.concord.runtime.v2.sdk.*;

import javax.inject.Inject;
import javax.inject.Named;
import java.net.URI;

@Named("taurus")
public class TaurusTaskV2 implements Task {

    private final Context context;
    private final TaurusTaskCommon delegate;
    private final DependencyManager dependencyManager;
    private final TaurusDockerService dockerService;

    @Inject
    public TaurusTaskV2(Context context, DependencyManager dependencyManager, DockerService dockerService) {
        this.context = context;
        this.delegate = new TaurusTaskCommon(context.workingDirectory());
        this.dependencyManager = dependencyManager;
        this.dockerService = (spec, logOut, logErr) -> dockerService.start(DockerContainerSpec.builder()
                        .image(spec.image())
                        .args(spec.args())
                        .debug(spec.debug())
                        .forcePull(spec.forcePull())
                        .env(spec.env())
                        .workdir(spec.pwd().toString())
                        .redirectErrorStream(false)
                        .options(DockerContainerSpec.Options.builder()
                                .hosts(spec.extraDockerHosts())
                                .build())
                        .pullRetryCount(spec.pullRetryCount())
                        .pullRetryInterval(spec.pullRetryInterval())
                        .build(),
                logOut::onLog,
                logErr::onLog);
    }

    @Override
    public TaskResult execute(Variables input) throws Exception {
        BinaryResolver binaryResolver = new BinaryResolver(url -> dependencyManager.resolve(URI.create(url)));

        Taurus.Result result = delegate.execute(TaskParams.of(input, context.defaultVariables().toMap()),
                                                binaryResolver, dockerService);

        return TaskResult.of(result.isOk(), result.getError())
                .value("code", result.getCode())
                .value("stdout", result.getStdout())
                .value("stderr", result.getStderr());
    }
}
