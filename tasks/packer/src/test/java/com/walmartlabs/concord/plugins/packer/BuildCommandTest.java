package com.walmartlabs.concord.plugins.packer;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2019 Walmart Inc.
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

import static org.mockito.Mockito.verify;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import com.walmartlabs.concord.plugins.packer.commands.BuildCommand;

public class BuildCommandTest {

    private Path workDir = Paths.get("/tmp/concord/workDir");
    private Path varsFile = Paths.get("/tmp/concord/vars.json");

    @Mock
    private Packer packerMock;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Test
    public void execTest() throws Exception {
        boolean debug = true;
        boolean force = false;
        int parallelBuilds = 10;
        List<String> except = Collections.emptyList();
        List<String> only = Collections.emptyList();
        Map<String, String> env = Collections.emptyMap();

        final BuildCommand buildCommand = new BuildCommand(debug, force, parallelBuilds, workDir, varsFile, except, only, env);

        buildCommand.exec(packerMock);

        List<String> args = new ArrayList<>();
        args.add("build");
        args.add("-color=false");
        args.add("-debug");
        args.add("-parallel-builds=" + parallelBuilds);
        args.add("-var-file=" + varsFile.toAbsolutePath());

        verify(packerMock).exec(workDir, Constants.PACKER_LOG_PREFIX, false, env, args);
    }

    @Test
    public void paramTest() throws Exception {
        boolean debug = false;
        boolean force = true;
        int parallelBuilds = 5;
        List<String> except = Arrays.asList("foo", "bar", "baz");
        List<String> only = Collections.emptyList();
        Map<String, String> env = Collections.emptyMap();

        final BuildCommand buildCommand = new BuildCommand(debug, force, parallelBuilds, workDir, varsFile, except, only, env);

        buildCommand.exec(packerMock);

        List<String> args = new ArrayList<>();
        args.add("build");
        args.add("-color=false");
        args.add("-except=" + String.join(",", except));
        args.add("-force");
        args.add("-parallel-builds=" + parallelBuilds);
        args.add("-var-file=" + varsFile.toAbsolutePath());

        verify(packerMock).exec(workDir, Constants.PACKER_LOG_PREFIX, false, env, args);
    }

}
