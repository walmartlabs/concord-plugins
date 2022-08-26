package com.walmartlabs.concord.plugins.akeyless;

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

import com.walmartlabs.concord.plugins.akeyless.model.TaskParams;
import com.walmartlabs.concord.plugins.akeyless.model.TaskParamsImpl;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class TaskParamsImplTest {

    @Test
    public void testGetSecretsParams() {
        Map<String, Object> cfg = new HashMap<>();
        cfg.put("txId", UUID.randomUUID().toString());
        cfg.put("action", "getSecrets");
        List<String> paths = new ArrayList<>(2);
        paths.add("/firstPath");
        paths.add("/second/path");
        cfg.put("paths", paths);


        TaskParams params = TaskParamsImpl.of(cfg, Collections.emptyMap(), Collections.emptyMap(), null);
        assertEquals(TaskParams.Action.GETSECRETS, params.action());

        TaskParams.GetSecretsParams getSecretsParams = (TaskParams.GetSecretsParams) params;
        assertEquals(2, getSecretsParams.paths().size());
        assertEquals("/second/path", getSecretsParams.paths().get(1));
    }
}
