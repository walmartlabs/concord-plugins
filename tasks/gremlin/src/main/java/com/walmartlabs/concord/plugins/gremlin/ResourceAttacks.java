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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.walmartlabs.concord.plugins.gremlin.TaskParams.*;
import static com.walmartlabs.concord.plugins.gremlin.Utils.createAttack;
import static com.walmartlabs.concord.plugins.gremlin.Utils.getAttackDetails;

public class ResourceAttacks {

    private static final Logger log = LoggerFactory.getLogger(GremlinTask.class);
    private static final String ATTACK_GUID = "attackGuid";
    private static final String ATTACK_DETAILS = "attackDetails";

    public Map<String, Object> cpu(CpuParams in) {
        int cores = in.cores();
        int length = in.length();

        List<String> args = new ArrayList<>(Arrays.asList("-l", Integer.toString(length), "-c", Integer.toString(cores)));

        Map<String, Object> objAttack = Collections.singletonMap("type", "cpu");
        Map<String, Object> objArgs = Collections.singletonMap("args", args);

        return processAttack(in, objAttack, objArgs);
    }

    public Map<String, Object> memory(MemoryParams in) {
        String unitOption = in.unitOption();
        int length = in.length();

        List<String> args = new ArrayList<>(Arrays.asList("-l", Integer.toString(length)));
        List<String> validUnitOption = Constants.GREMLIN_VALID_UNIT_OPTION;

        if (validUnitOption.contains(unitOption.toUpperCase())) {
            unitOption = unitOption.toUpperCase();
            if (unitOption.equals("GB") || unitOption.equals("MB")) {
                int memoryUnits = in.memoryUnits();
                if (unitOption.equals("GB")) {
                    args.add("-g");
                    args.add(Integer.toString(memoryUnits));
                } else {
                    args.add("-m");
                    args.add(Integer.toString(memoryUnits));
                }
            } else {
                int memoryPercent = in.memoryPercent();
                args.add("-p");
                args.add(Integer.toString(memoryPercent));
            }
        } else {
            throw new IllegalArgumentException("Invalid Protocol. Allowed values are only GB, MB, PERCENT");
        }

        Map<String, Object> objAttack = Collections.singletonMap("type", "memory");
        Map<String, Object> objArgs = Collections.singletonMap("args", args);

        return processAttack(in, objAttack, objArgs);
    }

    public Map<String, Object> disk(DiskParams in) {
        int length = in.length();
        String dir = in.dir();
        int percent = in.percent();
        int workers = in.workers(1);
        int blockSize = in.blockSize(5);

        List<String> args = new ArrayList<>(Arrays.asList("-d", dir, "--length", Integer.toString(length),
                "-p", Integer.toString(percent), "-w", Integer.toString(workers), "-b", Integer.toString(blockSize)));

        Map<String, Object> objAttack = Collections.singletonMap("type", "disk");
        Map<String, Object> objArgs = Collections.singletonMap("args", args);

        return processAttack(in, objAttack, objArgs);
    }

    public Map<String, Object> io(IOParams in) {
        int length = in.length();
        String dir = in.dir();
        String mode = in.mode();
        int workers = in.workers(1);
        int blockSize = in.blockSize(5);
        int blockCount = in.blockCount(5);

        List<String> args = new ArrayList<>(Arrays.asList("-d", dir, "-l", Integer.toString(length),
                "-m", mode, "-w", Integer.toString(workers), "-s", Integer.toString(blockSize), "-c", Integer.toString(blockCount)));

        Map<String, Object> objAttack = Collections.singletonMap("type", "io");
        Map<String, Object> objArgs = Collections.singletonMap("args", args);

        return processAttack(in, objAttack, objArgs);
    }

    private Map<String, Object> processAttack(AttackParams in, Map<String, Object> objAttack, Map<String, Object> objArgs) {
        String attackGuid = createAttack(in, objAttack, objArgs);
        String attackDetails = getAttackDetails(in, attackGuid);
        log.info("URL of Gremlin Attack report: {}", in.appUrl() + attackGuid);

        Map<String, Object> result = new HashMap<>();
        result.put(ATTACK_DETAILS, attackDetails);
        result.put(ATTACK_GUID, attackGuid);
        return result;
    }
}
