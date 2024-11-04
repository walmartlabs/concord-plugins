package com.walmartlabs.concord.plugins.opentelemetry;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2024 Walmart Inc., Concord Authors
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.walmartlabs.concord.runtime.common.injector.InstanceId;
import com.walmartlabs.concord.runtime.v2.model.*;
import com.walmartlabs.concord.runtime.v2.runner.DefaultTaskVariablesService;
import com.walmartlabs.concord.runtime.v2.runner.PersistenceService;
import com.walmartlabs.concord.runtime.v2.runner.context.ContextFactory;
import com.walmartlabs.concord.runtime.v2.runner.logging.SegmentedLogger;
import com.walmartlabs.concord.runtime.v2.runner.vm.*;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.ProcessConfiguration;
import com.walmartlabs.concord.svm.Runtime;
import com.walmartlabs.concord.svm.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.UUID;

public class TelemetryCollector implements ExecutionListener {

    public static final Logger log = LoggerFactory.getLogger(TelemetryCollector.class);

    private static final String TELEMETRY_STATE_FILENAME = "opentelemetry.json";

    private static final String PARENT_STEP_ID_VARIABLE = "__opentelemetry__parent_step_id";
    private static final String FLOW_CALL_CORRELATION_ID_STEP_ID_VARIABLE = "__opentelemetry__correlation_id";
    private static final String FLOW_CALL_FRAME_VARIABLE = "__opentelemetry__flow_call_frame";

    private final ObjectMapper objectMapper;
    private final PersistenceService persistenceService;

    private final UUID instanceId;
    private final ProcessConfiguration processConfiguration;
    private final TelemetryParams params;

    private FlowSteps flowSteps;

    @Inject
    public TelemetryCollector(ObjectMapper objectMapper,
                              PersistenceService persistenceService,
                              InstanceId instanceId,
                              ProcessConfiguration processConfiguration,
                              DefaultTaskVariablesService defaultTaskVariablesService) {

        this.objectMapper = objectMapper;
        this.persistenceService = persistenceService;
        this.instanceId = instanceId.getValue();
        this.processConfiguration = processConfiguration;
        this.params = new TelemetryParams(defaultTaskVariablesService.get("opentelemetry"));
    }

    @Override
    public void beforeProcessStart(Runtime runtime, State state) {
        if (!params.enabled()) {
            return;
        }

        this.flowSteps = new FlowSteps(instanceId, getEntryPoint(runtime, state, params, processConfiguration), System.currentTimeMillis(), List.of());
    }

    @Override
    public void beforeProcessResume(Runtime runtime, State state) {
        if (!params.enabled()) {
            return;
        }

        this.flowSteps = persistenceService.loadPersistedFile(TELEMETRY_STATE_FILENAME,
                is -> objectMapper.readValue(is, FlowSteps.class));
    }

    @Override
    public Result beforeCommand(Runtime runtime, VM vm, State state, ThreadId threadId, Command cmd) {
        if (!params.enabled()) {
            return Result.CONTINUE;
        }

        if (cmd instanceof PopFrameCommand) {
            // flow call end (including all steps)?
            if (isFlowCallFrame(state, threadId)) {
                UUID correlationId = getCorrelationId(state, threadId);
                StepId stepId = StepId.from(correlationId, VMUtils.getCombinedLocal(state, threadId, LoopWrapper.CURRENT_INDEX));

                boolean ok = flowSteps.onStepEnd(stepId, state.getThreadError(threadId) == null);
                if (!ok) {
                    log.warn("beforeCommand ['{}', '{}'] -> step start info not found. This is most likely a bug", stepId, cmd);
                }
            }
            return Result.CONTINUE;
        }

        if (!shouldTraceCommand(cmd, params)) {
            return Result.CONTINUE;
        }

        if (cmd instanceof TaskResumeCommand) { // we already added span for task command
            return Result.CONTINUE;
        }

        StepCommand<?> s = (StepCommand<?>) cmd;
        Step step = s.getStep();

        log.debug("beforeCommand: {}", s);

        StepId stepId = StepId.from(s.getCorrelationId(), VMUtils.getCombinedLocal(state, threadId, LoopWrapper.CURRENT_INDEX));

        flowSteps.onStepStart(stepId,
                getStepName(runtime, state, threadId, step),
                getParentId(state, threadId),
                step.getLocation().fileName(), step.getLocation().lineNum(),
                getFlowName(state, threadId));

        return Result.CONTINUE;
    }

