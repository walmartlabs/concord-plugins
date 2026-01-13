package com.walmartlabs.concord.plugins.confluence.model.auth;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2026 Walmart Inc., Concord Authors
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.walmartlabs.concord.plugins.confluence.TaskParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class AuthUtils {

    private static final Logger log = LoggerFactory.getLogger(AuthUtils.class);

    public static Auth parseAuth(TaskParams in, ObjectMapper mapper) {
        Map<String, Object> auth = in.auth();

        if (in.userId() != null && in.password() != null) {
            log.warn("Deprecated auth credentials provided. Please use the 'auth' input parameter");
            return new Auth(null, new BasicAuth(in.userId(), in.password()));
        }

        if (auth == null || auth.isEmpty()) {
            throw new IllegalArgumentException("Missing 'auth' input.");
        }

        if (auth.size() != 1) {
            throw new IllegalArgumentException("Invalid 'auth' input. Expected one element, got: " + auth.keySet());
        }

        return mapper.convertValue(auth, Auth.class);

    }
}
