package com.walmartlabs.concord.plugins.hashivault.model;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2021 Walmart Inc., Concord Authors
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

import com.walmartlabs.concord.runtime.v2.sdk.*;
import com.walmartlabs.concord.runtime.v2.sdk.Compiler;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

public class MockContextV2 implements Context {
    private final Variables variables;
    private final Variables defaultVariables;

    public MockContextV2(Map<String, Object> vars, Map<String, Object> defs) {
        this.variables = new MapBackedVariables(vars);
        this.defaultVariables = new MapBackedVariables(defs);
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
        return defaultVariables;
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
    public <T> T eval(Object o, Class<T> aClass) {
        return null;
    }

    @Override
    public <T> T eval(Object o, Map<String, Object> map, Class<T> aClass) {
        return null;
    }

    @Override
    public void suspend(String s) {
    }

    @Override
    public void reentrantSuspend(String s, Map<String, Serializable> map) {
    }
}
