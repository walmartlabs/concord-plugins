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

/**
 * Created by ppendha on 3/23/19.
 */

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.walmartlabs.concord.common.ConfigurationUtils;
import com.walmartlabs.concord.sdk.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.walmartlabs.concord.sdk.ContextUtils.assertList;
import static com.walmartlabs.concord.sdk.ContextUtils.assertMap;

public class Utils {

    private static final Logger log = LoggerFactory.getLogger(GremlinTask.class);

    private static final Gson gson = new GsonBuilder().create();
    private static final String ATTACK_TARGET_LIST = "targetList";
    private static final String ATTACK_TARGET_TAGS = "targetTags";
    private static final String ATTACK_GUID = "attackGuid";

    public static String getAttackDetails(Context ctx, String apiUrl, String attackGuid) {
        Map<String, Object> results;
        try {
            results = new GremlinClient(ctx)
                    .url(apiUrl + "attacks/" + attackGuid)
                    .successCode(200)
                    .get();
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while getting attack details", e);
        }

        return gson.toJson(results);
    }

    public static String creatAttack(Context ctx, Map<String, Object> objAttack, Map<String, Object> objArgs,
                                     String targetType, String apiUrl) {

        String attackGuid;
        Map<String, Object> objTargetType;
        Map<String, Object> objTargetList;

        try {
            if (targetType.equalsIgnoreCase("Random")) {
                Map<String, String> targetTags = assertMap(ctx, ATTACK_TARGET_TAGS);
                objTargetType = Collections.singletonMap("type", "Random");
                objTargetList = Collections.singletonMap("tags", targetTags);
            } else {
                List<String> targetList = assertList(ctx, ATTACK_TARGET_LIST);
                objTargetType = Collections.singletonMap("type", "Exact");
                objTargetList = Collections.singletonMap("exact", targetList);
            }

            // Build command
            objAttack = ConfigurationUtils.deepMerge(objAttack, objArgs);
            Map<String, Object> objCommand = Collections.singletonMap("command", objAttack);

            // Build target
            objTargetType = ConfigurationUtils.deepMerge(objTargetType, objTargetList);
            Map<String, Object> objTarget = Collections.singletonMap("target", objTargetType);

            objCommand = ConfigurationUtils.deepMerge(objCommand, objTarget);

            Map<String, Object> results = new GremlinClient(ctx)
                    .url(apiUrl + "attacks/new")
                    .successCode(201)
                    .post(objCommand);

            attackGuid = results.get("results").toString();
            attackGuid = attackGuid.replaceAll("\"", "");
            ctx.setVariable(ATTACK_GUID, attackGuid);
            log.info("Gremlin Attack Guid: '{}'", ctx.getVariable(ATTACK_GUID));
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while creating attack", e);
        }

        return attackGuid;
    }

    public void halt(Context ctx, String apiUrl, String attackGuid) {
        try {
            new GremlinClient(ctx)
                    .url(apiUrl + "attacks/" + attackGuid)
                    .successCode(202)
                    .delete();
            log.info("Gremlin Attack with Guid# '{}' is halted....", ctx.getVariable(ATTACK_GUID));
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while halting attack", e);
        }
    }
}
