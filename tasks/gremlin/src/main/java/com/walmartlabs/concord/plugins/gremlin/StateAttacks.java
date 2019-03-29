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

import static com.walmartlabs.concord.plugins.gremlin.Utils.creatAttack;
import static com.walmartlabs.concord.plugins.gremlin.Utils.getAttackDetails;
import static com.walmartlabs.concord.sdk.ContextUtils.*;

/**
 * Created by ppendha on 3/23/19.
 */
public class StateAttacks {

    private static final Logger log = LoggerFactory.getLogger(GremlinTask.class);

    private static final String ATTACK_SHUTDOWN_DELAY = "delay";
    private static final String ATTACK_SHUTDOWN_REBOOT = "reboot";
    private static final String ATTACK_TIME_TRAVEL_OFFSET = "offset";
    private static final String ATTACK_TIME_TRAVEL_NTP = "ntp";
    private static final String ATTACK_PROCESS_KILLER_INTERVAL = "interval";
    private static final String ATTACK_PROCESS_KILLER_PROCESS = "process";
    private static final String ATTACK_PROCESS_KILLER_GROUP = "group";
    private static final String ATTACK_PROCESS_KILLER_USER = "user";
    private static final String ATTACK_PROCESS_KILLER_NEWEST = "newest";
    private static final String ATTACK_PROCESS_KILLER_OLDEST = "oldest";
    private static final String ATTACK_PROCESS_KILLER_EXACT = "exact";
    private static final String ATTACK_PROCESS_KILLER_KILLCHILDREN = "killChildren";
    private static final String ATTACK_PROCESS_KILLER_FULLMATCH = "fullMatch";
    private static final String ATTACK_TARGET_TYPE = "targetType";
    private static final String ATTACK_LENGTH = "length";
    private static final String ATTACK_GUID = "attackGuid";
    private static final String ATTACK_DETAILS = "attackDetails";

    public void shutdown(Context ctx, String apiUrl, String appUrl) {
        int delay = getInt(ctx, ATTACK_SHUTDOWN_DELAY, 1);
        boolean reboot = getBoolean(ctx, ATTACK_SHUTDOWN_REBOOT, true);
        String targetType = getString(ctx, ATTACK_TARGET_TYPE, "Exact");

        List<String> args;
        if (reboot) {
            args = new ArrayList<>(Arrays.asList("-d", Integer.toString(delay), "--reboot"));
        } else {
            args = new ArrayList<>(Arrays.asList("-d", Integer.toString(delay)));
        }

        Map<String, Object> objAttack = Collections.singletonMap("type", "shutdown");
        Map<String, Object> objArgs = Collections.singletonMap("args", args);

        String attackGuid = creatAttack(ctx, objAttack, objArgs, targetType, apiUrl);
        String attackDetails = getAttackDetails(ctx, apiUrl, attackGuid);
        ctx.setVariable(ATTACK_DETAILS, attackDetails);
        log.info("URL of Gremlin Attack report: " + appUrl + ctx.getVariable(ATTACK_GUID));
    }

    public void timeTravel(Context ctx, String apiUrl, String appUrl) {
        int length = assertInt(ctx, ATTACK_LENGTH);
        int offset = getInt(ctx, ATTACK_TIME_TRAVEL_OFFSET, 5);
        boolean ntp = getBoolean(ctx, ATTACK_TIME_TRAVEL_NTP, false);
        String targetType = getString(ctx, ATTACK_TARGET_TYPE, "Exact");

        List<String> args;
        if (ntp) {
            args = new ArrayList<>(Arrays.asList("-l", Integer.toString(length), "-o", Integer.toString(offset), "--ntp"));
        } else {
            args = new ArrayList<>(Arrays.asList("-l", Integer.toString(length), "-o", Integer.toString(offset)));
        }

        Map<String, Object> objAttack = Collections.singletonMap("type", "time_travel");
        Map<String, Object> objArgs = Collections.singletonMap("args", args);

        String attackGuid = creatAttack(ctx, objAttack, objArgs, targetType, apiUrl);
        String attackDetails = getAttackDetails(ctx, apiUrl, attackGuid);
        ctx.setVariable(ATTACK_DETAILS, attackDetails);
        log.info("URL of Gremlin Attack report: " + appUrl + ctx.getVariable(ATTACK_GUID));
    }

    public void processKiller(Context ctx, String apiUrl, String appUrl) {
        int length = assertInt(ctx, ATTACK_LENGTH);
        int interval = getInt(ctx, ATTACK_PROCESS_KILLER_INTERVAL, 5);
        String process = assertString(ctx, ATTACK_PROCESS_KILLER_PROCESS);
        String group = getString(ctx, ATTACK_PROCESS_KILLER_GROUP, null);
        String user = getString(ctx, ATTACK_PROCESS_KILLER_USER, null);
        boolean newest = getBoolean(ctx, ATTACK_PROCESS_KILLER_NEWEST, false);
        boolean oldest = getBoolean(ctx, ATTACK_PROCESS_KILLER_OLDEST, false);
        boolean exact = getBoolean(ctx, ATTACK_PROCESS_KILLER_EXACT, false);
        boolean killChildren = getBoolean(ctx, ATTACK_PROCESS_KILLER_KILLCHILDREN, false);
        boolean fullMatch = getBoolean(ctx, ATTACK_PROCESS_KILLER_FULLMATCH, false);
        String targetType = getString(ctx, ATTACK_TARGET_TYPE, "Exact");

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

        String attackGuid = creatAttack(ctx, objAttack, objArgs, targetType, apiUrl);
        String attackDetails = getAttackDetails(ctx, apiUrl, attackGuid);
        ctx.setVariable(ATTACK_DETAILS, attackDetails);
        log.info("URL of Gremlin Attack report: " + appUrl + ctx.getVariable(ATTACK_GUID));
    }
}
