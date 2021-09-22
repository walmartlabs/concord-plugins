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

import java.util.*;

import static com.walmartlabs.concord.plugins.gremlin.TaskParams.*;

public class ResourceAttacks {

    public Map<String, Object> cpu(CpuParams in) {
        int cores = in.cores();
        int length = in.length();

        List<String> args = new ArrayList<>(Arrays.asList("-l", Integer.toString(length), "-c", Integer.toString(cores)));
        return Utils.performAttack(in, "cpu", args);
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

        return Utils.performAttack(in, "memory", args);
    }

    public Map<String, Object> disk(DiskParams in) {
        int length = in.length();
        String dir = in.dir();
        int percent = in.percent();
        int workers = in.workers(1);
        int blockSize = in.blockSize(5);

        List<String> args = new ArrayList<>(Arrays.asList("-d", dir, "--length", Integer.toString(length),
                "-p", Integer.toString(percent), "-w", Integer.toString(workers), "-b", Integer.toString(blockSize)));

        return Utils.performAttack(in, "disk", args);
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

        return Utils.performAttack(in, "io", args);
    }
}
