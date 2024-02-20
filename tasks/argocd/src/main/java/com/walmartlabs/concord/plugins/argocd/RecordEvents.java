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

import com.walmartlabs.concord.ApiException;
import com.walmartlabs.concord.client.ProcessEventRequest;
import com.walmartlabs.concord.client.ProcessEventsApi;
import com.walmartlabs.concord.plugins.argocd.model.EventStatus;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

public class RecordEvents {

    private final static Logger log = LoggerFactory.getLogger(RecordEvents.class);
    private final static List<String> BLACK_LIST = Arrays.asList("auth", "spec", "helm");

    static void recordEvent(ProcessEventsApi processEventsApi, UUID instanceId, UUID correlationId,
                            EventStatus eventStatus, String error, TaskParamsImpl taskParams, TaskResult taskResult) throws IOException {
        Map<String, Object> inVarsMap = taskParams.variables.toMap();
        Map<String, Object> eventData = new HashMap<>();
        for (Map.Entry<String, Object> e : inVarsMap.entrySet()) {
            if (!BLACK_LIST.contains(e.getKey())) {
                eventData.put(e.getKey(), e.getValue());
            }
        }
        eventData.put("correlationId", correlationId);
        eventData.put("status", eventStatus.toString());
        eventData.put("error", error);

        if(taskParams.action() == TaskParams.Action.UPDATESPEC) {
            TaskParams.UpdateSpecParams updateParams = (TaskParams.UpdateSpecParams) taskParams;
            Map<String, Object> spec = updateParams.spec();
            if(spec != null){
                if(spec.containsKey("destination")) {
                    Map<String, Object> destination = (Map<String, Object>) spec.get("destination");
                    if(destination != null) {
                        eventData.put("cluster", destination.get("name"));
                        eventData.put("namespace", destination.get("namespace"));
                    }
                }
                eventData.put("project",spec.get("project"));
            }
        }
        try {
            processEventsApi.event(instanceId, new ProcessEventRequest()
                    .setEventType("ARGOCD")
                    .setData(eventData));
        } catch (ApiException e) {
            log.warn("recordEvents -> error while recording the event, ignoring: {}", e.getMessage());
        }
    }
}