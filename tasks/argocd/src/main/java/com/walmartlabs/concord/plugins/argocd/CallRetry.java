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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

import static java.util.concurrent.TimeUnit.*;

public class CallRetry<R> {
    private static final Logger log = LoggerFactory.getLogger(CallRetry.class);
    private final Callable<R> mainAttempt;
    private final Collection<Callable<Optional<R>>> fallbackAttempts;
    private final Set<Class<? extends Exception>> exceptionsToNotRetry;

    /**
     * @param mainAttempt Primary call to attempt to execute
     * @param fallbackAttempt Fallback call which will be attempted if the main call throws an exception.
     * @param exceptionsToNotRetry dont retry for these exceptions
     */
    public CallRetry(Callable<R> mainAttempt, Callable<Optional<R>> fallbackAttempt, Set<Class<? extends Exception>> exceptionsToNotRetry) {
        this.mainAttempt = mainAttempt;
        this.fallbackAttempts = fallbackAttempt == null ? List.of() : List.of(fallbackAttempt);
        this.exceptionsToNotRetry = exceptionsToNotRetry;
    }

    /**
     * Attempts to execute a call with a 1-hour timeout for each attempt
     * @deprecated  Use {@link #attemptWithRetry(int, Duration)} instead
     */
    @Deprecated
    public R attemptWithRetry(int maxTries) {
        return attemptWithRetry(maxTries, Duration.ofHours(1));
    }

    /**
     * Attempts to call and return result from main call up-to the given number of tries.
     * If the fallback call successfully returns data, after a failure of the main call,
     * then the fallback result is returned immediately
     * @param maxTries number of attempts to make
     * @param totalTimeout timeout for the overall attempt, including retries
     * @return Results of main call, or fallback result if data is present after main failure
     */
    public R attemptWithRetry(int maxTries, Duration totalTimeout) {
        Throwable lastError = null;

        var executor = Executors.newSingleThreadExecutor();
        long startMillis = System.currentTimeMillis();

        int attemptsMade = 0;
        while (attemptsMade++ < maxTries) {
            long actualTimeout = Math.max(1000, totalTimeout.toMillis() - (System.currentTimeMillis() - startMillis));

            try {
                return executor.submit(mainAttempt).get(actualTimeout, MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) { // error within callable execution
                lastError = e.getCause();
            } catch (TimeoutException e) {
                return executeFallback().orElseThrow(() -> new RuntimeException("Call attempt timed out after " + totalTimeout.toMillis() + "ms"));
            } catch (Exception e) {
                lastError = e;
            }

            assertIsRetryable(lastError);

            // if any of these returns cleanly then that's good enough
            var fallbackResult = executeFallback();
            if (fallbackResult.isPresent()) {
                return fallbackResult.get();
            }

            log.warn("Retrying [{}/{}]...", attemptsMade, maxTries);
        }

        throw new RuntimeException("Too many attempts. Last error: ", lastError);
    }

    private Optional<R> executeFallback() {
        for (Callable<Optional<R>> c : fallbackAttempts) {
            try {
                return c.call();
            } catch (Exception e) {
                log.warn("Error attempting fallback: {}", e.getMessage());
            }
        }

        return Optional.empty();
    }

    private void assertIsRetryable(Throwable e) {
        boolean isNotRetryable = exceptionsToNotRetry.stream()
                .anyMatch(clazz -> clazz.isInstance(e));

        if (isNotRetryable) {
            throw new RuntimeException(e);
        }

    }
}
