package com.walmartlabs.concord.plugins.terraform;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2020 Walmart Inc., Concord Authors
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

import ca.ibodrov.concord.testcontainers.Concord;
import ca.ibodrov.concord.testcontainers.ConcordProcess;
import ca.ibodrov.concord.testcontainers.Payload;
import ca.ibodrov.concord.testcontainers.junit5.ConcordRule;
import com.walmartlabs.concord.client.ProcessEntry;
import com.walmartlabs.concord.common.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static ca.ibodrov.concord.testcontainers.Utils.randomString;

/**
 * Integration test. Assumes that the current version of the plugin
 * is present in the local Maven repository.
 */
public class TerraformTaskIT {

    @RegisterExtension
    public static final ConcordRule concord = new ConcordRule()
            .mode(Concord.Mode.DOCKER)
            .streamServerLogs(true)
            .streamAgentLogs(true)
            .useLocalMavenRepository(true);

    private static final String CURRENT_VERSION = getCurrentVersion();

    @Test
    public void testWithRuntimeV1() throws Exception {
        test("runtimeV1/concord.yml");
    }

    @Test
    public void testWithRuntimeV2() throws Exception {
        test("runtimeV2/concord.yml");
    }

    private void test(String concordYmlSource) throws Exception {
        String orgName = "org_" + randomString();
        concord.organizations().create(orgName);

        String projectName = "project_" + randomString();
        concord.projects().create(orgName, projectName);

        byte[] terraform_file = readToBytes("it/main.tf");

        Payload payload = new Payload()
                .org(orgName)
                .project(projectName)
                .file("main.tf", terraform_file)
                .file("mydir/main.tf", terraform_file)
                .file("mydir/nested/main.tf", terraform_file)
                .concordYml(new String(readToBytes(concordYmlSource))
                        .replace("%%version%%", CURRENT_VERSION));

        ConcordProcess proc = concord.processes().start(payload);
        proc.waitForStatus(ProcessEntry.StatusEnum.FINISHED);

        proc.assertLog(".*FINISHED.*");
        proc.assertLogAtLeast(".*No changes\\..*", 3);
    }

    private static String getCurrentVersion() {
        Properties props = new Properties();
        try (InputStream in = ClassLoader.getSystemResourceAsStream("version.properties")) {
            props.load(in);
            return props.getProperty("version");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] readToBytes(String resource) {
        try (InputStream in = TerraformTaskIT.class.getResourceAsStream(resource)) {
            return IOUtils.toByteArray(in);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
