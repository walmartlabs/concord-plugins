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

import com.walmartlabs.concord.runtime.v2.runner.vm.*;
import com.walmartlabs.concord.sdk.MapUtils;

import javax.annotation.Nullable;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

public class TelemetryParams implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final List<Class<? extends StepCommand<?>>> DEFAULT_STEP_TO_TRACE = List.of(
            FlowCallCommand.class,
            TaskCallCommand.class,
            TaskResumeCommand.class);

    private final Map<String, Object> params;

    public TelemetryParams(Map<String, Object> params) {
        this.params = params;
    }

    public boolean enabled() {
        return MapUtils.getBoolean(params, "enabled", false);
    }

    public String endpoint() {
        return MapUtils.assertString(params, "endpoint");
    }

    public String entryPointVariableName() {
        return MapUtils.getString(params, "entryPointVariableName");
    }

    @Nullable
    public String link() {
        return MapUtils.getString(params, "link");
    }

    public List<? extends Class<? extends StepCommand<?>>> stepsToTrace() {
        List<String> steps = MapUtils.getList(params, "additionalSteps", List.of());
        if (steps.isEmpty()) {
            return DEFAULT_STEP_TO_TRACE;
        }

        return Stream.concat(
                        DEFAULT_STEP_TO_TRACE.stream(),
                        steps.stream()
                                .map(TelemetryParams::stepToStepCommand)
                                .filter(Objects::nonNull))
                .toList();
    }

    private static Class<? extends StepCommand<?>> stepToStepCommand(String step) {
        switch (step) {
            case "expression":
                return ExpressionCommand.class;
            case "checkpoint":
                return CheckpointCommand.class;
            case "set":
                return SetVariablesCommand.class;
            case "script":
                return ScriptCallCommand.class;
            default:
                return null;
        }
    }
}
