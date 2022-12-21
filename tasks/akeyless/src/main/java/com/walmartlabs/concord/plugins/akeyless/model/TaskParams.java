package com.walmartlabs.concord.plugins.akeyless.model;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2022 Walmart Inc., Concord Authors
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

import org.immutables.value.Value;

import java.util.Collections;
import java.util.List;

public interface TaskParams {

    int DEFAULT_CONNECT_TIMEOUT = 30;
    int DEFAULT_READ_TIMEOUT = 30;
    int DEFAULT_WRITE_TIMEOUT = 30;

    String DEFAULT_BASE_API = "https://api.akeyless.io";
    String DEFAULT_PARAMS_KEY = "akeylessParams";


    @Value.Default
    default Action action() {
        return Action.GETSECRET;
    }

    @Value.Default
    default boolean debug() {
        return false;
    }

    @Value.Default
    default boolean enableConcordSecretCache() {
        return true;
    }

    @Value.Default
    default int connectTimeout() {
        return DEFAULT_CONNECT_TIMEOUT;
    }

    @Value.Default
    default int readTimeout() {
        return DEFAULT_READ_TIMEOUT;
    }

    @Value.Default
    default int writeTimeout() {
        return DEFAULT_WRITE_TIMEOUT;
    }

    @Value.Default
    default String apiBasePath() {
        return DEFAULT_BASE_API;
    }

    @Value.Default
    default boolean ignoreCache() {
        return false;
    }

    /**
     * @return unique identifier for the current session. Must change each time
     * a process runs. For example, the value must change between execution before
     * and after suspending for a form.
     */
    String sessionId();
    String txId();
    Auth auth();
    String accessToken();

    interface GetSecretParams extends TaskParams {
        String path();
    }

    interface GetSecretsParams extends TaskParams {
        List<String> paths();
    }

    interface CreateSecretParams extends TaskParams {
        String path();
        String value();
        String description();
        boolean multiline();
        String protectionKey();

        @Value.Default
        default List<String> tags() {
            return Collections.emptyList();
        }
    }

    interface UpdateSecretParams extends TaskParams {
        String path();
        String value();
        String protectionKey();
        Boolean multiline();

        @Value.Default
        default boolean keepPreviousVersion() {
            return true;
        }
    }

    interface DeleteItemParams extends TaskParams {
        String path();
        Integer version();
        boolean deleteImmediately();
        Long deleteInDays();
    }

    enum Action {
        AUTH,
        CREATESECRET,
        DELETEITEM,
        GETSECRET,
        GETSECRETS,
        UPDATESECRET
    }
}
