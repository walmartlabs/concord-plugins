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

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.walmartlabs.concord.plugins.git.Utils;
import com.walmartlabs.concord.plugins.git.model.AppInstallation;
import com.walmartlabs.concord.plugins.git.model.AppInstallationAccessToken;
import com.walmartlabs.concord.plugins.git.model.Auth;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.OffsetDateTime;
import java.util.Date;

public class AppInstallationTokenProvider implements AccessTokenProvider {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private final Auth.AppInstallationAuth auth;
    private final String ghBaseUrl;
    private final String installationRepo;
    private AppInstallationAccessToken lastToken;

    public AppInstallationTokenProvider(Auth.AppInstallationAuth auth,
                                        String ghBaseUrl,
                                        String installationRepo) {
        this.auth = auth;
        this.ghBaseUrl = ghBaseUrl;
        // TODO rethink....GH apps can be also installed on orgs. But this plugin's actions are all for repos...currently.
        this.installationRepo = installationRepo;
    }

    @Override
    public String getToken() {
        if (!isExpired(lastToken, auth.refreshBufferSeconds())) {
            // not expiring within the buffer window, no need to create a new one
            return lastToken.token();
        }

        try {
            var jwt = generateJWT(auth);
            var accessTokenUrl = accessTokenUrl(ghBaseUrl, installationRepo, jwt);
            lastToken = createAccessToken(accessTokenUrl, jwt);

            return lastToken.token();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate JWT token", e);
        }
    }

    static boolean isExpired(AppInstallationAccessToken token, long buffer) {
        if (token == null) {
            return true;
        }

        return OffsetDateTime.now()
                .isAfter(token.expiresAt().minusSeconds(buffer));
    }

    static String accessTokenUrl(String baseUrl, String installationRepo, String jwt) throws URISyntaxException {
        var uri = URI.create(Utils.buildApiUrl(baseUrl, "/repos/" + installationRepo + "/installation"));
        var req = HttpRequest.newBuilder()
                .GET()
                .uri(uri)
                .header("Authorization", "Bearer " + jwt)
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .build();

        try {
            var appInstallation = sendRequest(req, 200, AppInstallation.class);
            return appInstallation.accessTokensUrl();
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving app installation", e);
        }
    }

    static AppInstallationAccessToken createAccessToken(String accessTokenUrl, String jwt) {
        var req = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.noBody())
                .uri(URI.create(accessTokenUrl))
                .header("Authorization", "Bearer " + jwt)
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .build();

        try {
            return sendRequest(req, 201, AppInstallationAccessToken.class);
        } catch (IOException e) {
            throw new RuntimeException("Error generating app access token", e);
        }
    }

    static String generateJWT(Auth.AppInstallationAuth auth) throws Exception {
        var rsaJWK = JWK.parseFromPEMEncodedObjects(auth.privateKey()).toRSAKey();

        // Create RSA-signer with the private key
        var signer = new RSASSASigner(rsaJWK);

        // Prepare JWT with claims set
        var claimsSet = new JWTClaimsSet.Builder()
                .issueTime(new Date())
                .issuer(auth.clientId())
                .expirationTime(new Date(new Date().getTime() + 60 * 10 * 1000))
                .build();

        var signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256)
                        .keyID(rsaJWK.getKeyID())
                        .build(),
                claimsSet);

        // Compute the RSA signature
        signedJWT.sign(signer);

        // To serialize to compact form, produces something like
        return signedJWT.serialize();
    }

    private static <T> T sendRequest(HttpRequest httpRequest, int expectedCode, Class<T> clazz) throws IOException {
        try {
            var resp = HTTP_CLIENT.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());
            if (resp.statusCode() != expectedCode) {
                throw new RuntimeException("Failed to retrieve app installation info, status code: " + resp.statusCode());
            }
            return Utils.getObjectMapper().readValue(resp.body(), clazz);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        throw new IllegalStateException("Unexpected error sending HTTP request");
    }
}
