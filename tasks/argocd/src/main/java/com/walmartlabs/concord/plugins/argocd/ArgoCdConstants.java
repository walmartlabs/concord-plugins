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

import java.util.*;

public class ArgoCdConstants {

    static final List<String> FINALIZERS = Collections.singletonList("resources-finalizer.argocd.argoproj.io");

    static final String CREATE_NAMESPACE_OPTION = "CreateNamespace=true";

    static final Map<String, Object> SYNC_POLICY = Map.of("automated", Map.of(
            "prune", true,
            "selfHeal", true
    ));

    static final String ARGOCD_NAMESPACE = "argocd";

    static final List<String> DEFAULT_SOURCE_REPOS = Collections.singletonList("*");

    static final List<Map<String, String>> DEFAULT_DESTINATIONS;

    static {
        Map<String, String> m = new HashMap<>();
        m.put("name", "*");
        m.put("server", "*");
        m.put("namespace", "*");
        DEFAULT_DESTINATIONS = Collections.singletonList(m);
    }
}
