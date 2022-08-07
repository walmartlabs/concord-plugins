package com.walmartlabs.concord.plugins.terraform.docker;

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

import com.walmartlabs.concord.runtime.v2.sdk.DockerContainerSpec;

import java.util.List;
import java.util.Map;

public class DockerContainerSpecV1Compat implements DockerContainerSpec {

    com.walmartlabs.concord.sdk.DockerContainerSpec specV1;

    public DockerContainerSpecV1Compat(com.walmartlabs.concord.sdk.DockerContainerSpec specV1) {
        this.specV1 = specV1;
    }

    public String image() {
        return specV1.image();
    }

    public String name() {
        return specV1.name();
    }

    public String user() {
        return specV1.user();
    }

    public String workdir() {
        return specV1.workdir();
    }

    public String entryPoint() {
        return specV1.entryPoint();
    }

    public String cpu() {
        return specV1.cpu();
    }

    public String memory() {
        return specV1.memory();
    }

    public String stdOutFilePath() {
        return specV1.stdOutFilePath();
    }

    public List<String> args() {
        return specV1.args();
    }

    public Map<String, String> env() {
        return specV1.env();
    }

    public String envFile() {
        return specV1.envFile();
    }

    public Map<String, String> labels() {
        return specV1.labels();
    }

    @Override
    public Options options() {

        if (specV1.options() == null) {
            return null;
        }

        // Convert runtime-v1 docker options object to runtime-v2
        com.walmartlabs.concord.sdk.DockerContainerSpec.Options o = specV1.options();

        if (o == null || o.hosts() == null) {
            return null;
        }

        return Options.builder()
                .hosts(o.hosts())
                .build();
    }

    public int pullRetryCount() {
        return specV1.pullRetryCount();
    }

    public long pullRetryInterval() {
        return specV1.pullRetryInterval();
    }

    public boolean debug() {
        return specV1.debug();
    }

    public boolean forcePull() {
        return specV1.forcePull();
    }

    public boolean redirectErrorStream() {
        return specV1.redirectErrorStream();
    }
}
