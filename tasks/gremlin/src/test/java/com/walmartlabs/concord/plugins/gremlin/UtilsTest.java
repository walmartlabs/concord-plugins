package com.walmartlabs.concord.plugins.gremlin;

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

import com.walmartlabs.concord.runtime.v2.sdk.MapBackedVariables;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UtilsTest {

    @Test
    void filterK8sTargetsUsesStatefulSetAndPodNames() {
        var k8s = new TaskParams.KubernetesParams(new MapBackedVariables(Map.of(
                "cluster", "cluster-a",
                "namespace", "ns-a",
                "statefulSets", List.of("db"),
                "pods", List.of("worker")
        )));

        List<Map<String, Object>> result = Utils.filterK8sTargets(k8s, List.of(cluster(List.of(
                target("STATEFULSET", "db", "ns-a"),
                target("POD", "worker", "ns-a"),
                target("DAEMONSET", "db", "ns-a"),
                target("POD", "worker", "ns-b")
        ))));

        assertEquals(List.of("STATEFULSET", "POD"), result.stream().map(o -> o.get("kind")).toList());
    }

    @Test
    void filterK8sTargetsFailsWhenFiltersMatchNothing() {
        var k8s = new TaskParams.KubernetesParams(new MapBackedVariables(Map.of(
                "cluster", "cluster-a",
                "pods", List.of("missing")
        )));

        assertThrows(RuntimeException.class, () -> Utils.filterK8sTargets(k8s, List.of(cluster(List.of(
                target("POD", "worker", "ns-a")
        )))));
    }

    private static Map<String, Object> cluster(List<Map<String, Object>> objects) {
        return Map.of(
                "clusterId", "cluster-a",
                "objects", objects
        );
    }

    private static Map<String, Object> target(String kind, String name, String namespace) {
        return Map.of(
                "kind", kind,
                "name", name,
                "namespace", namespace
        );
    }
}
