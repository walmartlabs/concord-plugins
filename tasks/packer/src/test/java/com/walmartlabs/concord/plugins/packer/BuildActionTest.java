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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import com.walmartlabs.concord.plugins.packer.actions.BuildAction;
import com.walmartlabs.concord.sdk.Constants.Context;

public class BuildActionTest {

    private String workDir = "/tmp/concord/workDir";

    @Mock
    private Packer packerMock;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Test
    public void actionTest() throws Exception {
        final BuildAction buildAction = new BuildAction(setupConfig(), null);
        final Packer.Result result = new Packer.Result(0, "stdout", "stderr");
        when(packerMock.exec(any(Path.class), anyString(), eq(false), eq(null), anyList())).thenReturn(result);
        buildAction.exec(packerMock);

        List<String> args = new ArrayList<>();
        args.add("build");
        args.add("-color=false");
        args.add("-debug");
        args.add("-parallel-builds=" + 0);
        verify(packerMock).exec(Paths.get(workDir), Constants.PACKER_LOG_PREFIX, false, null, args);
    }

    private Map<String, Object> setupConfig() {
        final Map<String, Object> cfg = new HashMap<>();
        cfg.put(Constants.DEBUG_KEY, true);
        cfg.put(Constants.FORCE_KEY, false);
        cfg.put(Constants.PARALLEL_BUILDS_KEY, 0);

        cfg.put(Context.WORK_DIR_KEY, workDir);
        cfg.put(Constants.EXCEPT_KEY, Collections.emptyList());
        cfg.put(Constants.ONLY_KEY, Collections.emptyList());
        cfg.put(Constants.EXTRA_VARS_KEY, null);
        cfg.put(Constants.IGNORE_ERRORS_KEY, false);

        return cfg;
    }
}
