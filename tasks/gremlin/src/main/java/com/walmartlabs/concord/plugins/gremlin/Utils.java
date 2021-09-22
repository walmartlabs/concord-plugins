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

import com.walmartlabs.concord.common.ConfigurationUtils;
import com.walmartlabs.concord.sdk.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static com.walmartlabs.concord.plugins.gremlin.TaskParams.AttackParams;
import static com.walmartlabs.concord.plugins.gremlin.TaskParams.KubernetesParams;
import static com.walmartlabs.concord.plugins.gremlin.Utils.K8S_TARGET_KIND.*;
import static com.walmartlabs.concord.sdk.MapUtils.*;

public class Utils {

    private static final Logger log = LoggerFactory.getLogger(GremlinTask.class);

    private static final String ATTACK_GUID = "attackGuid";
    private static final String ATTACK_DETAILS = "attackDetails";

    enum K8S_TARGET_KIND {
        DAEMONSET,
        DEPLOYMENT,
        STATEFULSET,
        POD
    }

    private static final Set<String> K8S_TARGET_KINDS = Arrays.stream(K8S_TARGET_KIND.values()).map(Enum::name).collect(Collectors.toSet());

    public static Map<String, Object> performAttack(AttackParams in, String type, List<String> params) {
        AttackResult attack = createAttack(in, type, params);
        String attackGuid = attack.id();
        String attackDetails = attack.details(in);
        log.info("URL of Gremlin Attack report: {}", in.appUrl() + attackGuid);

        Map<String, Object> result = new HashMap<>();
        result.put(ATTACK_DETAILS, attackDetails);
        result.put(ATTACK_GUID, attackGuid);
        return result;
    }

    private static AttackResult createAttack(AttackParams in, String type, List<String> params) {
        switch (in.endPointType()) {
            case HOSTS:
                return createAttackOnHosts(in, type, params);
            case CONTAINERS:
                return createAttackOnContainers(in, type, params);
            case KUBERNETES:
                return createAttackOnK8s(in, type, params);
            default:
                throw new IllegalArgumentException("Unknown endPointType: '" + in.endPointType() + "'");
        }
    }

    public void halt(TaskParams.HaltParams in) {
        try {
            new GremlinClient(in)
                    .url("attacks/" + in.attackGuid())
                    .successCode(202)
                    .delete();
            log.info("Gremlin Attack with Guid# '{}' is halted....", in.attackGuid());
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while halting attack", e);
        }
    }

    private static AttackResult createAttackOnHosts(AttackParams in, String type, List<String> params) {
        Map<String, Object> objTargetType;
        Map<String, Object> objTargetList;

        try {
            if (in.targetType().equalsIgnoreCase("Random")) {
                objTargetType = Collections.singletonMap("type", "Random");
                objTargetList = Collections.singletonMap("tags", in.targetTags());
            } else {
                objTargetType = Collections.singletonMap("type", "Exact");
                objTargetList = Collections.singletonMap("exact", in.targetList());
            }

            // Build target
            objTargetType = ConfigurationUtils.deepMerge(objTargetType, objTargetList);

            Map<String, Object> request = new HashMap<>();
            request.put("target", objTargetType);
            request.put("command", buildCommand(type, params));

            Map<String, Object> results = new GremlinClient(in)
                    .url("attacks/new")
                    .successCode(201)
                    .post(request);

            String attackGuid = results.get("results").toString();
            attackGuid = attackGuid.replaceAll("\"", "");
            log.info("Gremlin Attack Guid: '{}'", attackGuid);
            return new AttackResult(attackGuid);
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while creating attack", e);
        }
    }

    private static Map<String, Object> buildCommand(String type, List<String> params) {
        Map<String, Object> result = new HashMap<>();
        result.put("type", type);
        result.put("args", params);
        return result;
    }

    private static AttackResult createAttackOnContainers(AttackParams in, String type, List<String> params) {
        Map<String, Object> objTargetType;
        Map<String, Object> objTargetContainers;
        Map<String, Object> objCount = new HashMap<>();

        try {
            if (in.targetType().equalsIgnoreCase("Exact")) {
                objTargetType = Collections.singletonMap("type", "Exact");
                Map<String, Object> objContainersIds = Collections.singletonMap("ids", in.containerIds());
                objTargetContainers = Collections.singletonMap("containers", objContainersIds);
            } else {
                objTargetType = Collections.singletonMap("type", "Random");

                Integer percent = in.containerPercent();
                if (percent != null) {
                    objCount = Collections.singletonMap("percent", percent);
                }

                Integer count = in.containerCount(percent == null ? 1 : null);
                if (count != null) {
                    objCount = Collections.singletonMap("exact", count);
                }

                Map<String, Object> objLabel = Collections.singletonMap("labels", in.containerLabels());
                objTargetContainers = Collections.singletonMap("containers", objLabel);
            }

            // Build target
            objTargetType = ConfigurationUtils.deepMerge(objTargetType, objTargetContainers);
            objTargetType = ConfigurationUtils.deepMerge(objTargetType, objCount);

            Map<String, Object> request = new HashMap<>();
            request.put("target", objTargetType);
            request.put("command", buildCommand(type, params));

            Map<String, Object> results = new GremlinClient(in)
                    .url("attacks/new")
                    .successCode(201)
                    .post(request);

            String attackGuid = results.get("results").toString();
            attackGuid = attackGuid.replaceAll("\"", "");
            log.info("Gremlin Attack Guid: '{}'", attackGuid);
            return new AttackResult(attackGuid);
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while creating attack", e);
        }
    }

