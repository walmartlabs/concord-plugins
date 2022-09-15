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
import com.walmartlabs.concord.plugins.argocd.model.Application;
import com.walmartlabs.concord.runtime.v2.sdk.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.*;

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
            default: {
                throw new IllegalArgumentException("Unsupported action type: " + params.action());
            }
        }
    }

    private TaskResult processUpdateSpecAction(TaskParams.UpdateSpecParams in) throws Exception {
        assertProjectInfo(context);
        lockService.projectLock(in.app());
        log.info("Updating '{}' app spec", in.app());

        record(in.recordEvents(), in.app(), in.baseUrl(), in.action().toString());

        try {
            ArgoCdClient client = new ArgoCdClient(in);
            String token = client.auth(in.auth());

            Application app = client.getApp(token, in.app(), false);
            Map<String, Object> appSpec = app.spec();

            appSpec = ConfigurationUtils.deepMerge(appSpec, in.spec());

            Map<String, Object> result = client.updateAppSpec(token, in.app(), appSpec);

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
            String token = client.auth(in.auth());

            Application app = client.getApp(token, in.app(), false);
            Map<String, Object> appSpec = app.spec();

            List<Map<String, Object>> appHelmParams = new ArrayList<>(MapUtils.get(appSpec, "source.helm.parameters", Collections.emptyList()));
            for (TaskParams.SetAppParams.HelmParam p : in.helm()) {
                addOrReplaceParam(appHelmParams, p);
            }
            appSpec = MapUtils.set(appSpec, "source.helm.parameters", appHelmParams);

            Map<String, Object> result = client.updateAppSpec(token, in.app(), appSpec);

            return TaskResult.success()
                    .value("spec", result);
        } finally {
            lockService.projectUnlock(in.app());
        }
    }

    private TaskResult processGetAction(TaskParams.GetParams in) throws Exception {
        ArgoCdClient client = new ArgoCdClient(in);
        log.info("Getting '{}' app info", in.app());
        String token = client.auth(in.auth());

        record(in.recordEvents(), in.app(), in.baseUrl(), in.action().toString());

        Application app = client.getApp(token, in.app(), in.refresh());
        return TaskResult.success()
                .value("app", toMap(app));
    }

    private TaskResult processSyncAction(TaskParams.SyncParams in) throws Exception {
        assertProjectInfo(context);
        lockService.projectLock(in.app());
        log.info("Synchronizing '{}' app", in.app());

        record(in.recordEvents(), in.app(), in.baseUrl(), in.action().toString());

        try {
            ArgoCdClient client = new ArgoCdClient(in);
            String token = client.auth(in.auth());
            Application app = client.syncApp(token, in);
            app = client.waitForSync(token, in.app(), app.resourceVersion(), in.syncTimeout(), toWatchParams(in.watchHealth()));
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
            ArgoCdClient client = new ArgoCdClient(in);
            String token = client.auth(in.auth());
            Application app = client.createApp(token, in);
            app = client.waitForSync(token, in.app(), app.resourceVersion(), in.syncTimeout(),
                    toWatchParams(false));
            return TaskResult.success()
                    .value("app", toMap(app));
        } finally {
            lockService.projectUnlock(in.app());
        }
    }

    private TaskResult processDeleteAction(TaskParams.DeleteAppParams in) throws Exception {
        assertProjectInfo(context);
        lockService.projectLock(in.app());
        log.info("Deleting '{}' app", in.app());

        record(in.recordEvents(), in.app(), in.baseUrl(), in.action().toString());

        try {
            ArgoCdClient client = new ArgoCdClient(in);
            String token = client.auth(in.auth());
            client.deleteApp(token, in.app(), in.cascade(), in.propagationPolicy());
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
            String token = client.auth(in.auth());
            client.patchApp(token, in.app(), in.patches());
            return TaskResult.success();
        } finally {
            lockService.projectUnlock(in.app());
        }
    }

    private Map<String, Object> toMap(Application app) {
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

    private static void addOrReplaceParam(List<Map<String, Object>> appHelmParams, TaskParams.SetAppParams.HelmParam p) {
        for (int i = 0; i < appHelmParams.size(); i++) {
            Map<String, Object> appParam = appHelmParams.get(i);
            if (p.name().equals(appParam.get("name"))) {
                appHelmParams.set(i, toMap(p));
                return;
            }
        }

        appHelmParams.add(toMap(p));
    }

    private static Map<String, Object> toMap(TaskParams.SetAppParams.HelmParam p) {
        Map<String, Object> result = new HashMap<>();
        result.put("name", p.name());
        result.put("value", p.value());
        return result;
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
