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

import com.walmartlabs.concord.ApiClient;
import com.walmartlabs.concord.client.ProcessEventsApi;
import com.walmartlabs.concord.common.ConfigurationUtils;
import com.walmartlabs.concord.plugins.argocd.openapi.ApiException;
import com.walmartlabs.concord.plugins.argocd.openapi.model.*;
import com.walmartlabs.concord.runtime.v2.sdk.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Named("argocd")
public class ArgoCdTask implements Task {

    private final static Logger log = LoggerFactory.getLogger(ArgoCdTask.class);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Context context;
    private final LockService lockService;
    private final ProcessEventsApi processEventsApi;

    @Inject
    public ArgoCdTask(Context context, LockService lockService, ApiClient apiClient) {
        this.context = context;
        this.lockService = lockService;
        this.processEventsApi = new ProcessEventsApi(apiClient);
    }

    @Override
    public TaskResult execute(Variables input) throws Exception {
        TaskParams params = TaskParamsImpl.of(input, context.defaultVariables().toMap());

        switch (params.action()) {
            case GET: {
                return processGetAction((TaskParams.GetParams) params);
            }
            case SYNC: {
                return processSyncAction((TaskParams.SyncParams) params);
            }
            case DELETE: {
                return processDeleteAction((TaskParams.DeleteAppParams) params);
            }
            case PATCH: {
                return processPatchAction((TaskParams.PatchParams) params);
            }
            case UPDATESPEC: {
                return processUpdateSpecAction((TaskParams.UpdateSpecParams) params);
            }
            case SETPARAMS: {
                return processSetParamsAction((TaskParams.SetAppParams) params);
            }
            case CREATE: {
                return processCreateAction((TaskParams.CreateUpdateParams) params);
            }
            case GETPROJECT: {
                return processGetProjectAction((TaskParams.GetProjectParams) params);
            }
            case CREATEPROJECT: {
                return processProjectCreateAction((TaskParams.CreateProjectParams) params);
            }
            case DELETEPROJECT: {
                return processProjectDeleteAction((TaskParams.DeleteProjectParams) params);
            }
            case GETAPPLICATIONSET: {
                return processGetApplicationSetAction((TaskParams.GetApplicationSetParams) params);
            }
            case DELETEAPPLICATIONSET: {
                return processDeleteApplicationSetAction((TaskParams.DeleteApplicationSetParams) params);
            }
            case CREATEAPPLICATIONSET: {
                return processCreateApplicationSetAction((TaskParams.CreateUpdateApplicationSetParams) params);
            }
            default: {
                throw new IllegalArgumentException("Unsupported action type: " + params.action());
            }
        }
    }

    private TaskResult processGetApplicationSetAction(TaskParams.GetApplicationSetParams in) throws Exception {
        ArgoCdClient client = new ArgoCdClient(in);
        V1alpha1ApplicationSet applicationSet = client.getApplicationSet(in.applicationSet());
        return TaskResult.success()
                .value("applicationSet", toMap(applicationSet));
    }

    private TaskResult processDeleteApplicationSetAction(TaskParams.DeleteApplicationSetParams in) throws Exception {
        ArgoCdClient client = new ArgoCdClient(in);
        log.info("Deleting '{}' applicationset ", in.applicationSet());
        record(in.recordEvents(), in.applicationSet(), in.baseUrl(), in.action().toString());

        client.deleteApplicationSet(in.applicationSet());
        return TaskResult.success();
    }

