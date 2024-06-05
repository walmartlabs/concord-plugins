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

import com.walmartlabs.concord.runtime.v2.sdk.MapBackedVariables;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class TaskParamsImplTest {

    @Test
    public void testReadTimeout() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("action", TaskParams.Action.GET.name());
        vars.put("readTimeout", 123);
        vars.put("recordEvents", false);

        TaskParams.GetParams in = (TaskParams.GetParams) TaskParamsImpl.of(new MapBackedVariables(vars), Collections.emptyMap());
        assertEquals(123, in.readTimeout());
        assertFalse(in.recordEvents());
    }

    @Test
    public void testTokenAuth() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("action", TaskParams.Action.GET.name());
        vars.put("auth", Collections.singletonMap("token", "iddqd"));

        TaskParams.GetParams in = (TaskParams.GetParams) TaskParamsImpl.of(new MapBackedVariables(vars), Collections.emptyMap());
        assertTrue(in.auth() instanceof TaskParams.TokenAuth);
        assertEquals("iddqd", ((TaskParams.TokenAuth)in.auth()).token());
    }

    @Test
    public void testBasicAuth() {
        Map<String, Object> authParams = new HashMap<>();
        authParams.put("username", "duke");
        authParams.put("password", "nukem");

        Map<String, Object> vars = new HashMap<>();
        vars.put("action", TaskParams.Action.GET.name());
        vars.put("auth", Collections.singletonMap("basic", authParams));

        TaskParams.GetParams in = (TaskParams.GetParams) TaskParamsImpl.of(new MapBackedVariables(vars), Collections.emptyMap());
        assertTrue(in.auth() instanceof TaskParams.BasicAuth);
        assertEquals("duke", ((TaskParams.BasicAuth)in.auth()).username());
        assertEquals("nukem", ((TaskParams.BasicAuth)in.auth()).password());
    }

    @Test
    public void testAzureAuth() {
        Map<String, Object> authParams = new HashMap<>();
        authParams.put("username", "duke");
        authParams.put("password", "nukem");
        authParams.put("clientId", "client-1");
        authParams.put("authority", "https://login.azure.com/cleint-1");
        authParams.put("scope", Collections.unmodifiableSet(new HashSet<>(Arrays.asList("user.read", "user.write"))));

        Map<String, Object> vars = new HashMap<>();
        vars.put("action", TaskParams.Action.GET.name());
        vars.put("auth", Collections.singletonMap("azure", authParams));

        TaskParams.GetParams in = (TaskParams.GetParams) TaskParamsImpl.of(new MapBackedVariables(vars), Collections.emptyMap());
        assertTrue(in.auth() instanceof TaskParams.AzureAuth);
        assertEquals("duke", ((TaskParams.AzureAuth)in.auth()).username());
        assertEquals("nukem", ((TaskParams.AzureAuth)in.auth()).password());
        assertEquals("client-1", ((TaskParams.AzureAuth)in.auth()).clientId());
        assertEquals("https://login.azure.com/cleint-1", ((TaskParams.AzureAuth)in.auth()).authority());
        assertTrue(((TaskParams.AzureAuth)in.auth()).scope().contains("user.read"));
    }


    @Test
    public void testUpdateSpecParams() {
        Map<String, Object> authParams = new HashMap<>();

        Map<String, Object> vars = new HashMap<>();
        vars.put("action", TaskParams.Action.UPDATESPEC.name());
        vars.put("auth", Collections.singletonMap("azure", authParams));
        vars.put("syncTimeout", "PT5M");

        TaskParams.UpdateSpecParams in = (TaskParams.UpdateSpecParams) TaskParamsImpl.of(new MapBackedVariables(vars), Collections.emptyMap());
        assertFalse(in.waitForSync());
        assertFalse(in.watchHealth());
        assertEquals(in.syncTimeout(), Duration.parse("PT5M"));
    }


    @Test
    public void testCreateSpecParams() {
        Map<String, Object> authParams = new HashMap<>();

        Map<String, Object> vars = new HashMap<>();
        vars.put("action", TaskParams.Action.CREATE.name());
        vars.put("auth", Collections.singletonMap("azure", authParams));
        vars.put("syncTimeout", "PT5M");

        TaskParams.CreateUpdateParams in = (TaskParams.CreateUpdateParams) TaskParamsImpl.of(new MapBackedVariables(vars), Collections.emptyMap());
        assertTrue(in.waitForSync());
        assertFalse(in.watchHealth());
        assertEquals(in.syncTimeout(), Duration.parse("PT5M"));
    }

}