    private static AttackResult createAttackOnK8s(AttackParams in, String attackType, List<String> attackArgs) {
        List<Map<String, Object>> targets = resolveK8sTargets(in);

        KubernetesParams.SelectionType selectionType = in.k8s().selectionType();
        Map<String, Object> containerSelection = Collections.singletonMap("selectionType", selectionType.name());

        Map<String, Object> strategy = new HashMap<>();
        strategy.put("k8sObjects", targets);
        strategy.put("percentage", 100);
        strategy.put("containerSelection", containerSelection);

        List<String> cliArgs = new ArrayList<>(attackArgs);
        cliArgs.add(0, attackType);

        Map<String, Object> request = new HashMap<>();
        request.put("impactDefinition", Collections.singletonMap("cliArgs", cliArgs));
        request.put("targetDefinition", Collections.singletonMap("strategy", strategy));

        log.info("Creating Attack on {} objects", targets.size());

        try {
            Map<String, Object> results = new GremlinClient(in)
                    .url("kubernetes/attacks/new")
                    .successCode(201)
                    .post(request);
            String attackGuid = results.get("results").toString();
            attackGuid = attackGuid.replaceAll("\"", "");
            log.info("Gremlin Attack Guid: '{}'", attackGuid);
            return new K8SAttackResult(attackGuid);
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while creating attack", e);
        }
    }

    private static List<Map<String, Object>> resolveK8sTargets(AttackParams in) {
        List<Map<String, Object>> targets;
        try {
            Map<String, Object> result = new GremlinClient(in)
                    .url("kubernetes/targets")
                    .successCode(200)
                    .get();
            targets = MapUtils.getList(result, "results", Collections.emptyList());
        } catch (Exception e) {
            throw new RuntimeException("Error while resolving targets: " + e.getMessage(), e);
        }

        if (targets.isEmpty()) {
            throw new RuntimeException("No targets");
        }

        KubernetesParams k8s = in.k8s();
        Map<String, Object> cluster = findK8sCluster(k8s.cluster(), targets);
        if (cluster == null) {
            throw new IllegalArgumentException("Can't find cluster with name '" + cluster + "'");
        }

        List<Map<String, Object>> objects = assertList(cluster, "objects");
        objects = objects.stream()
                .filter(o -> K8S_TARGET_KINDS.contains(getString(o, "kind")))
                .collect(Collectors.toList());

        if (k8s.namespace() != null) {
            objects = objects.stream()
                    .filter(o -> k8s.namespace().equals(getString(o, "namespace")))
                    .collect(Collectors.toList());
        }

        List<Map<String, Object>> result = new ArrayList<>();
        if (!k8s.deployments().isEmpty()) {
            result = objects.stream()
                    .filter(o -> DEPLOYMENT.name().equals(getString(o, "kind")))
                    .filter(o -> k8s.deployments().contains(getString(o, "name")))
                    .collect(Collectors.toList());
        }

        if (!k8s.daemonSets().isEmpty()) {
            result = objects.stream()
                    .filter(o -> DAEMONSET.name().equals(getString(o, "kind")))
                    .filter(o -> k8s.daemonSets().contains(getString(o, "name")))
                    .collect(Collectors.toList());
        }

        if (!k8s.statefulSets().isEmpty()) {
            result = objects.stream()
                    .filter(o -> STATEFULSET.name().equals(getString(o, "kind")))
                    .filter(o -> k8s.daemonSets().contains(getString(o, "name")))
                    .collect(Collectors.toList());
        }

        if (!k8s.pods().isEmpty()) {
            result = objects.stream()
                    .filter(o -> POD.name().equals(getString(o, "kind")))
                    .filter(o -> k8s.daemonSets().contains(getString(o, "name")))
                    .collect(Collectors.toList());
        }

        if (targets.isEmpty()) {
            throw new RuntimeException("Filtered no targets");
        }

        return result;
    }

    private static Map<String, Object> findK8sCluster(String cluster, List<Map<String, Object>> items) {
        for (Map<String, Object> item : items) {
            if (cluster.equals(item.get("clusterId")) && !getList(item, "objects", Collections.emptyList()).isEmpty()) {
                return item;
            }
        }
        return null;
    }
}