    private TaskResult processCreateApplicationSetAction(TaskParams.CreateUpdateApplicationSetParams in) throws Exception {
        assertProjectInfo(context);
        lockService.projectLock(in.applicationSet());
        log.info("Updating '{}' applicationset spec", in.applicationSet());

        record(in.recordEvents(), in.applicationSet(), in.baseUrl(), in.action().toString());

        try {
            ArgoCdClient client = new ArgoCdClient(in);

            V1alpha1Application application = objectMapper.buildApplicationObject(in);

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("name", in.applicationSet());
            metadata.put("namespace", in.applicationSetNamespace());
            Map<String, Object> applicationSetMap = new HashMap<>();
            applicationSetMap.put("metadata", metadata);
            Map<String, Object> spec = new HashMap<>();
            spec.put("generators", in.generators());
            Map<String,Object> syncPolicy = new HashMap<>();
            syncPolicy.put("preserveResourcesOnDeletion", in.preserveResourcesOnDeletion());
            spec.put("syncPolicy",syncPolicy);
            spec.put("strategy", in.strategy());
            spec.put("template", application);
            applicationSetMap.put("spec", spec);
            applicationSetMap.put("status", in.status());

            V1alpha1ApplicationSet applicationSet = client.createApplicationSet(objectMapper.mapToModel(applicationSetMap,V1alpha1ApplicationSet.class), in.upsert());
            return TaskResult.success()
                    .value("spec", toMap(applicationSet));

        } finally {
            lockService.projectUnlock(in.applicationSet());
        }
    }

    private TaskResult processUpdateSpecAction(TaskParams.UpdateSpecParams in) throws Exception {
        assertProjectInfo(context);
        lockService.projectLock(in.app());
        log.info("Updating '{}' app spec", in.app());

        record(in.recordEvents(), in.app(), in.baseUrl(), in.action().toString());

        try {
            ArgoCdClient client = new ArgoCdClient(in);

            V1alpha1Application app = client.getApp(in.app(), false);
            Map<String, Object> appSpec = toMap(app.getSpec());

            appSpec = ConfigurationUtils.deepMerge(appSpec, in.spec());
            V1alpha1ApplicationSpec specObject = objectMapper.mapToModel(appSpec, V1alpha1ApplicationSpec.class);
            V1alpha1ApplicationSpec result  = client.updateAppSpec(in.app(),specObject);

            return TaskResult.success()
                    .value("spec", result);
        } finally {
            lockService.projectUnlock(in.app());
        }
    }

    private TaskResult processSetParamsAction(TaskParams.SetAppParams in) throws Exception {
        assertProjectInfo(context);
        lockService.projectLock(in.app());
        log.info("Setting '{}' app params", in.app());

        record(in.recordEvents(), in.app(), in.baseUrl(), in.action().toString());

        try {
            ArgoCdClient client = new ArgoCdClient(in);

            V1alpha1Application app = client.getApp(in.app(), false);
            List<V1alpha1HelmParameter> appHelmParams = app.getSpec().getSource().getHelm().getParameters();

            for (TaskParams.SetAppParams.HelmParam p : in.helm()) {
                addOrReplaceParam(appHelmParams, p);
            }

            V1alpha1ApplicationSpec appSpec = client.updateAppSpec(in.app(), app.getSpec());

            return TaskResult.success()
                    .value("spec", appSpec);
        } finally {
            lockService.projectUnlock(in.app());
        }
    }

    private TaskResult processGetAction(TaskParams.GetParams in) throws Exception {
        ArgoCdClient client = new ArgoCdClient(in);
        V1alpha1Application app = client.getApp(in.app(), in.refresh());
        return TaskResult.success()
                .value("app", toMap(app));
    }

    private TaskResult processGetProjectAction(TaskParams.GetProjectParams in) throws Exception {
        ArgoCdClient client = new ArgoCdClient(in);
        log.info("Getting '{}' project info", in.project());

        record(in.recordEvents(), in.project(), in.baseUrl(), in.action().toString());

        V1alpha1AppProject project = client.getProject(in.project());
        return TaskResult.success()
                .value("project", objectMapper.toMap(project));
    }

    private TaskResult processProjectDeleteAction(TaskParams.DeleteProjectParams in) throws Exception {
        ArgoCdClient client = new ArgoCdClient(in);
        log.info("Deleting '{}' project ", in.project());
        record(in.recordEvents(), in.project(), in.baseUrl(), in.action().toString());

        client.deleteProject(in.project());
        return TaskResult.success();
    }

    private TaskResult processSyncAction(TaskParams.SyncParams in) throws Exception {
        assertProjectInfo(context);
        lockService.projectLock(in.app());
        log.info("Synchronizing '{}' app", in.app());

        record(in.recordEvents(), in.app(), in.baseUrl(), in.action().toString());

        try {
            ArgoCdClient client = new ArgoCdClient(in);
            V1alpha1Application app = client.syncApp(in);
            app = client.waitForSync(in.app(), app.getMetadata().getResourceVersion(), in.syncTimeout(), toWatchParams(in.watchHealth()));
            return TaskResult.success()
                    .value("app", toMap(app));
        } finally {
            lockService.projectUnlock(in.app());
        }
    }

