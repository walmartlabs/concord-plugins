package com.walmartlabs.concord.plugins.codecoverage;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2024 Walmart Inc.
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

import com.walmartlabs.concord.common.IOUtils;
import com.walmartlabs.concord.runtime.v2.ProcessDefinitionUtils;
import com.walmartlabs.concord.runtime.v2.model.FlowCall;
import com.walmartlabs.concord.runtime.v2.model.ProcessDefinition;
import com.walmartlabs.concord.runtime.v2.model.Step;
import com.walmartlabs.concord.runtime.v2.runner.PersistenceService;
import com.walmartlabs.concord.runtime.v2.runner.vm.ElementEventProducer;
import com.walmartlabs.concord.runtime.v2.runner.vm.FlowCallCommand;
import com.walmartlabs.concord.runtime.v2.runner.vm.TaskCallCommand;
import com.walmartlabs.concord.runtime.v2.sdk.WorkingDirectory;
import com.walmartlabs.concord.svm.*;
import com.walmartlabs.concord.svm.Runtime;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

public class CodeCoverage implements ExecutionListener {

    public static final Logger log = LoggerFactory.getLogger(CodeCoverage.class);

    private static final String COVERAGE_INFO_FILENAME = "coverage.info";
    private static final String FLOWS_FILENAME = "flows.zip";

    private final StepsRecorder steps;
    private final PersistenceService persistenceService;
    private final Path workDir;

    @Inject
    public CodeCoverage(StepsRecorder steps, PersistenceService persistenceService, WorkingDirectory workingDirectory) {
        this.steps = steps;
        this.persistenceService = persistenceService;
        this.workDir = workingDirectory.getValue();
    }

    @Override
    public void beforeProcessStart(Runtime runtime, State state) {
        saveFlows(runtime.getService(ProcessDefinition.class));
    }

    @Override
    public Result beforeCommand(Runtime runtime, VM vm, State state, ThreadId threadId, Command cmd) {
        // we need the name of the flow, so we can handle the call step only in `afterCommand`
        if (cmd instanceof FlowCallCommand) {
            return Result.CONTINUE;
        }

        if (cmd instanceof ElementEventProducer) {
            processStep(((ElementEventProducer) cmd).getStep(), runtime, state, threadId);
        } else if (cmd instanceof TaskCallCommand) {
            processStep(((TaskCallCommand) cmd).getStep(), runtime, state, threadId);
        }

        return Result.CONTINUE;
    }

    @Override
    public Result afterCommand(Runtime runtime, VM vm, State state, ThreadId threadId, Command cmd) {
        if (cmd instanceof FlowCallCommand) {
            processStep(((FlowCallCommand) cmd).getStep(), runtime, state, threadId);
        }
        return Result.CONTINUE;
    }

    private void processStep(Step step, Runtime runtime, State state, ThreadId threadId) {
        var loc = step.getLocation();
        if (loc == null || loc.lineNum() < 0 || loc.fileName() == null) {
            return;
        }

        var pd = runtime.getService(ProcessDefinition.class);

        steps.record(StepInfo.builder()
                .fileName(Objects.requireNonNull(loc.fileName()))
                .line(loc.lineNum())
                .processDefinitionId(ProcessDefinitionUtils.getCurrentFlowName(pd, step))
                .flowCallName(flowCallName(step, state, threadId))
                .build());
    }

    @Override
    public void onProcessError(Runtime runtime, State state, Exception e) {
        generateReport(runtime);
    }

    @Override
    public void afterProcessEnds(Runtime runtime, State state, Frame lastFrame) {
        if (isSuspended(state)) {
            return;
        }

        generateReport(runtime);
    }

    private void generateReport(Runtime runtime) {
        log.info("Generating code coverage info...");

        try {
            var reportProducer = new LcovReportProducer(runtime.getService(ProcessDefinition.class));

            steps.stream().forEach(reportProducer::onStep);
            steps.cleanup();

            persistenceService.persistFile(COVERAGE_INFO_FILENAME, reportProducer::produce, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception e) {
            throw new RuntimeException("Can't generate code coverage report", e);
        }

        log.info("Coverage info saved as attachment with name '{}'", COVERAGE_INFO_FILENAME);
    }

    private static boolean isSuspended(State state) {
        return state.threadStatus().entrySet().stream()
                .anyMatch(e -> e.getValue() == ThreadStatus.SUSPENDED);
    }

    private static String flowCallName(Step step, State state, ThreadId threadId) {
        if (step instanceof FlowCall) {
            return FlowCallCommand.getFlowName(state, threadId);
        }
        return null;
    }

    private void saveFlows(ProcessDefinition processDefinition) {
        persistenceService.persistFile(FLOWS_FILENAME, out -> {
            try (ZipArchiveOutputStream zip = new ZipArchiveOutputStream(out)) {

                for (var e : processDefinition.flows().entrySet()) {
                    String flowName = e.getKey();
                    String fileName = e.getValue().location().fileName();

                    if (fileName == null) {
                        log.warn("CodeCoverage: flow '{}' has no associated file name. Skipping...", flowName);
                        continue;
                    }

                    Path file = workDir.resolve(fileName);
                    if (Files.notExists(file)) {
                        log.warn("CodeCoverage: can't save flow '{}' -> file not exists. This is most likely a bug", fileName);
                        continue;
                    }

                    try {
                        IOUtils.zipFile(zip, file, fileName);
                    } catch (IOException ex) {
                        log.error("CodeCoverage: failed to add file '{}' for flow '{}'. Error: {}", fileName, flowName, ex.getMessage());
                        throw ex;
                    }
                }
            }
        });
        log.info("CodeCoverage: flows saved as '{}' process attachment", FLOWS_FILENAME);
    }
}
