package com.walmartlabs.concord.plugins.terraform.commands;

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

import com.walmartlabs.concord.plugins.terraform.Terraform;
import com.walmartlabs.concord.plugins.terraform.TerraformArgs;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class InitCommandTest {

    @TempDir
    Path tempDir;

    @Test
    public void testBackendConfig() throws Exception {
        List<String> capturedArgs = new ArrayList<>();
        TerraformArgs args = mockArgs(capturedArgs);

        Terraform terraform = mock(Terraform.class);
        when(terraform.buildArgs(any(), any(Path.class))).thenReturn(args);
        when(terraform.exec(any(), anyString(), anyBoolean(), anyMap(), any(TerraformArgs.class)))
                .thenReturn(new Terraform.Result(0, "", ""));

        List<String> backendConfig = Arrays.asList(
                "path/to/config.hcl",
                "key=my/state.tfstate"
        );

        new InitCommand(tempDir, tempDir, backendConfig, Collections.emptyMap(), true)
                .exec(terraform);

        assertTrue(capturedArgs.contains("-backend-config=path/to/config.hcl"));
        assertTrue(capturedArgs.contains("-backend-config=key=my/state.tfstate"));
    }

    @Test
    public void testNullBackendConfig() throws Exception {
        List<String> capturedArgs = new ArrayList<>();
        TerraformArgs args = mockArgs(capturedArgs);

        Terraform terraform = mock(Terraform.class);
        when(terraform.buildArgs(any(), any(Path.class))).thenReturn(args);
        when(terraform.exec(any(), anyString(), anyBoolean(), anyMap(), any(TerraformArgs.class)))
                .thenReturn(new Terraform.Result(0, "", ""));

        new InitCommand(tempDir, tempDir, null, Collections.emptyMap(), true)
                .exec(terraform);

        assertFalse(capturedArgs.stream().anyMatch(a -> a.contains("-backend-config")));
    }

    @Test
    public void testEmptyBackendConfig() throws Exception {
        List<String> capturedArgs = new ArrayList<>();
        TerraformArgs args = mockArgs(capturedArgs);

        Terraform terraform = mock(Terraform.class);
        when(terraform.buildArgs(any(), any(Path.class))).thenReturn(args);
        when(terraform.exec(any(), anyString(), anyBoolean(), anyMap(), any(TerraformArgs.class)))
                .thenReturn(new Terraform.Result(0, "", ""));

        new InitCommand(tempDir, tempDir, Collections.emptyList(), Collections.emptyMap(), true)
                .exec(terraform);

        assertFalse(capturedArgs.stream().anyMatch(a -> a.contains("-backend-config")));
    }

    private static TerraformArgs mockArgs(List<String> capturedArgs) {
        TerraformArgs args = mock(TerraformArgs.class);
        when(args.hasChdir()).thenReturn(true);
        when(args.add(anyString(), anyString())).thenAnswer(invocation -> {
            capturedArgs.add(invocation.getArgument(0) + "=" + invocation.getArgument(1));
            return args;
        });
        when(args.add(anyString())).thenAnswer(invocation -> {
            capturedArgs.add(invocation.getArgument(0, String.class));
            return args;
        });
        return args;
    }
}
