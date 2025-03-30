package com.walmartlabs.concord.plugins.argocd;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2024 Walmart Inc., Concord Authors
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

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WireMockTest
@ExtendWith(MockitoExtension.class)
class CallRetryTest {

    @Mock
    Callable<String> primaryResp;
    @Mock
    Callable<Optional<String>> fallbackResp;

    private static final Duration SHORT_TIMEOUT = Duration.ofMillis(10);
    private static final Duration LONG_TIMEOUT = Duration.ofSeconds(30);

    @Test
    void test() throws Exception {
        when(primaryResp.call()).thenReturn("a");

        String result = new CallRetry<>(primaryResp, fallbackResp, Collections.emptySet()).attemptWithRetry(2, LONG_TIMEOUT);

        assertEquals("a", result);
        verify(primaryResp, times(1)).call();
        verify(fallbackResp, times(0)).call();
    }

    @Test
    void testPrimaryFail() throws Exception {
        when(primaryResp.call()).thenThrow(new IllegalStateException("forced exception"));
        when(fallbackResp.call()).thenReturn(Optional.of("b"));

        String result = new CallRetry<>(primaryResp, fallbackResp, Collections.emptySet()).attemptWithRetry(2, LONG_TIMEOUT);

        assertEquals("b", result);
        verify(primaryResp, times(1)).call();
        verify(fallbackResp, times(1)).call();
    }

    @Test
    void testPrimaryFailWithExpectedException() throws Exception {
        when(primaryResp.call()).thenThrow(new SocketTimeoutException("forced exception"));
        CallRetry<String> callRetry = new CallRetry<>(primaryResp, fallbackResp, Set.of(SocketTimeoutException.class));
        Exception e = assertThrows(RuntimeException.class, () -> callRetry.attemptWithRetry(2, LONG_TIMEOUT));
        assertEquals("java.net.SocketTimeoutException: forced exception", e.getMessage());
        verify(primaryResp, times(1)).call();
        verify(fallbackResp, times(0)).call();
    }

    @Test
    void testPrimaryCallTimeout(WireMockRuntimeInfo wiremock) throws Exception {
        stubFor(get("/timeout").willReturn(aResponse()
                .withFixedDelay(10_000)
                .withStatus(200)));

        doAnswer(invocation -> {
            var req = HttpRequest.newBuilder(URI.create(wiremock.getHttpBaseUrl() + "/timeout")).GET().build();
             HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.discarding());
            return null;
        }).when(primaryResp).call();
        when(fallbackResp.call()).thenReturn(Optional.empty());

        CallRetry<String> callRetry = new CallRetry<>(primaryResp, fallbackResp, Set.of(SocketTimeoutException.class));

        Exception e = assertThrows(RuntimeException.class, () -> callRetry.attemptWithRetry(2, SHORT_TIMEOUT));
        assertEquals("Call attempt timed out after 10ms", e.getMessage());
        verify(primaryResp, times(1)).call();
        verify(fallbackResp, times(1)).call();
    }

    @Test
    void testRetry() throws Exception {
        when(primaryResp.call()).thenAnswer(new Answer<String>() {
            int attempts = 0;
            @Override
            public String answer(InvocationOnMock invocation) {
                if (++attempts < 2) {
                    throw new IllegalStateException("forced exception");
                }

                return "a";
            }
        });
        when(fallbackResp.call()).thenReturn(Optional.empty());

        String result = new CallRetry<>(primaryResp, fallbackResp, Collections.emptySet()).attemptWithRetry(2, LONG_TIMEOUT);

        assertEquals("a", result);
        verify(primaryResp, times(2)).call();
        verify(fallbackResp, times(1)).call();
    }
}
