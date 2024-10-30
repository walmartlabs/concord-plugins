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

import com.walmartlabs.concord.runtime.v2.model.FlowCallOptions;
import com.walmartlabs.concord.runtime.v2.model.TaskCallOptions;
import com.walmartlabs.concord.runtime.v2.runner.vm.FlowCallCommand;
import com.walmartlabs.concord.runtime.v2.runner.vm.StepCommand;
import com.walmartlabs.concord.runtime.v2.runner.vm.TaskCallCommand;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;

import static java.util.Objects.requireNonNullElse;

public class Spans {

    public static Span startSpan(Tracer tracer, StepCommand<?> cmd) {
        var span = tracer.spanBuilder(spanName(cmd))
                .startSpan();

        applyLocationAttributes(span, cmd);
        applyStepAttributes(span, cmd);

        return span;
    }

    private static String spanName(StepCommand<?> cmd) {
        if (cmd instanceof TaskCallCommand) {
            return "task_call";
        } else if (cmd instanceof FlowCallCommand) {
            return "flow_call";
        }

        // TODO

        return "command";
    }

    public static void applyStepAttributes(Span span, StepCommand<?> cmd) {
        span.setAttribute("command.class", cmd.getClass().getSimpleName());
        span.setAttribute("cmd.correlationId", cmd.getCorrelationId().toString());

        if (cmd instanceof TaskCallCommand tcc) {
            var step = tcc.getStep();
            span.setAttribute("task.name", step.getName());

            var options = step.getOptions();
            if (options != null) {
                applyTaskCallOptionsAttributes(span, options);
            }
        } else if (cmd instanceof FlowCallCommand fcc) {
            var step = fcc.getStep();
            span.setAttribute("flow.name", step.getFlowName());

            var options = step.getOptions();
            if (options != null) {
                applyFlowCallOptionsAttributes(span, options);
            }
        }

        // TODO
    }

    private static void applyLocationAttributes(Span span, StepCommand<?> cmd) {
        var location = cmd.getStep().getLocation();
        span.setAttribute("location.filename", requireNonNullElse(location.fileName(), "unknown"));
        span.setAttribute("location.lineNum", location.lineNum());
        span.setAttribute("location.column", location.column());
    }

    private static void applyTaskCallOptionsAttributes(Span span, TaskCallOptions options) {
        span.setAttribute("task.options.ignoreErrors", options.ignoreErrors());

        var out = options.out();
        if (out != null) {
            span.setAttribute("task.options.out", out);
        }

        // TODO
    }

    private static void applyFlowCallOptionsAttributes(Span span, FlowCallOptions options) {
        var out = options.out();
        if (out != null) {
            span.setAttribute("flow.options.out", "[\"" + String.join("\",\"", out) + "\"]");
        }

        // TODO
    }

    private Spans() {
    }
}
