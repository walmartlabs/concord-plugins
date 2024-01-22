package com.walmartlabs.concord.plugins.argocd;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2024 Walmart Inc., Concord Authors
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

import com.walmartlabs.concord.plugins.argocd.openapi.ApiClient;
import com.walmartlabs.concord.plugins.argocd.openapi.api.ApplicationServiceApi;
import com.walmartlabs.concord.plugins.argocd.openapi.model.StreamResultOfV1alpha1ApplicationWatchEvent;
import com.walmartlabs.concord.plugins.argocd.openapi.model.V1ObjectMeta;
import com.walmartlabs.concord.plugins.argocd.openapi.model.V1alpha1Application;
import com.walmartlabs.concord.plugins.argocd.openapi.model.V1alpha1ApplicationStatus;
import com.walmartlabs.concord.plugins.argocd.openapi.model.V1alpha1ApplicationWatchEvent;
import com.walmartlabs.concord.plugins.argocd.openapi.model.V1alpha1HealthStatus;
import com.walmartlabs.concord.plugins.argocd.openapi.model.V1alpha1Operation;
import com.walmartlabs.concord.plugins.argocd.openapi.model.V1alpha1OperationState;
import com.walmartlabs.concord.plugins.argocd.openapi.model.V1alpha1SyncOperation;
import com.walmartlabs.concord.plugins.argocd.openapi.model.V1alpha1SyncStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArgoCdClientTest {

    private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER =
            new com.fasterxml.jackson.databind.ObjectMapper();

    @TempDir
    private Path tempDir;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    ApiClient apiClient;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    ApplicationServiceApi applicationServiceApi;

    @Test
    void testWaitForSync() throws Exception {
        mockStatusApi();
        mockApplicationServiceApi();

        WaitWatchParams wwParams = WaitWatchParams.builder()
                .watchSync(true)
                .watchHealth(true)
                .watchOperation(false)
                .watchSuspended(false)
                .build();

        Callable<V1alpha1Application> c = () -> ArgoCdClient.getAppWatchEvent("test-app", apiClient, null, wwParams, applicationServiceApi);

        IllegalStateException e = assertThrows(IllegalStateException.class, c::call);
        assertEquals("No sync status returned", e.getMessage());
        System.out.println(e);

        V1alpha1Application result = assertDoesNotThrow(c::call);
        assertNotNull(result.getMetadata());
        assertEquals("test-app", result.getMetadata().getName());
        assertEquals("test-ns", result.getMetadata().getNamespace());
    }

    private void mockStatusApi() throws Exception {
        StreamResultOfV1alpha1ApplicationWatchEvent payload = new StreamResultOfV1alpha1ApplicationWatchEvent()
                .result(new V1alpha1ApplicationWatchEvent()
                        .application(new V1alpha1Application()
                                .metadata(new V1ObjectMeta()
                                        .name("test-app")
                                        .name("test-ns"))
                                .operation(new V1alpha1Operation()
                                        .sync(new V1alpha1SyncOperation()
                                                .dryRun(false)))
                                .status(new V1alpha1ApplicationStatus()
                                        .sync(new V1alpha1SyncStatus()
                                                .status("Synced"))
                                        .health(new V1alpha1HealthStatus()
                                                .status("Healthy"))
                                        .reconciledAt(OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                                        .operationState(new V1alpha1OperationState()
                                                .finishedAt(OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))))));


        Path appFile = tempDir.resolve("appsync.json");
        MAPPER.writeValue(appFile.toFile(), payload);


        CloseableHttpResponse notReadyResp = mock(CloseableHttpResponse.class, RETURNS_DEEP_STUBS);
        // first return nothing, then return a valid response on subsequent call(s)
        when(notReadyResp.getEntity().getContent())
                .thenReturn(ArgoCdClientTest.class.getResourceAsStream("emptyResponse.json"), Files.newInputStream(appFile));

        when(apiClient.getHttpClient().execute(any()))
                .thenReturn(notReadyResp);

    }

    private void mockApplicationServiceApi() throws Exception {
        when(applicationServiceApi.applicationServiceGet(anyString(), anyString(), any(), any(), any(), any(), any()))
                .thenReturn(new V1alpha1Application()
                        .metadata(new V1ObjectMeta()
                                .name("test-app")
                                .namespace("test-ns")));
    }
}
