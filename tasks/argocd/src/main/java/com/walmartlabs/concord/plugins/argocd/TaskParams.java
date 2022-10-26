package com.walmartlabs.concord.plugins.argocd;

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

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public interface TaskParams {

    interface AuthParams {
    }

    interface BasicAuth extends AuthParams {

        String username();

        String password();
    }

    interface LdapAuth extends AuthParams {

        @Value.Default
        default String connectorId() {
            return "ldap";
        }

        String username();

        String password();
    }

    interface TokenAuth extends AuthParams {

        String token();
    }

    int DEFAULT_CONNECT_TIMEOUT = 30;
    int DEFAULT_READ_TIMEOUT = 30;
    int DEFAULT_WRITE_TIMEOUT = 30;

    @Value.Default
    default Action action() {
        return Action.SYNC;
    }

    @Value.Default
    default boolean debug() {
        return false;
    }

    @Value.Default
    default boolean validateCerts() {
        return true;
    }

    @Value.Default
    default long connectTimeout() {
        return DEFAULT_CONNECT_TIMEOUT;
    }

    @Value.Default
    default long readTimeout() {
        return DEFAULT_READ_TIMEOUT;
    }

    @Value.Default
    default long writeTimeout() {
        return DEFAULT_WRITE_TIMEOUT;
    }

    @Value.Default
    default boolean recordEvents() {
        return false;
    }

    String baseUrl();

    AuthParams auth();

    interface GetParams extends TaskParams {

        String app();

        @Value.Default
        default boolean refresh() {
            return false;
        }
    }

    interface CreateUpdateParams extends TaskParams {

        String app();

        String cluster();

        String namespace();

        @Value.Default
        default boolean createNamespace() {
            return false;
        }

        @Value.Default
        default String project() {
            return "default";
        }

        @Nullable
        GitRepo gitRepo();

        @Nullable
        HelmRepo helmRepo();

        @Nullable
        Helm helm();

        @Nullable
        Duration syncTimeout();

        @Nullable
        Map<String, String> annotations();

        interface GitRepo {
            String repoUrl();

            String path();

            String targetRevision();
        }

        interface HelmRepo {
            String repoUrl();

            String targetRevision();

            String chart();
        }

        interface Helm {
            @Nullable
            List<HelmParams> parameters();

            String values();
        }

        interface HelmParams {
            String name();

            String value();
        }
    }

    interface PatchParams extends TaskParams {

        String app();

        List<Map<String, Object>> patches();
    }

    interface UpdateSpecParams extends TaskParams {

        String app();

        Map<String, Object> spec();
    }

    interface SetAppParams extends TaskParams {

        String app();

        List<HelmParam> helm();

        interface HelmParam {

            String name();

            Object value();
        }
    }

    interface SyncParams extends TaskParams {

        interface Resource {

            String group();

            String kind();

            String name();

            String namespace();
        }

        String app();

        @Nullable
        String revision();

        List<Resource> resources();

        @Value.Default
        default boolean dryRun() {
            return false;
        }

        @Value.Default
        default boolean prune() {
            return false;
        }

        @Value.Default
        default Map<String, Object> retryStrategy() {
            return Collections.emptyMap();
        }

        @Value.Default
        default Map<String, Object> strategy() {
            return Collections.emptyMap();
        }

        @Value.Default
        default boolean watchHealth() {
            return false;
        }

        @Nullable
        Duration syncTimeout();
    }

    interface DeleteAppParams extends TaskParams {

        String app();

        @Value.Default
        default boolean cascade() {
            return false;
        }

        @Nullable
        String propagationPolicy();
    }

    enum Action {
        DELETE,
        SYNC,
        GET,
        PATCH,
        UPDATESPEC,
        SETPARAMS,
        CREATE
    }
}
