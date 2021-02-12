package com.walmartlabs.concord.plugins.hashivault.model;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2021 Walmart Inc., Concord Authors
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

import com.walmartlabs.concord.sdk.Context;
import com.walmartlabs.concord.sdk.SecretService;

import java.util.Map;

public class MockSecretServiceV1 implements SecretService {
    private final MockSecretServiceDelegate delegate;

    public MockSecretServiceV1(MockSecretServiceDelegate d) {
        this.delegate = d;
    }

    @Override
    public String exportAsString(Context ctx, String instanceId, String name, String password) throws Exception {
        throw new Exception("Not implemented");
    }

    @Override
    public String exportAsString(Context ctx, String instanceId, String o, String n, String p) throws Exception {
        return delegate.exportString(o, n, p);
    }

    @Override
    public Map<String, String> exportKeyAsFile(Context ctx, String instanceId, String workDir, String name, String password) throws Exception {
        throw new Exception("Not implemented");
    }

    @Override
    public Map<String, String> exportKeyAsFile(Context ctx, String instanceId, String workDir, String orgName, String name, String password) throws Exception {
        throw new Exception("Not implemented");
    }

    @Override
    public Map<String, String> exportCredentials(Context ctx, String instanceId, String workDir, String name, String password) throws Exception {
        throw new Exception("Not implemented");
    }

    @Override
    public Map<String, String> exportCredentials(Context ctx, String instanceId, String workDir, String orgName, String name, String password) throws Exception {
        throw new Exception("Not implemented");
    }

    @Override
    public String exportAsFile(Context ctx, String instanceId, String workDir, String name, String password) throws Exception {
        throw new Exception("Not implemented");
    }

    @Override
    public String exportAsFile(Context ctx, String instanceId, String workDir, String orgName, String name, String password) throws Exception {
        throw new Exception("Not implemented");
    }

    @Override
    public String decryptString(Context ctx, String instanceId, String s) throws Exception {
        throw new Exception("Not implemented");
    }

    @Override
    public String encryptString(Context ctx, String instanceId, String orgName, String projectName, String value) throws Exception {
        throw new Exception("Not implemented");
    }
}
