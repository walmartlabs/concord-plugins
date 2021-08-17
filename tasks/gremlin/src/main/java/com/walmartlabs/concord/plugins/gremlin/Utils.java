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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.walmartlabs.concord.common.ConfigurationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.walmartlabs.concord.plugins.gremlin.TaskParams.AttackParams;

public class Utils {

    private static final Logger log = LoggerFactory.getLogger(GremlinTask.class);

    private static final Gson gson = new GsonBuilder().create();

    public static String getAttackDetails(TaskParams in,String attackGuid) {
        Map<String, Object> results;
        try {
            results = new GremlinClient(in)
                    .url("attacks/" + attackGuid)
                    .successCode(200)
                    .get();
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while getting attack details", e);
        }

        return gson.toJson(results);
    }

    public static String createAttackOnHosts(AttackParams in, Map<String, Object> objAttack, Map<String, Object> objArgs) {

        String attackGuid;
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

            // Build command
            objAttack = ConfigurationUtils.deepMerge(objAttack, objArgs);
            Map<String, Object> objCommand = Collections.singletonMap("command", objAttack);

            // Build target
            objTargetType = ConfigurationUtils.deepMerge(objTargetType, objTargetList);
            Map<String, Object> objTarget = Collections.singletonMap("target", objTargetType);

            objCommand = ConfigurationUtils.deepMerge(objCommand, objTarget);

            Map<String, Object> results = new GremlinClient(in)
                    .url("attacks/new")
                    .successCode(201)
                    .post(objCommand);

            attackGuid = results.get("results").toString();
            attackGuid = attackGuid.replaceAll("\"", "");
            log.info("Gremlin Attack Guid: '{}'", attackGuid);
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while creating attack", e);
        }

        return attackGuid;
    }

    public static String createAttackOnContainers(AttackParams in, Map<String, Object> objAttack, Map<String, Object> objArgs) {
        String attackGuid;
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

            // Build command
            objAttack = ConfigurationUtils.deepMerge(objAttack, objArgs);
            Map<String, Object> objCommand = Collections.singletonMap("command", objAttack);

            // Build target
            objTargetType = ConfigurationUtils.deepMerge(objTargetType, objTargetContainers);
            objTargetType = ConfigurationUtils.deepMerge(objTargetType, objCount);

            Map<String, Object> objTarget = Collections.singletonMap("target", objTargetType);
            objCommand = ConfigurationUtils.deepMerge(objCommand, objTarget);

            Map<String, Object> results = new GremlinClient(in)
                    .url("attacks/new")
                    .successCode(201)
                    .post(objCommand);

            attackGuid = results.get("results").toString();
            attackGuid = attackGuid.replaceAll("\"", "");
            log.info("Gremlin Attack Guid: '{}'", attackGuid);
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while creating attack", e);
        }

        return attackGuid;
    }

    public static String createAttack(AttackParams in, Map<String, Object> objAttack, Map<String, Object> objArgs) {
        String attackGuid;
        if ("containers".equals(in.endPointType())) {
            attackGuid = createAttackOnContainers(in, objAttack, objArgs);
        } else if ("hosts".equals(in.endPointType())) {
            attackGuid = createAttackOnHosts(in, objAttack, objArgs);
        } else {
            throw new IllegalArgumentException("Invalid endPointType. Allowed values are only 'hosts', 'containers' ");
        }
        return attackGuid;
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
}
