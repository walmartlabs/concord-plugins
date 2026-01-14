package com.walmartlabs.concord.plugins.confluence;

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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;

public abstract class AbstractTaskTest {

    protected static final String MOCK_PAT = "mock-pat" + randomString(10);
    protected static final String MOCK_USERNAME = "mock-user";
    protected static final String MOCK_PASSWORD = "mock-pass-" + randomString(5);
    protected static final String B64_ENC_CREDENTIALS = base64Enc(MOCK_USERNAME + ":" + MOCK_PASSWORD);

    void stubGetContentWithBasicAuth() {
        stubFor(get("/rest/api/content/12345?expand=body.storage")
                .withHeader("Authorization", equalTo("Basic " + B64_ENC_CREDENTIALS))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(resourceToString("wiremock/content/get_content_200.json"))));
    }

    void stubGetContentWithPat() {
        stubFor(get("/rest/api/content/12345?expand=body.storage")
                .withHeader("Authorization", equalTo("Bearer " + MOCK_PAT))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(resourceToString("wiremock/content/get_content_200.json"))));
    }

    static String resourceToString(String resource) {
        try (InputStream in = TaskV2Test.class.getResourceAsStream(resource)) {
            if (Objects.isNull(in)) {
                throw new IllegalStateException("Failed to load resource: " + resource);
            }

            return new String(in.readAllBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static String randomString(int len) {
        int lower = 97; // 'a'
        int upper = 122; // 'z'

        return new SecureRandom().ints(lower, upper + 1)
                .limit(len)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    static String base64Enc(String src) {
        return Base64.getEncoder().encodeToString(src.getBytes(StandardCharsets.UTF_8));
    }
}
