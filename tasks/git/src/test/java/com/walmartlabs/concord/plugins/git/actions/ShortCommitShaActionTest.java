package com.walmartlabs.concord.plugins.git.actions;

import com.walmartlabs.concord.plugins.git.GitHubTaskParams;
import com.walmartlabs.concord.plugins.git.model.GitHubApiInfo;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import com.walmartlabs.concord.runtime.v2.sdk.UserDefinedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.Objects;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "GH_TEST_TOKEN", matches = ".+")
public class ShortCommitShaActionTest {

    @Test
    public void testOkAction() {
        var apiInfo = GitHubApiInfo.builder()
                .baseUrl("https://github.com")
                .accessTokenProvider(() -> Objects.requireNonNull(System.getenv("GH_TEST_TOKEN")))
                .build();

        var input = new GitHubTaskParams.GetShortCommitSha("walmartlabs", "concord-plugins", "db89df46a95deb3a35cf31b76274258391ebc61d", 7);

        var action = new ShortCommitShaAction();
        var result = action.execute(UUID.randomUUID(), apiInfo, input);
        assertNotNull(result);
        assertEquals("db89df4", result.get("shortSha"));
    }

    @Test
    public void testFailAction() {
        var apiInfo = GitHubApiInfo.builder()
                .baseUrl("https://github.com")
                .accessTokenProvider(() -> Objects.requireNonNull(System.getenv("GH_TEST_TOKEN")))
                .build();

        var input = new GitHubTaskParams.GetShortCommitSha("walmartlabs", "concord-plugins", "0123456789abcdef0123456789abcdef01234567", 7);

        var action = new ShortCommitShaAction();
        var ex = assertThrows(UserDefinedException.class, () -> action.execute(UUID.randomUUID(), apiInfo, input));
        assertTrue(ex.getMessage().contains("not found in"));
    }
}
