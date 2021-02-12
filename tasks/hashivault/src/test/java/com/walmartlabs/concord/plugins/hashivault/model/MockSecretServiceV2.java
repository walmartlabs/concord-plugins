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

import com.walmartlabs.concord.runtime.v2.sdk.SecretService;

import java.nio.file.Path;

public class MockSecretServiceV2 implements SecretService {
    private final MockSecretServiceDelegate delegate;

    public MockSecretServiceV2(MockSecretServiceDelegate d) {
        this.delegate = d;
    }

    @Override
    public SecretCreationResult createKeyPair(SecretParams secret, KeyPair keyPair) throws Exception {
        throw new Exception("Not implemented");
    }

    @Override
    public SecretCreationResult createUsernamePassword(SecretParams secret, UsernamePassword usernamePassword) throws Exception {
        throw new Exception("Not implemented");
    }

    @Override
    public SecretCreationResult createData(SecretParams secret, byte[] data) throws Exception {
        throw new Exception("Not implemented");
    }

    @Override
    public String exportAsString(String o, String n, String p) throws Exception {
        return delegate.exportString(o, n, p);
    }

    @Override
    public KeyPair exportKeyAsFile(String orgName, String secretName, String password) throws Exception {
        throw new Exception("Not implemented");
    }

    @Override
    public UsernamePassword exportCredentials(String orgName, String secretName, String password) throws Exception {
        throw new Exception("Not implemented");
    }

    @Override
    public Path exportAsFile(String orgName, String secretName, String password) throws Exception {
        throw new Exception("Not implemented");
    }

    @Override
    public String decryptString(String encryptedValue) throws Exception {
        throw new Exception("Not implemented");
    }

    @Override
    public String encryptString(String orgName, String projectName, String value) throws Exception {
        throw new Exception("Not implemented");
    }
}
