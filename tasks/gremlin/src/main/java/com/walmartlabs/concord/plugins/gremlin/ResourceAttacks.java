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

import com.walmartlabs.concord.sdk.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.walmartlabs.concord.plugins.gremlin.Utils.createAttack;
import static com.walmartlabs.concord.plugins.gremlin.Utils.getAttackDetails;
import static com.walmartlabs.concord.sdk.ContextUtils.*;

public class ResourceAttacks {

    private static final Logger log = LoggerFactory.getLogger(GremlinTask.class);
    private static final String ATTACK_LENGTH = "length";
    private static final String ATTACK_TARGET_TYPE = "targetType";
    private static final String ATTACK_CPU_CORES = "cores";
    private static final String ATTACK_MEMORY_UNIT_OPTIONS = "unitOption";
    private static final String ATTACK_MEMORY_UNITS = "memoryUnits";
    private static final String ATTACK_MEMORY_PERCENT = "memoryPercent";
    private static final String ATTACK_DISK_PERCENT = "percent";
    private static final String ATTACK_IO_BLOCK_COUNT = "blockCount";
    private static final String ATTACK_IO_MODE = "mode";
    private static final String ATTACK_DIR = "dir";
    private static final String ATTACK_WORKERS = "workers";
    private static final String ATTACK_BLOCK_SIZE = "blockSize";
    private static final String ATTACK_GUID = "attackGuid";
    private static final String ATTACK_DETAILS = "attackDetails";
    private static final String ATTACK_ENDPOINT_TYPE = "endPointType";


    public void cpu(Context ctx, String apiUrl, String appUrl) {
        int cores = assertInt(ctx, ATTACK_CPU_CORES);
        int length = assertInt(ctx, ATTACK_LENGTH);
        String targetType = getString(ctx, ATTACK_TARGET_TYPE, Constants.GREMLIN_DEFAULT_TARGET_TYPE);
        String endPointType = getString(ctx, ATTACK_ENDPOINT_TYPE, Constants.GREMLIN_DEFAULT_ENDPOINT_TYPE);

        List<String> args = new ArrayList<>(Arrays.asList("-l", Integer.toString(length), "-c", Integer.toString(cores)));

        Map<String, Object> objAttack = Collections.singletonMap("type", "cpu");
        Map<String, Object> objArgs = Collections.singletonMap("args", args);

        String attackGuid = createAttack(ctx, objAttack, objArgs, targetType, apiUrl, endPointType);
        String attackDetails = getAttackDetails(ctx, apiUrl, attackGuid);
        ctx.setVariable(ATTACK_DETAILS, attackDetails);
        log.info("URL of Gremlin Attack report: " + appUrl + ctx.getVariable(ATTACK_GUID));
    }

    public void memory(Context ctx, String apiUrl, String appUrl) {
        String unitOption = assertString(ctx, ATTACK_MEMORY_UNIT_OPTIONS);
        int length = assertInt(ctx, ATTACK_LENGTH);
        String targetType = getString(ctx, ATTACK_TARGET_TYPE, Constants.GREMLIN_DEFAULT_TARGET_TYPE);
        String endPointType = getString(ctx, ATTACK_ENDPOINT_TYPE, Constants.GREMLIN_DEFAULT_ENDPOINT_TYPE);

        List<String> args = new ArrayList<>(Arrays.asList("-l", Integer.toString(length)));
        List<String> validUnitOption = Constants.GREMLIN_VALID_UNIT_OPTION;

        if (validUnitOption.contains(unitOption.toUpperCase())) {
            unitOption = unitOption.toUpperCase();
            if (unitOption.equals("GB") || unitOption.equals("MB")) {
                int memoryUnits = assertInt(ctx, ATTACK_MEMORY_UNITS);
                if (unitOption.equals("GB")) {
                    args.add("-g");
                    args.add(Integer.toString(memoryUnits));
                } else {
                    args.add("-m");
                    args.add(Integer.toString(memoryUnits));
                }
            } else {
                int memoryPercent = assertInt(ctx, ATTACK_MEMORY_PERCENT);
                args.add("-p");
                args.add(Integer.toString(memoryPercent));
            }
        } else {
            throw new IllegalArgumentException("Invalid Protocol. Allowed values are only GB, MB, PERCENT");
        }

        Map<String, Object> objAttack = Collections.singletonMap("type", "memory");
        Map<String, Object> objArgs = Collections.singletonMap("args", args);

        String attackGuid = createAttack(ctx, objAttack, objArgs, targetType, apiUrl, endPointType);
        String attackDetails = getAttackDetails(ctx, apiUrl, attackGuid);
        ctx.setVariable(ATTACK_DETAILS, attackDetails);
        log.info("URL of Gremlin Attack report: " + appUrl + ctx.getVariable(ATTACK_GUID));
    }

