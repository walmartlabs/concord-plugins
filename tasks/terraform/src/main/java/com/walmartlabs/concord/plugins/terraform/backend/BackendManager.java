package com.walmartlabs.concord.plugins.terraform.backend;

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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.walmartlabs.concord.plugins.terraform.Constants;
import com.walmartlabs.concord.sdk.LockService;
import com.walmartlabs.concord.sdk.MapUtils;
import com.walmartlabs.concord.sdk.ObjectStorage;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Named
@Singleton
public class BackendManager {

    private final static String DEFAULT_BACKEND = "concord";
    private final Set<String> supportedBackends;
    private final LockService lockService;
    private final ObjectStorage objectStorage;
    private final ObjectMapper objectMapper;

    @Inject
    public BackendManager(LockService lockService, ObjectStorage objectStorage) {
        this.lockService = lockService;
        this.objectStorage = objectStorage;
        this.supportedBackends = supportedBackends();
        this.objectMapper = new ObjectMapper();
    }

    public boolean isSupported(String backend) {
        return supportedBackends.contains(backend);
    }

    // Supported backend types:
    //
    // https://www.terraform.io/docs/backends/types/index.html
    private Set<String> supportedBackends() {
        Set<String> supportedBackends = new HashSet<>();
        supportedBackends.add("artifactory"); // https://www.terraform.io/docs/backends/types/artifactory.html
        supportedBackends.add("azurerm"); // https://www.terraform.io/docs/backends/types/azurerm.html
        supportedBackends.add("consul"); // https://www.terraform.io/docs/backends/types/consul.html
        supportedBackends.add("etcd"); // https://www.terraform.io/docs/backends/types/etcd.html
        supportedBackends.add("etcdv3"); // https://www.terraform.io/docs/backends/types/etcdv3.html
        supportedBackends.add("gcs"); // https://www.terraform.io/docs/backends/types/gcs.html
        supportedBackends.add("http"); // https://www.terraform.io/docs/backends/types/http.html
        supportedBackends.add("manta"); // https://www.terraform.io/docs/backends/types/manta.html
        supportedBackends.add("oss"); // https://www.terraform.io/docs/backends/types/oss.html
        supportedBackends.add("p3"); // https://www.terraform.io/docs/backends/types/pg.html
        supportedBackends.add("s3"); // https://www.terraform.io/docs/backends/types/s3.html
        supportedBackends.add("swift"); // https://www.terraform.io/docs/backends/types/swift.html
        supportedBackends.add("atlas"); // https://www.terraform.io/docs/backends/types/terraform-enterprise.html
        //
        // This requires special processing as it's not a strict set of key-value pairs, but if you're
        // using TF Enterprise you're likely not going to be using Concord but if you're migrating from
        // TF Enterprise to Concord you might want to use TFE's remote state engine as part
        // of a transition.
        //
        // supportedBackends.add("remote"); //https://www.terraform.io/docs/backends/types/remote.html
        //
        return supportedBackends;
    }

    public Backend getBackend(Map<String, Object> cfg) {
        boolean debug = MapUtils.get(cfg, Constants.DEBUG_KEY, false, Boolean.class);
        String backendId = DEFAULT_BACKEND;
        if (cfg.get(Constants.BACKEND_KEY) != null) {
            if (Map.class.isAssignableFrom(cfg.get(Constants.BACKEND_KEY).getClass())) {
                Map<String, Object> backend = MapUtils.getMap(cfg, Constants.BACKEND_KEY, null);
                if (backend.keySet().size() > 1) {
                    throw new IllegalArgumentException(
                            String.format("Only a single backend configuration is supported. There are %s configured.", backend.keySet().size()));
                }
                backendId = backend.keySet().iterator().next();
                if (isSupported(backendId)) {
                    // Retrieve the backend configuration parameters
                    return new SupportedBackend(debug, backendId, MapUtils.getMap(backend, backendId, null), objectMapper);
                }
            } else {
                backendId = MapUtils.getString(cfg, Constants.BACKEND_KEY, DEFAULT_BACKEND);
            }
        }
        switch (backendId) {
            case "none": {
                return new DummyBackend();
            }
            case "concord": {
                return new ConcordBackend(debug, lockService, objectStorage, objectMapper);
            }
            default: {
                throw new IllegalArgumentException("Unsupported backend type: " + backendId);
            }
        }
    }
}
