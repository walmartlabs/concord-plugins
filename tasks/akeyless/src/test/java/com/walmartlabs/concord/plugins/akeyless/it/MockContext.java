package com.walmartlabs.concord.plugins.akeyless.it;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2022 Walmart Inc., Concord Authors
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

import com.walmartlabs.concord.runtime.v2.sdk.Compiler;
import com.walmartlabs.concord.runtime.v2.sdk.*;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

public class MockContext implements Context {

    final Variables defaultsVars;
    final Variables variables;

    public MockContext(Map<String, Object> defaultsVars, Map<String, Object> variables) {
        this.defaultsVars = new MapBackedVariables(defaultsVars);
        this.variables = new MapBackedVariables(variables);
    }

    @Override
    public Path workingDirectory() {
        return null;
    }

    @Override
    public UUID processInstanceId() {
        return null;
    }

    @Override
    public Variables variables() {
        return variables;
    }

    @Override
    public Variables defaultVariables() {
        return defaultsVars;
    }

    @Override
    public FileService fileService() {
        return null;
    }

    @Override
    public DockerService dockerService() {
        return null;
    }

    @Override
    public SecretService secretService() {
        return null;
    }

    @Override
    public LockService lockService() {
        return null;
    }

    @Override
    public ApiConfiguration apiConfiguration() {
        return null;
    }

    @Override
    public ProcessConfiguration processConfiguration() {
        return null;
    }

    @Override
    public Execution execution() {
        return null;
    }

    @Override
    public Compiler compiler() {
        return null;
    }

    @Override
    public <T> T eval(Object v, Class<T> type) {
        return null;
    }

    @Override
    public <T> T eval(Object v, Map<String, Object> additionalVariables, Class<T> type) {
        return null;
    }

    @Override
    public void suspend(String eventName) {

    }

    @Override
    public void reentrantSuspend(String eventName, Map<String, Serializable> payload) {

    }
}
