package com.walmartlabs.concord.plugins.terraform.docker;

import com.walmartlabs.concord.runtime.v2.sdk.DockerContainerSpec;
import org.jetbrains.annotations.Nullable;

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

    @Nullable
    public String name() {
        return specV1.name();
    }

    @Nullable
    public String user() {
        return specV1.user();
    }

    @Nullable
    public String workdir() {
        return specV1.workdir();
    }

    @Nullable
    public String entryPoint() {
        return specV1.entryPoint();
    }

    @Nullable
    public String cpu() {
        return specV1.cpu();
    }

    @Nullable
    public String memory() {
        return specV1.memory();
    }

    @Nullable
    public String stdOutFilePath() {
        return specV1.stdOutFilePath();
    }

    @Nullable
    public List<String> args() {
        return specV1.args();
    }

    @Nullable
    public Map<String, String> env() {
        return specV1.env();
    }

    @Nullable
    public String envFile() {
        return specV1.envFile();
    }

    @Nullable
    public Map<String, String> labels() {
        return specV1.labels();
    }

    @Nullable
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
