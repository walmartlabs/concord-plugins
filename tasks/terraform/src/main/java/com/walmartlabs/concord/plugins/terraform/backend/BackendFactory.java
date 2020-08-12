package com.walmartlabs.concord.plugins.terraform.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.walmartlabs.concord.plugins.terraform.TaskConstants;
import com.walmartlabs.concord.sdk.MapUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class BackendFactory {

    private final static String DEFAULT_BACKEND = "concord";

    private final Set<String> commonBackends;
    private final ObjectMapper objectMapper;

    public BackendFactory() {
        this.commonBackends = commonBackends();
        this.objectMapper = new ObjectMapper();
    }

    public Backend getBackend(Map<String, Object> cfg) {
        boolean debug = MapUtils.get(cfg, TaskConstants.DEBUG_KEY, false, Boolean.class);

        String backendId = DEFAULT_BACKEND;
        if (cfg.get(TaskConstants.BACKEND_KEY) != null) {
            // check if the `backend` value is a map/object
            if (Map.class.isAssignableFrom(cfg.get(TaskConstants.BACKEND_KEY).getClass())) {
                Map<String, Object> backend = MapUtils.getMap(cfg, TaskConstants.BACKEND_KEY, null);
                if (backend.keySet().size() > 1) {
                    throw new IllegalArgumentException(
                            String.format("Only a single backend configuration is supported. There are %s configured.", backend.keySet().size()));
                }

                backendId = backend.keySet().iterator().next();

                if (isCommon(backendId)) {
                    return new CommonBackend(backendId, debug, MapUtils.getMap(backend, backendId, null), objectMapper);
                } else if (backendId.equals("remote")) {
                    return createRemoteBackend(backendId, debug, cfg, objectMapper);
                }
            } else {
                // use the literal `backend` value
                backendId = MapUtils.getString(cfg, TaskConstants.BACKEND_KEY, DEFAULT_BACKEND);
            }
        }

        switch (backendId) {
            case "none": {
                return new DummyBackend();
            }
            case "concord": {
                return createConcordBackend(debug, cfg, objectMapper);
            }
            default: {
                throw new IllegalArgumentException("Unsupported backend type: " + backendId);
            }
        }
    }

    protected abstract Backend createRemoteBackend(String backendId, boolean debug, Map<String, Object> cfg, ObjectMapper objectMapper);

    protected abstract Backend createConcordBackend(boolean debug, Map<String, Object> cfg, ObjectMapper objectMapper);

    private boolean isCommon(String backend) {
        return commonBackends.contains(backend);
    }

    // Supported backend types:
    //
    // https://www.terraform.io/docs/backends/types/index.html
    private static Set<String> commonBackends() {
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
        return supportedBackends;
    }
}
