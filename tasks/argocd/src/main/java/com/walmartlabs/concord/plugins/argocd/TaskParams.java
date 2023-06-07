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
import java.util.Set;

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

    interface AzureAuth extends AuthParams {

        String clientId();

        String authority();

        @Value.Default
        default Set<String> scope() { return Collections.singleton("user.read"); }

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

    Map<String, String> addlParams();

    interface ApplicationParams extends TaskParams {
        String app();
    }

    interface ApplicationSetParams extends TaskParams {
        String applicationSet();
    }

    interface ProjectParams extends TaskParams {
        String project();
    }

    interface GetParams extends ApplicationParams {
        @Value.Default
        default boolean refresh() {
            return false;
        }
    }

    interface CreateUpdateParams extends ApplicationParams {

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
            List<Map<String, Object>> parameters();

            String values();
        }
    }

    interface PatchParams extends ApplicationParams {

        List<Map<String, Object>> patches();
    }

    interface UpdateSpecParams extends ApplicationParams {

        Map<String, Object> spec();
    }

    interface SetAppParams extends ApplicationParams {

        List<HelmParam> helm();

        interface HelmParam {

            String name();

            Object value();
        }
    }

    interface SyncParams extends ApplicationParams {

        interface Resource {

            String group();

            String kind();

            String name();

            String namespace();
        }

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

    interface DeleteAppParams extends ApplicationParams {

        @Value.Default
        default boolean cascade() {
            return false;
        }

        @Nullable
        String propagationPolicy();
    }

    interface CreateUpdateApplicationSetParams extends ApplicationSetParams,CreateUpdateParams {

        String applicationSetNamespace();

        List<Map<String, Object>> generators();

        boolean preserveResourcesOnDeletion();

        Map<String, Object> strategy();

        Map<String, Object> status();

        boolean upsert();
    }

    interface CreateProjectParams extends ProjectParams {

        boolean upsert();

        @Value.Default
        default String namespace() {
            return "argocd";
        }

        @Nullable
        String cluster();

        @Nullable
        String description();

        @Nullable
        Map<String, String> annotations();

        @Value.Default
        default List<String> sourceRepos() {
            return Collections.emptyList();
        }

        @Nullable
        List<Destinations> destinations();

        interface Destinations {

            @Nullable
            String name();

            @Nullable
            String namespace();

            @Nullable
            String server();
        }
    }

    enum Action {
        DELETE,
        SYNC,
        GET,
        PATCH,
        UPDATESPEC,
        SETPARAMS,
        CREATE,
        GETPROJECT,
        CREATEPROJECT,
        DELETEPROJECT,
        GETAPPLICATIONSET,
        DELETEAPPLICATIONSET,
        CREATEAPPLICATIONSET
    }
}
