package com.walmartlabs.concord.plugins.git.v2;

import com.walmartlabs.concord.common.IOUtils;
import com.walmartlabs.concord.plugins.git.GitTask;
import com.walmartlabs.concord.runtime.v2.sdk.MapBackedVariables;
import com.walmartlabs.concord.runtime.v2.sdk.SecretService;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import com.walmartlabs.concord.runtime.v2.sdk.WorkingDirectory;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class GitTaskV2Test {

    private Path workDir;

    @Before
    public void setUp() throws Exception {
        this.workDir = Files.createTempDirectory("test");
    }

    public void tearDown() throws Exception {
        if (this.workDir != null && Files.exists(this.workDir)) {
            IOUtils.deleteRecursively(workDir);
        }
    }

    @Test
    public void test() throws Exception {
        Map<String, Object> input = new HashMap<>();
        input.put(GitTask.ACTION_KEY, GitTask.Action.CLONE.name());
        input.put(GitTask.GIT_URL, "https://github.com/walmartlabs/concord-plugins.git");

        GitTaskV2 task = new GitTaskV2(mock(SecretService.class), new WorkingDirectory(workDir));
        TaskResult result = task.execute(new MapBackedVariables(input));
        assertTrue(result.ok());
    }
}