    @Override
    public Result afterCommand(Runtime runtime, VM vm, State state, ThreadId threadId, Command cmd) {
        if (params.enabled()) {
            afterCommand(state, threadId, cmd, true);
        }

        return Result.CONTINUE;
    }

    @Override
    public Result onCommandError(Runtime runtime, VM vm, State state, ThreadId threadId, Command cmd, Exception e) {
        if (params.enabled()) {
            afterCommand(state, threadId, cmd, false);
        }

        return Result.CONTINUE;
    }

    private void afterCommand(State state, ThreadId threadId, Command cmd, boolean success) {
        log.debug("afterCommand: {}", cmd);

        if (!shouldTraceCommand(cmd, params)) {
            return;
        }

        StepCommand<?> s = (StepCommand<?>) cmd;

        StepId stepId = StepId.from(s.getCorrelationId(), VMUtils.getCombinedLocal(state, threadId, LoopWrapper.CURRENT_INDEX));

        if (cmd instanceof FlowCallCommand) {
            setParentId(state, threadId, stepId);
            setCorrelationId(state, threadId, s.getCorrelationId());
            markFrameAsFlowCall(state, threadId);

            return;
        }

        boolean ok = flowSteps.onStepEnd(stepId, success);
        if (!ok) {
            log.warn("afterCommand ['{}', '{}'] -> step start info not found. This is most likely a bug", stepId, cmd);
        }
    }

    @Override
    public void afterProcessEnds(Runtime runtime, State state, Frame lastFrame) {
        if (!params.enabled()) {
            return;
        }

        if (isSuspended(state)) {
            persistenceService.persistFile(TELEMETRY_STATE_FILENAME,
                    out -> objectMapper.writeValue(out, this.flowSteps));

            return;
        }

        sendTelemetry(true);
    }

    @Override
    public void onProcessError(Runtime runtime, State state, Exception e) {
        if (!params.enabled()) {
            return;
        }

        sendTelemetry(false);
    }

    private void sendTelemetry(boolean isProcessFinishedOk) {
        long start = System.currentTimeMillis();
        log.info("Sending telemetry for process");

        TelemetryExporter telemetryExporter = new TelemetryExporter(params.endpoint());
        String traceId = telemetryExporter.sendTelemetry(flowSteps, isProcessFinishedOk);

        log.info("Sending telemetry for process -> done in {} ms", (System.currentTimeMillis() - start));

        String link = params.link();
        if (link != null) {
            log.info("Opentelemetry traces link: {}", link.replace("<traceId>", traceId));
        }
    }

    private static boolean isSuspended(State state) {
        return state.threadStatus().entrySet().stream()
                .anyMatch(e -> e.getValue() == ThreadStatus.SUSPENDED);
    }

    private static void setParentId(State state, ThreadId threadId, StepId id) {
        state.peekFrame(threadId).setLocal(PARENT_STEP_ID_VARIABLE, id);
    }

    private static StepId getParentId(State state, ThreadId threadId) {
        return VMUtils.getCombinedLocal(state, threadId, PARENT_STEP_ID_VARIABLE);
    }

    private static void setCorrelationId(State state, ThreadId threadId, UUID id) {
        state.peekFrame(threadId).setLocal(FLOW_CALL_CORRELATION_ID_STEP_ID_VARIABLE, id);
    }