    private TaskResult processCreateAction(TaskParams.CreateUpdateParams in) throws Exception {
        assertProjectInfo(context);
        lockService.projectLock(in.app());
        log.info("Creating '{}' app", in.app());

        record(in.recordEvents(), in.app(), in.baseUrl(), in.action().toString());

        try {
            V1alpha1Application application = objectMapper.buildApplicationObject(in);
            ArgoCdClient client = new ArgoCdClient(in);
            V1alpha1Application app = client.createApp(application);
            app = client.waitForSync(in.app(), app.getMetadata().getResourceVersion(), in.syncTimeout(),
                    toWatchParams(false));
            return TaskResult.success()
                    .value("app", toMap(app));
        } finally {
            lockService.projectUnlock(in.app());
        }
    }

    private TaskResult processProjectCreateAction(TaskParams.CreateProjectParams in) throws Exception {
        assertProjectInfo(context);
        lockService.projectLock(in.project());
        log.info("Creating '{}' project", in.project());

        record(in.recordEvents(), in.project(), in.baseUrl(), in.action().toString());

        try {
            ArgoCdClient client = new ArgoCdClient(in);
            V1alpha1AppProject project = client.createProject(in);
            return TaskResult.success()
                    .value("project", objectMapper.toMap(project));
        } finally {
            lockService.projectUnlock(in.project());
        }
    }

    private TaskResult processDeleteAction(TaskParams.DeleteAppParams in) throws Exception {
        assertProjectInfo(context);
        lockService.projectLock(in.app());
        log.info("Deleting '{}' app", in.app());

        record(in.recordEvents(), in.app(), in.baseUrl(), in.action().toString());

        try {
            ArgoCdClient client = new ArgoCdClient(in);
            client.deleteApp(in.app(), in.cascade(), in.propagationPolicy());
            return TaskResult.success();
        } finally {
            lockService.projectUnlock(in.app());
        }
    }

    private TaskResult processPatchAction(TaskParams.PatchParams in) throws Exception {
        assertProjectInfo(context);
        lockService.projectLock(in.app());
        log.info("Patching '{}' app", in.app());

        record(in.recordEvents(), in.app(), in.baseUrl(), in.action().toString());

        try {
            ArgoCdClient client = new ArgoCdClient(in);
            client.patchApp(in.app(), in.patches());
            return TaskResult.success();
        } finally {
            lockService.projectUnlock(in.app());
        }
    }

    private Map<String, Object> toMap(Object app) {
        return objectMapper.toMap(app);
    }

    private void record(boolean recordEvents, String app, String baseUrl, String action) {
        if (recordEvents) {
            RecordEvents.recordEvent(
                    processEventsApi,
                    app,
                    baseUrl,
                    action,
                    context.execution().correlationId(),
                    context.processInstanceId());
        }
    }

    private static void addOrReplaceParam(List<V1alpha1HelmParameter> appHelmParams, TaskParams.SetAppParams.HelmParam p) {
        for (V1alpha1HelmParameter appParam : appHelmParams) {
            if (p.name().equals(appParam.getName())) {
                appParam.setValue(p.value().toString());
            }
        }
        V1alpha1HelmParameter helmParameter = new V1alpha1HelmParameter();
        helmParameter.setName(p.name());
        helmParameter.setValue(p.value().toString());
        appHelmParams.add(helmParameter);
    }

    private static WaitWatchParams toWatchParams(boolean watchHealth) {
        return WaitWatchParams.builder()
                .watchSync(true)
                .watchHealth(watchHealth)
                .watchOperation(false)
                .watchSuspended(false)
                .build();
    }

    private static void assertProjectInfo(Context ctx) {
        ProjectInfo projectInfo = ctx.processConfiguration().projectInfo();
        if (projectInfo == null || projectInfo.projectName() == null) {
            throw new IllegalArgumentException("Can't determine project info. Argo CD task can only be used for processes running in a project.");
        }
    }
}
