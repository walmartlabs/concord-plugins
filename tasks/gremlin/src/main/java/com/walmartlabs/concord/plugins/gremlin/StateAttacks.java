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

public class StateAttacks {

    private static final Logger log = LoggerFactory.getLogger(GremlinTask.class);

    private static final String ATTACK_GUID = "attackGuid";
    private static final String ATTACK_DETAILS = "attackDetails";

    public Map<String, Object> shutdown(ShutdownParams in) {
        int delay = in.delay(1);
        boolean reboot = in.reboot();

        List<String> args;
        if (reboot) {
            args = new ArrayList<>(Arrays.asList("-d", Integer.toString(delay), "--reboot"));
        } else {
            args = new ArrayList<>(Arrays.asList("-d", Integer.toString(delay)));
        }

        Map<String, Object> objAttack = Collections.singletonMap("type", "shutdown");
        Map<String, Object> objArgs = Collections.singletonMap("args", args);

        return processAttack(in, objAttack, objArgs);
    }

    public Map<String, Object> timeTravel(TimeTravelParams in) {
        int length = in.length();
        int offset = in.offset(5);
        boolean ntp = in.ntp();

        List<String> args;
        if (ntp) {
            args = new ArrayList<>(Arrays.asList("-l", Integer.toString(length), "-o", Integer.toString(offset), "--ntp"));
        } else {
            args = new ArrayList<>(Arrays.asList("-l", Integer.toString(length), "-o", Integer.toString(offset)));
        }

        Map<String, Object> objAttack = Collections.singletonMap("type", "time_travel");
        Map<String, Object> objArgs = Collections.singletonMap("args", args);

        return processAttack(in, objAttack, objArgs);
    }

    public Map<String, Object> processKiller(ProcessKiller in) {
        int length = in.length();
        int interval = in.interval(5);
        String process = in.process();
        String group = in.group();
        String user = in.user();
        boolean newest = in.newest();
        boolean oldest = in.oldest();
        boolean exact = in.exact();
        boolean killChildren = in.killChildren();
        boolean fullMatch = in.fullMatch();

        List<String> args = new ArrayList<>(Arrays.asList("-l", Integer.toString(length), "-i", Integer.toString(interval), "-p", process));

        if (group != null && !group.isEmpty()) {
            args.add("-g");
            args.add(group);
        }

        if (user != null && !user.isEmpty()) {
            args.add("-u");
            args.add(user);
        }

        if (newest) {
            args.add("--newest");
        }

        if (oldest) {
            args.add("--oldest");
        }

        if (exact) {
            args.add("--exact");
        }

        if (killChildren) {
            args.add("--kill_children");
        }

        if (fullMatch) {
            args.add("--full");
        }

        Map<String, Object> objAttack = Collections.singletonMap("type", "process_killer");
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
