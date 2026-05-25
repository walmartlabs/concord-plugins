package com.walmartlabs.concord.plugins.terraform;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2026 Walmart Inc., Concord Authors
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

import com.walmartlabs.concord.plugins.terraform.actions.TerraformActionResult;
import com.walmartlabs.concord.plugins.terraform.backend.Backend;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TerraformTaskCommonTest {

    @TempDir
    Path tempDir;

    @Test
    void testInitAction() throws Exception {
        Map<String, Object> cfg = new HashMap<>();
        cfg.put(TaskConstants.ACTION_KEY, Action.INIT.name());
        cfg.put(TaskConstants.PWD_KEY, tempDir.toString());
        cfg.put(TaskConstants.DIR_KEY, tempDir.toString());
        cfg.put(TaskConstants.BACKEND_CONFIG_KEY, Arrays.asList(
                "path/to/config.hcl",
                "key=my/state.tfstate"
        ));

        Terraform terraform = mock(Terraform.class);
        TerraformArgs args = mock(TerraformArgs.class);
        when(args.hasChdir()).thenReturn(true);
        when(args.add(anyString(), anyString())).thenReturn(args);
        when(terraform.buildArgs(any(), any(Path.class))).thenReturn(args);
        when(terraform.exec(any(), anyString(), anyBoolean(), anyMap(), any(TerraformArgs.class)))
                .thenReturn(new Terraform.Result(0, "", ""));

        Backend backend = mock(Backend.class);

        TerraformActionResult result = TerraformTaskCommon.execute(terraform, Action.INIT, backend, tempDir, cfg,
                Collections.emptyMap());

        assertTrue(result.isOk());
        verify(backend).lock();
        verify(backend).init(tempDir);
        verify(args).add("-backend-config", "path/to/config.hcl");
        verify(args).add("-backend-config", "key=my/state.tfstate");
        verify(backend).cleanup(tempDir);
        verify(backend).unlock();
    }
}
