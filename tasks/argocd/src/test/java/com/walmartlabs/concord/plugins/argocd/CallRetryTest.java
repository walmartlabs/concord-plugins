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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CallRetryTest {

    @Mock
    Callable<String> primaryResp;
    @Mock
    Callable<Optional<String>> fallbackResp;

    @Test
    void test() throws Exception {
        when(primaryResp.call()).thenReturn("a");

        String result = new CallRetry<>(primaryResp, fallbackResp, Collections.emptyList()).attemptWithRetry(2);

        assertEquals("a", result);
        verify(primaryResp, times(1)).call();
        verify(fallbackResp, times(0)).call();
    }

    @Test
    void testPrimaryFail() throws Exception {
        when(primaryResp.call()).thenThrow(new IllegalStateException("forced exception"));
        when(fallbackResp.call()).thenReturn(Optional.of("b"));

        String result = new CallRetry<>(primaryResp, fallbackResp, Collections.emptyList()).attemptWithRetry(2);

        assertEquals("b", result);
        verify(primaryResp, times(1)).call();
        verify(fallbackResp, times(1)).call();
    }

    @Test
    void testPrimaryFailWithExpectedException() throws Exception {
        when(primaryResp.call()).thenThrow(new SocketTimeoutException("forced exception"));
        CallRetry<String> callRetry = new CallRetry<>(primaryResp, fallbackResp, List.of(SocketTimeoutException.class));
        Exception e = assertThrows(RuntimeException.class, () -> callRetry.attemptWithRetry(2));
        assertEquals(e.getMessage(), "java.net.SocketTimeoutException: forced exception");
        verify(primaryResp, times(1)).call();
        verify(fallbackResp, times(0)).call();
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

        String result = new CallRetry<>(primaryResp, fallbackResp, Collections.emptyList()).attemptWithRetry(2);

        assertEquals("a", result);
        verify(primaryResp, times(2)).call();
        verify(fallbackResp, times(1)).call();
    }
}
