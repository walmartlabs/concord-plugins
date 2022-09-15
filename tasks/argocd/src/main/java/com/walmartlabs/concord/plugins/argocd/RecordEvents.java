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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RecordEvents {

    private final static Logger log = LoggerFactory.getLogger(RecordEvents.class);

    static void recordEvent(ProcessEventsApi processEventsApi, String app, String argoUrl, String action,
                             UUID correlationId, UUID instanceId) {
        Map<String, Object> m = new HashMap<>();

        m.put("correlationId", correlationId);
        m.put("appName", app);
        m.put("argoInstanceUrl", argoUrl);
        m.put("action", action);

        try {
            processEventsApi.event(instanceId, new ProcessEventRequest()
                    .setEventType("ARGOCD")
                    .setData(m));
        } catch (ApiException e) {
            log.warn("recordEvents -> error while recording the event, ignoring: {}", e.getMessage());
        }
    }
}
