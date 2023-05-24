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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.walmartlabs.concord.runtime.v2.sdk.MapBackedVariables;
import com.walmartlabs.concord.runtime.v2.sdk.Variables;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TaskParamsImpl implements TaskParams {

    public static TaskParams of(Variables input, Map<String, Object> defaults) {
        Map<String, Object> variablesMap = new HashMap<>(defaults != null ? defaults : Collections.emptyMap());
        variablesMap.putAll(input.toMap());

        Variables variables = new MapBackedVariables(variablesMap);
        switch (TaskParamsImpl.action(variables)) {
            case GET: {
                return new GetParamsImpl(variables);
            }
            case SYNC: {
                return new SyncParamsImpl(variables);
            }
            case DELETE: {
                return new DeleteParamsImpl(variables);
            }
            case PATCH: {
                return new PatchParamsImpl(variables);
            }
            case UPDATESPEC: {
                return new UpdateSpecParamsImpl(variables);
            }
            case SETPARAMS: {
                return new SetAppParamsImpl(variables);
            }
            case CREATE: {
                return new CreateParamsImpl(variables);
            }
            case GETPROJECT: {
                return new GetProjectParamsImpl(variables);
            }
            case CREATEPROJECT: {
                return new CreateProjectParamsImpl(variables);
            }
            case CREATEAPPLICATIONSET: {
                return new CreateUpdateApplicationSetParamsImpl(variables);
            }
            case GETAPPLICATIONSET: {
                return new GetApplicationSetParamsImpl(variables);
            }
            case DELETEAPPLICATIONSET: {
                return new DeleteApplicationSetParamsImpl(variables);
            }
            default: {
                throw new IllegalArgumentException("Unsupported action type: " + action(variables));
            }
        }
    }

    private static final String ACTION_KEY = "action";
    private static final String AUTH_KEY = "auth";
    private static final String BASE_URL_KEY = "baseUrl";
    private static final String CONNECTION_TIMEOUT_KEY = "connectTimeout";
    private static final String DEBUG_KEY = "debug";
    private static final String READ_TIMEOUT_KEY = "readTimeout";
    private static final String VALIDATE_CERTS_KEY = "validateCerts";
    private static final String WRITE_TIMEOUT_KEY = "writeTimeout";
    private static final String RECORD_EVENTS_KEY = "recordEvents";

    private static final Map<String, Function<Variables, AuthParams>> authBuilders = createBuilders();

    private static Map<String, Function<Variables, AuthParams>> createBuilders() {
        Map<String, Function<Variables, AuthParams>> result = new HashMap<>();
        result.put("basic", BasicAuthImpl::new);
        result.put("ldap", LdapAuthImpl::new);
        result.put("token", TokenAuthImpl::new);
        result.put("azure", AzureAuthImpl::new);
        return result;
    }

    protected final Variables variables;

    protected TaskParamsImpl(Variables variables) {
        this.variables = variables;
    }

    @Override
    public Action action() {
        return action(variables);
    }

    @Override
    public boolean debug() {
        return variables.getBoolean(DEBUG_KEY, TaskParams.super.debug());
    }

    @Override
    public boolean validateCerts() {
        return variables.getBoolean(VALIDATE_CERTS_KEY, TaskParams.super.validateCerts());
    }

    @Override
    public long connectTimeout() {
        return variables.getLong(CONNECTION_TIMEOUT_KEY, TaskParams.super.connectTimeout());
    }

    @Override
    public long readTimeout() {
        return variables.getLong(READ_TIMEOUT_KEY, TaskParams.super.readTimeout());
    }

    @Override
    public long writeTimeout() {
        return variables.getLong(WRITE_TIMEOUT_KEY, TaskParams.super.writeTimeout());
    }

    @Override
    public String baseUrl() {
        return variables.assertString(BASE_URL_KEY);
    }

    @Override
    public boolean recordEvents() {
        return variables.getBoolean(RECORD_EVENTS_KEY, true);
    }

    @Override
    public AuthParams auth() {
        Map<String, Object> auth = variables.assertMap(AUTH_KEY);
        if (auth.isEmpty()) {
            throw new IllegalArgumentException("Empty auth");
        }

        if (auth.size() != 1) {
            throw new IllegalArgumentException("Multi auth definition");
        }

        String authType = auth.keySet().iterator().next();

        Function<Variables, AuthParams> builder = authBuilders.get(authType);
        if (builder == null) {
            throw new IllegalArgumentException("Unknown auth type '" + authType + "'. Available: " + authBuilders.keySet());
        }

        Variables authParams;
        if (auth.get(authType) instanceof Map) {
            authParams = new MapBackedVariables(new MapBackedVariables(auth).assertMap(authType));
        } else {
            authParams = new MapBackedVariables(Collections.singletonMap(authType, auth.get(authType)));
        }
        return builder.apply(authParams);
    }

    private static class BasicAuthImpl implements BasicAuth {

        private static final String USERNAME_KEY = "username";
        private static final String PASSWORD_KEY = "password";

        private final Variables variables;

        private BasicAuthImpl(Variables variables) {
            this.variables = variables;
        }

        @Override
        public String username() {
            return variables.assertString(USERNAME_KEY);
        }

        @Override
        public String password() {
            return variables.assertString(PASSWORD_KEY);
        }
    }

    private static class LdapAuthImpl implements LdapAuth {

        private static final String USERNAME_KEY = "username";
        private static final String PASSWORD_KEY = "password";
        private static final String CONNECTOR_ID_KEY = "connectorId";

        private final Variables variables;

        private LdapAuthImpl(Variables variables) {
            this.variables = variables;
        }

        @Override
        public String connectorId() {
            return variables.getString(CONNECTOR_ID_KEY, LdapAuth.super.connectorId());
        }

        @Override
        public String username() {
            return variables.assertString(USERNAME_KEY);
        }

        @Override
        public String password() {
            return variables.assertString(PASSWORD_KEY);
        }
    }

    private static class AzureAuthImpl implements AzureAuth {

        private static final String USERNAME_KEY = "username";
        private static final String PASSWORD_KEY = "password";
        private static final String CLIENT_ID_KEY = "clientId";
        private static final String AUTHORITY_KEY = "authority";
        private static final String SCOPE_KEY = "scope";


        private final Variables variables;

        private AzureAuthImpl(Variables variables) {
            this.variables = variables;
        }

        @Override
        public String clientId() {
            return variables.assertString(CLIENT_ID_KEY);
        }

        @Override
        public String authority() {
            return variables.assertString(AUTHORITY_KEY);
        }

        @Override
        public Set<String> scope() {
            return new HashSet<>(variables.getCollection(SCOPE_KEY, AzureAuth.super.scope()));
        }

        @Override
        public String username() {
            return variables.assertString(USERNAME_KEY);
        }

        @Override
        public String password() {
            return variables.assertString(PASSWORD_KEY);
        }
    }

    private static class TokenAuthImpl implements TokenAuth {

        private static final String TOKEN_KEY = "token";

        private final Variables variables;

        private TokenAuthImpl(Variables variables) {
            this.variables = variables;
        }

        @Override
        public String token() {
            return variables.assertString(TOKEN_KEY);
        }
    }

    private static class GetParamsImpl extends TaskParamsImpl implements GetParams {

        private static final String APP_KEY = "app";
        private static final String REFRESH_KEY = "refresh";

        protected GetParamsImpl(Variables variables) {
            super(variables);
        }

        @Override
        public String app() {
            return variables.assertString(APP_KEY);
        }

        @Override
        public boolean refresh() {
            return variables.getBoolean(REFRESH_KEY, false);
        }
    }

    private static class GetProjectParamsImpl extends TaskParamsImpl implements GetProjectParams {

        private static final String PROJECT_KEY = "project";

        protected GetProjectParamsImpl(Variables variables) {
            super(variables);
        }

        @Override
        public String project() {
            return variables.assertString(PROJECT_KEY);
        }
    }

    private static class GetApplicationSetParamsImpl extends TaskParamsImpl implements GetApplicationSetParams {

        private static final String APPLICATIONSET_KEY = "applicationSet";

        protected GetApplicationSetParamsImpl(Variables variables) {
            super(variables);
        }

        @Override
        public String applicationSet() {
            return variables.assertString(APPLICATIONSET_KEY);
        }
    }

    private static class CreateUpdateApplicationSetParamsImpl extends CreateParamsImpl implements CreateUpdateApplicationSetParams {

        private static final String APPLICATION_SET_KEY = "applicationSet";
        private static final String UPSERT_KEY = "upsert";
        private static final String GENERATORS_KEY = "generators";
        private static final String STRATEGY_KEY = "strategy";
        private static final String PRESERVE_RESOURCES_ON_DELETEION_KEY = "preserveResourcesOnDeletion";

        protected CreateUpdateApplicationSetParamsImpl(Variables variables) {
            super(variables);
        }

        @Override
        public String applicationSet() {
            return variables.assertString(APPLICATION_SET_KEY);
        }


        @Override
        public List<Map<String, Object>> generators() {
            return variables.assertList(GENERATORS_KEY);
        }

        @Override
        public boolean preserveResourcesOnDeletion() {
            return variables.getBoolean(PRESERVE_RESOURCES_ON_DELETEION_KEY, true);
        }

        @Override
        public Map<String, Object> strategy() {
            return variables.getMap(STRATEGY_KEY, Collections.emptyMap());
        }

        @Override
        public String project() {
            return variables.assertString(PROJECT_KEY);
        }

        @Override
        public boolean upsert() {
            return variables.getBoolean(UPSERT_KEY, false);
        }

    }

    private static class DeleteApplicationSetParamsImpl extends TaskParamsImpl implements DeleteApplicationSetParams {

        private static final String APPLICATIONSET_KEY = "applicationSet";

        protected DeleteApplicationSetParamsImpl(Variables variables) {
            super(variables);
        }

        @Override
        public String applicationSet() {
            return variables.assertString(APPLICATIONSET_KEY);
        }
    }

    public static class CreateParamsImpl extends TaskParamsImpl implements CreateUpdateParams {

        private static final String APP_KEY = "app";
        private static final String NAMESPACE_KEY = "namespace";
        private static final String CREATE_NAMESPACE_KEY = "createNamespace";
        protected static final String PROJECT_KEY = "project";
        private static final String CLUSTER_KEY = "cluster";
        private static final String GIT_REPO_KEY = "gitRepo";
        private static final String HELM_REPO_KEY = "helmRepo";
        private static final String HELM_KEY = "helm";
        private static final String ANNOTATIONS_KEY = "annotations";
        private static final String SYNC_TIMEOUT_KEY = "syncTimeout";

        protected CreateParamsImpl(Variables variables) {
            super(variables);
        }

        @Override
        public String app() {
            return variables.assertString(APP_KEY);
        }

        @Override
        public String cluster() {
            return variables.assertString(CLUSTER_KEY);
        }

        @Override
        public String namespace() {
            return variables.assertString(NAMESPACE_KEY);
        }

        @Override
        public boolean createNamespace() {
            return variables.getBoolean(CREATE_NAMESPACE_KEY, CreateUpdateParams.super.createNamespace());
        }

        @Override
        public String project() {
            return variables.getString(PROJECT_KEY, "default");
        }

        @Nullable
        @Override
        public GitRepo gitRepo() {
            Map<String, Object> gitRepoMap = variables.getMap(GIT_REPO_KEY, Collections.emptyMap());

            if (gitRepoMap.isEmpty()) {
                return null;
            }
            MapBackedVariables gitRepo = new MapBackedVariables(gitRepoMap);
            return new GitRepoImpl(gitRepo);
        }

        @Nullable
        @Override
        public HelmRepo helmRepo() {
            Map<String, Object> helmRepoMap = variables.getMap(HELM_REPO_KEY, Collections.emptyMap());

            if (helmRepoMap.isEmpty()) {
                return null;
            }
            MapBackedVariables helmRepo = new MapBackedVariables(helmRepoMap);
            return new HelmRepoImpl(helmRepo);
        }

        @Nullable
        @Override
        public Helm helm() {
            Map<String, Object> helmMap = variables.getMap(HELM_KEY, Collections.emptyMap());

            if (helmMap.isEmpty()) {
                return null;
            }
            MapBackedVariables helm = new MapBackedVariables(variables.assertMap(HELM_KEY));
            return new HelmImpl(helm);
        }

        @Nullable
        @Override
        public Map<String, String> annotations() {
            Map<String, String> annotations = variables.getMap(ANNOTATIONS_KEY, Collections.emptyMap());

            if (annotations.isEmpty())
                return null;

            return annotations;
        }

        @Nullable
        @Override
        public Duration syncTimeout() {
            String value = variables.getString(SYNC_TIMEOUT_KEY);
            if (value == null) {
                return null;
            }

            return Duration.parse(value);
        }

        public static class GitRepoImpl implements GitRepo {

            private static final String REPO_URL_KEY = "repoUrl";
            private static final String PATH_KEY = "path";
            private static final String TARGET_REVISION_KEY = "targetRevision";

            private final Variables variables;

            private GitRepoImpl(Variables variables) {
                this.variables = variables;
            }

            @Override
            public String repoUrl() {
                return variables.assertString(REPO_URL_KEY);
            }

            @Override
            public String path() {
                return variables.assertString(PATH_KEY);
            }

            @Override
            public String targetRevision() {
                return variables.assertString(TARGET_REVISION_KEY);
            }
        }

        public static class HelmRepoImpl implements HelmRepo {

            private static final String REPO_URL_KEY = "repoUrl";
            private static final String TARGET_REVISION_KEY = "targetRevision";
            private static final String CHART_KEY = "chart";

            private final Variables variables;

            public HelmRepoImpl(Variables variables) {
                this.variables = variables;
            }

            @Override
            public String repoUrl() {
                return variables.assertString(REPO_URL_KEY);
            }

            @Override
            public String targetRevision() {
                return variables.assertString(TARGET_REVISION_KEY);
            }

            @Override
            public String chart() {
                return variables.assertString(CHART_KEY);
            }
        }

        private static class HelmImpl implements Helm {

            private static final String PARAMS_KEY = "parameters";
            private static final String VALUES_KEY = "values";

            private final Variables variables;

            public HelmImpl(Variables variables) {
                this.variables = variables;
            }

            @Override
            public List<Map<String, Object>> parameters() {
                return variables.getList(PARAMS_KEY, null);
            }

            @Override
            public String values() {
                return variables.assertString(VALUES_KEY);
            }
        }
    }

    private static class CreateProjectParamsImpl extends TaskParamsImpl implements CreateProjectParams {

        private static final String PROJECT_KEY = "project";
        private static final String NAMESPACE_KEY = "namespace";
        private static final String CLUSTER_KEY = "cluster";
        private static final String ANNOTATIONS_KEY = "annotations";
        private static final String DESCRIPTION_KEY = "description";
        private static final String SOURCE_REPOS_KEY = "sourceRepos";
        private static final String DESTINATIONS_KEY = "destinations";
        private static final String UPSERT_KEY = "upsert";

        protected CreateProjectParamsImpl(Variables variables) {
            super(variables);
        }

        @Override
        public String project() {
            return variables.assertString(PROJECT_KEY);
        }

        @Override
        public boolean upsert() {
            return variables.getBoolean(UPSERT_KEY, false);
        }

        @Override
        public String namespace() {
            return variables.getString(NAMESPACE_KEY, CreateProjectParams.super.namespace());
        }

        @Nullable
        @Override
        public String cluster() {
            return variables.getString(CLUSTER_KEY, null);
        }

        @Nullable
        @Override
        public String description() {
            return variables.getString(DESCRIPTION_KEY, null);
        }

        @Nullable
        @Override
        public Map<String, String> annotations() {
            return variables.getMap(ANNOTATIONS_KEY, Collections.emptyMap());
        }

        @Nullable
        @Override
        public List<String> sourceRepos() {
            return variables.getList(SOURCE_REPOS_KEY, CreateProjectParams.super.sourceRepos());
        }

        @Nullable
        @Override
        public List<Destinations> destinations() {
            List<Map<String, Object>> destinationList = variables.getList(DESTINATIONS_KEY, Collections.emptyList());

            if (destinationList.isEmpty()) {
                return null;
            }

            return destinationList.stream()
                    .map(MapBackedVariables::new)
                    .map(CreateProjectParamsImpl.DestinationsImpl::new)
                    .collect(Collectors.toList());
        }

        private static class DestinationsImpl implements Destinations {

            private static final String NAME_KEY = "name";
            private static final String SERVER_KEY = "server";
            private static final String NAMESPACE_KEY = "namespace";

            private final Variables variables;

            public DestinationsImpl(Variables variables) {
                this.variables = variables;
            }

            @Nullable
            @Override
            public String name() {
                return variables.getString(NAME_KEY, null);
            }

            @Nullable
            @Override
            public String namespace() {
                return variables.getString(NAMESPACE_KEY, null);
            }

            @Nullable
            @Override
            public String server() {
                return variables.getString(SERVER_KEY, null);
            }
        }
    }

    private static class PatchParamsImpl extends TaskParamsImpl implements PatchParams {

        private static final String APP_KEY = "app";
        private static final String PATCHES_KEY = "patches";

        protected PatchParamsImpl(Variables variables) {
            super(variables);
        }

        @Override
        public String app() {
            return variables.assertString(APP_KEY);
        }

        @Override
        @SuppressWarnings("unchecked")
        public List<Map<String, Object>> patches() {
            List<Object> patchesPlain = variables.assertList(PATCHES_KEY);

            List<Map<String, Object>> result = new ArrayList<>();
            for (Object p : patchesPlain) {
                if (!(p instanceof Map)) {
                    throw new IllegalArgumentException("Invalid variable type, expected: object, got: " + p.getClass());
                }

                result.add((Map<String, Object>) p);
            }

            return result;
        }
    }

    private static final class UpdateSpecParamsImpl extends TaskParamsImpl implements UpdateSpecParams {

        private static final String APP_KEY = "app";
        private static final String SPEC_KEY = "spec";

        protected UpdateSpecParamsImpl(Variables variables) {
            super(variables);
        }

        @Override
        public String app() {
            return variables.assertString(APP_KEY);
        }

        @Override
        public Map<String, Object> spec() {
            return variables.assertMap(SPEC_KEY);
        }
    }

    private static final class SetAppParamsImpl extends TaskParamsImpl implements SetAppParams {

        private static final String APP_KEY = "app";
        private static final String HELM_KEY = "helm";

        protected SetAppParamsImpl(Variables variables) {
            super(variables);
        }

        @Override
        public String app() {
            return variables.assertString(APP_KEY);
        }

        @Override
        public List<HelmParam> helm() {
            List<Map<String, Object>> params = variables.assertList(HELM_KEY);
            return params.stream()
                    .map(MapBackedVariables::new)
                    .map(HelmParamImpl::new)
                    .collect(Collectors.toList());
        }

        private static class HelmParamImpl implements HelmParam {

            private static final String NAME_KEY = "name";
            private static final String VALUE_KEY = "value";

            private final Variables variables;

            private HelmParamImpl(Variables variables) {
                this.variables = variables;
            }

            @Override
            public String name() {
                return variables.assertString(NAME_KEY);
            }

            @Override
            public Object value() {
                return variables.assertVariable(VALUE_KEY, Object.class);
            }
        }
    }

    private static class DeleteParamsImpl extends TaskParamsImpl implements DeleteAppParams {

        private static final String APP_KEY = "app";
        private static final String CASCADE_KEY = "cascade";
        private static final String PROPAGATION_POLICY = "propagationPolicy";

        protected DeleteParamsImpl(Variables variables) {
            super(variables);
        }

        @Override
        public String app() {
            return variables.assertString(APP_KEY);
        }

        @Override
        public boolean cascade() {
            return variables.getBoolean(CASCADE_KEY, DeleteAppParams.super.cascade());
        }

        @Override
        public String propagationPolicy() {
            return variables.getString(PROPAGATION_POLICY);
        }
    }

    private static class SyncParamsImpl extends TaskParamsImpl implements SyncParams {

        private static class ResourceImpl implements SyncParams.Resource {

            private static final String GROUP_KEY = "group";
            private static final String KIND_KEY = "kind";
            private static final String NAME_KEY = "name";
            private static final String NAMESPACE_KEY = "namespace";

            private final Variables variables;

            private ResourceImpl(Variables variables) {
                this.variables = variables;
            }

            @Override
            @JsonProperty("group")
            public String group() {
                return variables.getString(GROUP_KEY);
            }

            @Override
            @JsonProperty("kind")
            public String kind() {
                return variables.assertString(KIND_KEY);
            }

            @Override
            @JsonProperty("name")
            public String name() {
                return variables.assertString(NAME_KEY);
            }

            @Override
            @JsonProperty("namespace")
            public String namespace() {
                return variables.getString(NAMESPACE_KEY);
            }
        }

        private static final String APP_KEY = "app";
        private static final String REVISION_KEY = "revision";
        private static final String RETRY_STRATEGY_KEY = "retryStrategy";
        private static final String STRATEGY_KEY = "strategy";
        private static final String PRUNE_KEY = "prune";
        private static final String DRY_RUN_KEY = "dryRun";
        private static final String SYNC_TIMEOUT_KEY = "syncTimeout";
        private static final String RESOURCES_KEY = "resources";
        private static final String WATCH_HEALTH_KEY = "watchHealth";

        protected SyncParamsImpl(Variables variables) {
            super(variables);
        }

        @Override
        public String app() {
            return variables.assertString(APP_KEY);
        }

        @Override
        public String revision() {
            return variables.getString(REVISION_KEY);
        }

        @Override
        public boolean dryRun() {
            return variables.getBoolean(DRY_RUN_KEY, SyncParams.super.dryRun());
        }

        @Override
        public boolean prune() {
            return variables.getBoolean(PRUNE_KEY, SyncParams.super.prune());
        }

        @Override
        public Map<String, Object> retryStrategy() {
            return variables.getMap(RETRY_STRATEGY_KEY, SyncParams.super.retryStrategy());
        }

        @Override
        public Map<String, Object> strategy() {
            return variables.getMap(STRATEGY_KEY, SyncParams.super.strategy());
        }

        @Override
        public boolean watchHealth() {
            return variables.getBoolean(WATCH_HEALTH_KEY, SyncParams.super.watchHealth());
        }

        @Nullable
        @Override
        public Duration syncTimeout() {
            String value = variables.getString(SYNC_TIMEOUT_KEY);
            if (value == null) {
                return null;
            }

            return Duration.parse(value);
        }

        @Override
        public List<Resource> resources() {
            List<Map<String, Object>> params = variables.assertList(RESOURCES_KEY);
            return params.stream()
                    .map(MapBackedVariables::new)
                    .map((MapBackedVariables t) -> new ResourceImpl(variables))
                    .collect(Collectors.toList());
        }
    }


    private static Action action(Variables variables) {
        String action = variables.getString(ACTION_KEY, Action.SYNC.name());
        try {
            return Action.valueOf(action.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Unknown action: '" + action + "'. Available actions: " + Arrays.toString(Action.values()));
        }
    }
}
