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

import ca.vanzyl.concord.plugins.tool.ToolCommand;
import ca.vanzyl.concord.plugins.tool.ToolInitializer;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.walmartlabs.concord.plugins.ConcordTestSupport;
import com.walmartlabs.concord.plugins.OKHttpDownloadManager;
import com.walmartlabs.concord.plugins.packer.commands.Build;
import com.walmartlabs.concord.plugins.packer.commands.Version;
import com.walmartlabs.concord.sdk.Context;
import org.junit.Test;

import java.util.Map;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

public class PackerTaskTest
        extends ConcordTestSupport {
    @Test
    public void valiatePackerBuildCommandConstruction()
            throws Exception {

        ToolInitializer toolInitializer = new ToolInitializer(new OKHttpDownloadManager("packer"));
        Map<String, ToolCommand> commands = ImmutableMap.of("packer/build", new Build());
        PackerTask task = new PackerTask(commands, toolInitializer);

        Map<String, Object> args = Maps.newHashMap(mapBuilder()
                .put("dryRun", true)
                .put("command", "build")
                .put("saveOutput", true)
                .put("debug", true)
                .put("force", true)
                .put("except", ImmutableList.of("foo", "bar", "baz"))
                .put("extraVars", mapBuilder()
                        .put("aws_access_key", "foo")
                        .put("aws_secret_key", "bar")
                        .build())
                .put("template", "packer.json")
                .build());

        Context context = context(args);
        task.execute(context);

        // the extraVars are going to get serialized to a packer variables json file
        String expectedCommandLine = "packer build -debug -color=false -force -except=foo,bar,baz packer.json -var-file=.*\\.variables\\.json";
        assertThat(normalizedCommandLineArguments(context)).matches(expectedCommandLine);
    }

    @Test
    public void valiateDownloadingNonDefaultVersionOfPacker()
            throws Exception {

        ToolInitializer toolInitializer = new ToolInitializer(new OKHttpDownloadManager("packer"));
        Map<String, ToolCommand> commands = ImmutableMap.of("packer/version", new Version());
        PackerTask task = new PackerTask(commands, toolInitializer);

        Map<String, Object> args = Maps.newHashMap(mapBuilder()
                .put("version", "1.5.2")
                .put("command", "version")
                .put("saveOutput", true)
                .build());

        Context context = context(args);
        task.execute(context);

        String expectedCommandLine = "packer version";
        assertThat(normalizedCommandLineArguments(context)).isEqualTo(expectedCommandLine);

        // Retrieve the logs from the context and assert we retrieved an alternate version
        assertThat(varAsString(context, "logs")).contains("Packer v1.5.2");
    }
}