    public void disk(Context ctx, String apiUrl, String appUrl) {
        int length = assertInt(ctx, ATTACK_LENGTH);
        String dir = assertString(ctx, ATTACK_DIR);
        int percent = assertInt(ctx, ATTACK_DISK_PERCENT);
        int workers = getInt(ctx, ATTACK_WORKERS, 1);
        int blockSize = getInt(ctx, ATTACK_BLOCK_SIZE, 5);
        String targetType = getString(ctx, ATTACK_TARGET_TYPE, Constants.GREMLIN_DEFAULT_TARGET_TYPE);
        String endPointType = getString(ctx, ATTACK_ENDPOINT_TYPE, Constants.GREMLIN_DEFAULT_ENDPOINT_TYPE);

        List<String> args = new ArrayList<>(Arrays.asList("-d", dir, "--length", Integer.toString(length),
                "-p", Integer.toString(percent), "-w", Integer.toString(workers), "-b", Integer.toString(blockSize)));

        Map<String, Object> objAttack = Collections.singletonMap("type", "disk");
        Map<String, Object> objArgs = Collections.singletonMap("args", args);

        String attackGuid = createAttack(ctx, objAttack, objArgs, targetType, apiUrl, endPointType);
        String attackDetails = getAttackDetails(ctx, apiUrl, attackGuid);
        ctx.setVariable(ATTACK_DETAILS, attackDetails);
        log.info("URL of Gremlin Attack report: " + appUrl + ctx.getVariable(ATTACK_GUID));
    }

    public void io(Context ctx, String apiUrl, String appUrl) {
        int length = assertInt(ctx, ATTACK_LENGTH);
        String dir = assertString(ctx, ATTACK_DIR);
        String mode = assertString(ctx, ATTACK_IO_MODE);
        int workers = getInt(ctx, ATTACK_WORKERS, 1);
        int blockSize = getInt(ctx, ATTACK_BLOCK_SIZE, 5);
        int blockCount = getInt(ctx, ATTACK_IO_BLOCK_COUNT, 5);
        String targetType = getString(ctx, ATTACK_TARGET_TYPE, Constants.GREMLIN_DEFAULT_TARGET_TYPE);
        String endPointType = getString(ctx, ATTACK_ENDPOINT_TYPE, Constants.GREMLIN_DEFAULT_ENDPOINT_TYPE);

        List<String> args = new ArrayList<>(Arrays.asList("-d", dir, "-l", Integer.toString(length),
                "-m", mode, "-w", Integer.toString(workers), "-s", Integer.toString(blockSize), "-c", Integer.toString(blockCount)));

        Map<String, Object> objAttack = Collections.singletonMap("type", "io");
        Map<String, Object> objArgs = Collections.singletonMap("args", args);

        String attackGuid = createAttack(ctx, objAttack, objArgs, targetType, apiUrl, endPointType);
        String attackDetails = getAttackDetails(ctx, apiUrl, attackGuid);
        ctx.setVariable(ATTACK_DETAILS, attackDetails);
        log.info("URL of Gremlin Attack report: " + appUrl + ctx.getVariable(ATTACK_GUID));
    }
}
