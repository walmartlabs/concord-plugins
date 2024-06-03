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

import java.util.*;
import java.util.concurrent.Callable;

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
        this.fallbackAttempts = Collections.singleton(fallbackAttempt);
        this.exceptionsToNotRetry = exceptionsToNotRetry;
    }

    /**
     * Attempts to call and return result from main call up-to the given number of tries.
     * If the fallback call successfully returns data, after a failure of the main call,
     * then the fallback result is returned immediately
     * @param maxTries number of attempts to make
     * @return Results of main call, or fallback result if data is present after main failure
     */
    public R attemptWithRetry(int maxTries) {
        Throwable lastError = null;

        int attemptsMade = 0;
        while (attemptsMade++ < maxTries) {
            try {
                R out = mainAttempt.call();
                return out;
            } catch (Exception e) {
                if(exceptionsToNotRetry.stream().anyMatch(exceptionToNotRetry -> exceptionToNotRetry.isInstance(e))) {
                    throw new RuntimeException(e);
                }
                lastError = e;
            }

            // if any of these returns cleanly then that's good enough
            for (Callable<Optional<R>> c : fallbackAttempts) {
                try {
                    Optional<R> result = c.call();

                    if (result.isPresent()) {
                        return result.get();
                    }
                } catch (Exception e) {
                    log.warn("Error attempting fallback: {}", e.getMessage());
                }
            }

            log.warn("Retrying [{}/{}]...", attemptsMade, maxTries);
        }

        throw new RuntimeException("Too many attempts. Last error: ", lastError);
    }
}
