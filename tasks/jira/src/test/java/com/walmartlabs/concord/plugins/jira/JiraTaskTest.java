package com.walmartlabs.concord.plugins.jira;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2019 Walmart Inc.
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

import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.walmartlabs.concord.sdk.Context;
import com.walmartlabs.concord.sdk.SecretService;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import java.util.HashMap;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class JiraTaskTest {
    @Rule
    public WireMockRule rule = new WireMockRule(wireMockConfig()
            .dynamicPort()
            .notifier(new ConsoleNotifier(true)));

    private JiraTask task;
    private Context mockContext = mock(Context.class);
    private SecretService secretService = Mockito.mock(SecretService.class);
    protected String response;

    @Before
    public void setup() {
        task = new JiraTask(secretService);
        stubForBasicAuth();
    }

    @After
    public void tearDown() {
        response = null;
    }


    @Test
    public void testJiraBasicAuth() throws Exception {
        Map<String, Object> auth = new HashMap<>();
        Map<String, Object> basic = new HashMap<>();
        basic.put("username", "user");
        basic.put("password", "pass");

        auth.put("basic", basic);
        String url = rule.baseUrl() + "/";
        initCxtForRequest(mockContext, "CREATEISSUE", url, "projKey", "summary", "description",
                "requestorUid", "bug", auth);

        task.execute(mockContext);
    }

    @Test
    public void testCreateIssueWithSecret() throws Exception {
        Map<String, Object> auth = new HashMap<>();
        Map<String, Object> secret = new HashMap<>();
        secret.put("name", "secret");
        secret.put("org", "organization");

        auth.put("secret", secret);

        String url = rule.baseUrl() + "/";
        initCxtForRequest(mockContext, "CREATEISSUE", url, "projKey", "summary", "description",
                "requestorUid", "bug", auth);
        task.execute(mockContext);
    }



    private void initCxtForRequest(Context ctx, Object action, Object apiUrl,  Object projectKey, Object summary, Object description,
                                        Object requestorUid, Object issueType, Object auth) throws Exception
    {
        when(ctx.getVariable("action")).thenReturn(action);
        when(ctx.getVariable("apiUrl")).thenReturn(apiUrl);
        when(ctx.getVariable("projectKey")).thenReturn(projectKey);
        when(ctx.getVariable("summary")).thenReturn(summary);
        when(ctx.getVariable("description")).thenReturn(description);
        when(ctx.getVariable("requestorUid")).thenReturn(requestorUid);
        when(ctx.getVariable("issueType")).thenReturn(issueType);
        when(ctx.getVariable("auth")).thenReturn(auth);

        doAnswer((Answer<Void>) invocation -> {
            response = (String) invocation.getArguments()[1];
            return null;
        }).when(ctx).setVariable(anyString(), any());

        doReturn(getCredentials()).when(secretService)
          .exportCredentials(any(), anyString(), anyString(), anyString(), anyString(), anyString());

    }

    private Map<String, String> getCredentials(){
        Map<String, String> credentials = new HashMap<>();
        credentials.put("username", "user");
        credentials.put("password", "pwd");
        return credentials;
    }

    private void stubForBasicAuth() {
        rule.stubFor(post(urlEqualTo("/issue/"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        //.withHeader("Accept", "application/json")
                        .withBody("{\n" +
                                "  \"id\": \"123\",\n"+
                                "  \"key\": \"key1\",\n" +
                                "  \"self\": \"2\"\n" +
                                "}"))
        );
    }

}
