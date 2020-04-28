package com.walmartlabs.concord.plugins.packer.commands;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2020 Walmart Inc.
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

import ca.vanzyl.concord.plugins.tool.ToolCommandSupport;
import ca.vanzyl.concord.plugins.tool.annotations.Flag;
import ca.vanzyl.concord.plugins.tool.annotations.OptionAsCsv;
import ca.vanzyl.concord.plugins.tool.annotations.OptionWithEquals;
import ca.vanzyl.concord.plugins.tool.annotations.Value;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.walmartlabs.concord.sdk.Context;

import javax.inject.Named;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/*
Usage: packer build [options] TEMPLATE

  Will execute multiple builds in parallel as defined in the template.
  The various artifacts created by the template will be outputted.

Options:

  -color=false                  Disable color output. (Default: color)
  -debug                        Debug mode enabled for builds.
  -except=foo,bar,baz           Run all builds and post-procesors other than these.
  -only=foo,bar,baz             Build only the specified builds.
  -force                        Force a build to continue if artifacts exist, deletes existing artifacts.
  -machine-readable             Produce machine-readable output.
  -on-error=[cleanup|abort|ask] If the build fails do: clean up (default), abort, or ask.
  -parallel=false               Disable parallelization. (Default: true)
  -parallel-builds=1            Number of builds to run in parallel. 0 means no limit (Default: 0)
  -timestamp-ui                 Enable prefixing of each ui output with an RFC3339 timestamp.
  -var 'key=value'              Variable for templates, can be used multiple times.
  -var-file=path
*/

@Named("packer/build")
public class Build
        extends ToolCommandSupport {

    @JsonProperty("debug")
    @Flag(name = {"-debug"})
    private Boolean debug;

    @JsonProperty("color")
    @OptionWithEquals(name = {"-color"})
    private Boolean color = false;

    @JsonProperty("force")
    @Flag(name = {"-force"})
    private Boolean force;

    @JsonProperty("except")
    @OptionAsCsv(name = {"-except"})
    private List<String> except;

    @JsonProperty("only")
    @OptionAsCsv(name = {"-only"})
    private List<String> only;

    @JsonProperty("machineReadable")
    @Flag(name = {"-machine-readable"})
    private Boolean machineReadable;

    @JsonProperty("onError")
    @OptionWithEquals(name = {"-on-error"}, allowedValues = {"cleanup", "abort", "ask"})
    private String onError;

    @JsonProperty("parallel")
    @Flag(name = {"-parallel"})
    private Boolean parallel;

    @JsonProperty("parallelBuilds")
    @OptionWithEquals(name = {"-parallel-builds"})
    private String parallelBuilds;

    @JsonProperty("timestampUi")
    @Flag(name = {"-timestamp-ui"})
    private Boolean timestampUi;

    @JsonProperty("varFile")
    @OptionWithEquals(name = {"-var-file"})
    private String varFile;

    @JsonProperty("template")
    @Value
    private String template;

    @JsonProperty("extraVars")
    private Map<String, Object> extraVars;

    @Override
    public void preExecute(Context context, Path workDir, List<String> cliArguments)
            throws Exception {
        if (extraVars == null || extraVars.isEmpty()) {
            return;
        }
        Path variablesFile = Files.createTempFile(workDir, "packer", ".variables.json");
        try (OutputStream out = Files.newOutputStream(variablesFile)) {
            new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(out, extraVars);
            cliArguments.add("-var-file=" + variablesFile.toAbsolutePath().toString());
        }
    }
}
