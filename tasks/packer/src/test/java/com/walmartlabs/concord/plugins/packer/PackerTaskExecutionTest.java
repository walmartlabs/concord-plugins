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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.walmartlabs.concord.plugins.ConcordTestSupport;
import com.walmartlabs.concord.plugins.OKHttpDownloadManager;
import com.walmartlabs.concord.plugins.RequiresAwsCredentials;
import com.walmartlabs.concord.plugins.packer.commands.Build;
import com.walmartlabs.concord.sdk.Context;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Ignore("requires AWS credentials")
@RequiresAwsCredentials
public class PackerTaskExecutionTest
        extends ConcordTestSupport {
    @Test
    public void validateExecutingPacker()
            throws Exception {
        ToolInitializer toolInitializer = new ToolInitializer(new OKHttpDownloadManager("packer"));
        Map<String, ToolCommand> commands = ImmutableMap.of("packer/build", new Build());
        PackerTask task = new PackerTask(commands, toolInitializer);

        Map<String, Object> args = Maps.newHashMap(mapBuilder()
                .put("command", "build")
                .put("saveOutput", true)
                .put("template", packerTestFile().toString())
                .build());

        Context context = context(args);
        task.execute(context);

        /*

        Debug mode enabled. Builds will not be parallelized.
        ==> amazon-ebs: Prevalidating any provided VPC information
        ==> amazon-ebs: Prevalidating AMI Name: packer-example 1585436207
            amazon-ebs: Found Image ID: ami-04ac550b78324f651
        ==> amazon-ebs: Creating temporary keypair: packer_5e7fd62f-8128-badf-da32-4f8356ed603a
            amazon-ebs: Saving key for debug purposes: ec2_amazon-ebs.pem
        ==> amazon-ebs: Creating temporary security group for this instance: packer_5e7fd632-c20b-002b-ccb6-ea9e2adac21e
        ==> amazon-ebs: Authorizing access to port 22 from [0.0.0.0/0] in the temporary security groups...
        ==> amazon-ebs: Launching a source AWS instance...
        ==> amazon-ebs: Adding tags to source instance
            amazon-ebs: Adding tag: "Name": "Packer Builder"
            amazon-ebs: Instance ID: i-0d5e29779b4f2406d
        ==> amazon-ebs: Waiting for instance (i-0d5e29779b4f2406d) to become ready...
            amazon-ebs: Public DNS: ec2-34-238-135-221.compute-1.amazonaws.com
            amazon-ebs: Public IP: 34.238.135.221
            amazon-ebs: Private IP: 172.31.91.204
        ==> amazon-ebs: Using ssh communicator to connect: 34.238.135.221
        ==> amazon-ebs: Waiting for SSH to become available...
        ==> amazon-ebs: Connected to SSH!
        ==> amazon-ebs: Stopping the source instance...
            amazon-ebs: Stopping instance
        ==> amazon-ebs: Waiting for the instance to stop...
        ==> amazon-ebs: Creating AMI packer-example 1585436207 from instance i-0d5e29779b4f2406d
            amazon-ebs: AMI: ami-083dfd9b6f66d45b5
        ==> amazon-ebs: Waiting for AMI to become ready...
        ==> amazon-ebs: Terminating the source AWS instance...
        ==> amazon-ebs: Cleaning up any extra volumes...
        ==> amazon-ebs: No volumes to clean up, skipping
        ==> amazon-ebs: Deleting temporary security group...
        ==> amazon-ebs: Deleting temporary keypair...
        Build 'amazon-ebs' finished.

        ==> Builds finished. The artifacts of successful builds are:
        --> amazon-ebs: AMIs were created:
        us-east-1: ami-083dfd9b6f66d45b5

        */

        // Retrieve the logs from the context
        String logs = varAsString(context, "logs");

        assertThat(logs).contains("==> Builds finished. The artifacts of successful builds are:");
        assertThat(logs).contains("--> amazon-ebs: AMIs were created:");
    }

    private Path packerTestFile() {
        String terraformTestFileEnvar = System.getenv("PACKER_TEST_FILE");
        if (terraformTestFileEnvar != null) {
            return Paths.get(System.getenv("PACKER_TEST_FILE"));
        }
        // Use the test packer file we have in the repository
        return new File(basedir, "src/test/packer/packer-test.json").toPath();
    }
}
