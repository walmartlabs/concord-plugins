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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.walmartlabs.concord.plugins.gremlin.TaskParams.*;

public class StateAttacks {

    public Map<String, Object> shutdown(ShutdownParams in) {
        int delay = in.delay(1);
        boolean reboot = in.reboot();

        List<String> args;
        if (reboot) {
            args = new ArrayList<>(Arrays.asList("-d", Integer.toString(delay), "--reboot"));
        } else {
            args = new ArrayList<>(Arrays.asList("-d", Integer.toString(delay)));
        }

        return Utils.performAttack(in, "shutdown", args);
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

        return Utils.performAttack(in, "time_travel", args);
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

        return Utils.performAttack(in, "process_killer", args);
    }
}
