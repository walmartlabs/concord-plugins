package com.walmartlabs.concord.plugins.git.tokens;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2025 Walmart Inc., Concord Authors
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

import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.walmartlabs.concord.plugins.git.GitSecretService;
import com.walmartlabs.concord.plugins.git.model.Auth;
import com.walmartlabs.concord.plugins.git.model.ImmutableAppInstallationAuth;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessTokenProviderTest {

    @RegisterExtension
    protected static WireMockExtension httpRule = WireMockExtension.newInstance()
            .options(wireMockConfig()
                    .dynamicPort()
                    .templatingEnabled(true)
                    .globalTemplating(true)
                    .usingFilesUnderClasspath("wiremock/auth")
                    .notifier(new ConsoleNotifier(false))) // set to true for verbose logging
            .build();

    @TempDir
    Path workDir;

    @Mock
    GitSecretService secretService;

    @Test
    void testFromAccessTokenAuth() {
        var auth = Auth.builder()
                .accessToken("mock-token")
                .build();

        var provider = AccessTokenProvider.fromAuth(auth, null, null, secretService);

        assertEquals("mock-token", provider.getToken());
    }

    @Test
    void testFromAppInstallationTokenAuth() {
        var auth = Auth.builder()
                .appInstallation(Auth.AppInstallationAuth.builder()
                        .privateKey("mock-pk")
                        .clientId("mock-client")
                        .build())
                .build();

        var provider = AccessTokenProvider.fromAuth(auth, null, null, secretService);

        assertInstanceOf(AppInstallationTokenProvider.class, provider);
    }

    @Test
    void testFromAppInstallationTokenSecretAuth() throws Exception {
        var auth = Auth.builder()
                .appInstallationSecret(Auth.AppInstallationSecretAuth.builder()
                        .org("mock-secret-org")
                        .name("mock-secret-name")
                        .build())
                .build();

        when(secretService.exportFile(any(), any(), any()))
                .thenReturn(Files.writeString(workDir.resolve("secret.json"), """
                        {
                            "clientId": "mock-client",
                            "privateKey": "mock-pk"
                        }
                        """));

        var provider = AccessTokenProvider.fromAuth(auth, null, null, secretService);

        assertInstanceOf(AppInstallationTokenProvider.class, provider);
    }

    @Test
    void testGenerateJWT() throws Exception {
        var pk = generatePrivateKey();
        var jwt = AppInstallationTokenProvider.generateJWT(ImmutableAppInstallationAuth.builder()
                .privateKey(pk)
                .clientId("mock-client")
                .build());

        assertNotNull(jwt);
    }

    @Test
    void testGetAccessTokenUrl() {
        var url = assertDoesNotThrow(() ->
                AppInstallationTokenProvider.accessTokenUrl(httpRule.baseUrl(),
                        "octocat/mock-repo", "mock-jwt"));

        assertNotNull(url);
        assertEquals(httpRule.baseUrl() + "/api/v3/app/installations/12345/access_tokens", url);
    }

    @Test
    void testCreateAccessToken() {
        // TODO assert auth/jwt request header
        var token = AppInstallationTokenProvider.createAccessToken(httpRule.baseUrl() + "/api/v3/app/installations/12345/access_tokens", "mock-jwt");

        assertNotNull(token);
        assertEquals("mock_token", token.token());

        assertTrue(AppInstallationTokenProvider.isExpired(token, 1));
    }

    @Test
    void testFull() throws Exception {
        var pk = generatePrivateKey();

        var auth = ImmutableAppInstallationAuth.builder()
                .privateKey(pk)
                .clientId("mock-client")
                .build();

        var provider = new AppInstallationTokenProvider(auth, httpRule.baseUrl(), "octocat/mock-repo");

        var token = provider.getToken();
        assertEquals("mock_token", token);
    }

    private static String generatePrivateKey() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");

        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();

        Base64.Encoder encoder = Base64.getEncoder();

        return "-----BEGIN PRIVATE KEY-----\n" +
                encoder.encodeToString(kp.getPrivate().getEncoded()) +
                "\n-----END PRIVATE KEY-----\n";
    }

}