    private static UUID getCorrelationId(State state, ThreadId threadId) {
        return (UUID) state.peekFrame(threadId).getLocal(FLOW_CALL_CORRELATION_ID_STEP_ID_VARIABLE);
    }

    private static String getFlowName(State state, ThreadId threadId) {
        return FlowCallCommand.getFlowName(state, threadId);
    }

    private static void markFrameAsFlowCall(State state, ThreadId threadId) {
        Frame frame = state.peekFrame(threadId);
        frame.setLocal(FLOW_CALL_FRAME_VARIABLE, frame.id());
    }

    private static boolean isFlowCallFrame(State state, ThreadId threadId) {
        Frame frame = state.peekFrame(threadId);
        Object value = frame.getLocal(FLOW_CALL_FRAME_VARIABLE);
        if (value instanceof FrameId) {
            return value.equals(frame.id());
        }
        return false;
    }

    private static String getStepName(Runtime runtime, State state, ThreadId threadId, Step step) {
        if (step instanceof AbstractStep) {
            ContextFactory contextFactory = runtime.getService(ContextFactory.class);
            Context ctx = contextFactory.create(runtime, state, threadId, step);

            String rawSegmentName = SegmentedLogger.getSegmentName((AbstractStep<?>) step);
            String segmentName = ctx.eval(rawSegmentName, String.class);
            if (segmentName != null) {
                return segmentName;
            }
        }

        return getDefaultDescription(runtime, state, threadId, step);
    }

    private static String getEntryPoint(Runtime runtime, State state, TelemetryParams params, ProcessConfiguration processConfiguration) {
        if (params.entryPointVariableName() == null) {
            return processConfiguration.entryPoint();
        }

        ContextFactory contextFactory = runtime.getService(ContextFactory.class);
        Context ctx = contextFactory.create(runtime, state, state.getRootThreadId(), null);

        String result = ctx.eval("${" + params.entryPointVariableName() + "}", String.class);
        if (result != null) {
            return result;
        }

        log.warn("can't load entry point from variable '{}', using process entry point: {}", params.entryPointVariableName(), processConfiguration.entryPoint());

        return processConfiguration.entryPoint();
    }

    private static String getDefaultDescription(Runtime runtime, State state, ThreadId threadId, Step step) {
        if (step instanceof FlowCall) {
            String flowName = ((FlowCall) step).getFlowName();

            ContextFactory contextFactory = runtime.getService(ContextFactory.class);
            Context ctx = contextFactory.create(runtime, state, threadId, step);

            flowName = ctx.eval(flowName, String.class);

            return "Flow call: " + flowName;
        } else if (step instanceof Expression) {
            return "Expression: " + ((Expression) step).getExpr();
        } else if (step instanceof ScriptCall) {
            return "Script: " + ((ScriptCall) step).getLanguageOrRef();
        } else if (step instanceof IfStep) {
            return "Check: " + ((IfStep) step).getExpression();
        } else if (step instanceof SwitchStep) {
            return "Switch: " + ((SwitchStep) step).getExpression();
        } else if (step instanceof SetVariablesStep) {
            return "Set variables";
        } else if (step instanceof Checkpoint) {
            return "Checkpoint: " + ((Checkpoint) step).getName();
        } else if (step instanceof FormCall) {
            return "Form call: " + ((FormCall) step).getName();
        } else if (step instanceof GroupOfSteps) {
            return "Group of steps";
        } else if (step instanceof ParallelBlock) {
            return "Parallel block";
        } else if (step instanceof ExitStep) {
            return "Exit";
        } else if (step instanceof ReturnStep) {
            return "Return";
        } else if (step instanceof TaskCall) {
            return "Task: " + ((TaskCall) step).getName();
        }

        return step.getClass().getName();
    }

    private boolean shouldTraceCommand(Command cmd, TelemetryParams params) {
        for (Class<?> cls : params.stepsToTrace()) {
            if (cls.isInstance(cmd)) {
                return true;
            }
        }
        return false;
    }
}
