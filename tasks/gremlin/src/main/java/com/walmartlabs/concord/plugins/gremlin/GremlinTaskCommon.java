package com.walmartlabs.concord.plugins.gremlin;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2018 Walmart Inc.
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

import java.util.Collections;
import java.util.Map;

import static com.walmartlabs.concord.plugins.gremlin.TaskParams.*;

public class GremlinTaskCommon {

    private static final Logger log = LoggerFactory.getLogger(GremlinTaskCommon.class);

    public Map<String, Object> execute(TaskParams in) {
        Action action = in.action();

        if (action == Action.HALT) {
            log.info("Starting '{}' action...", action);
        } else {
            log.info("Starting '{}' attack...", action);
        }

        log.info("Using Gremlin API URL: '{}'...", in.apiUrl());

        switch (action) {
            case CPU: {
                ResourceAttacks a = new ResourceAttacks();
                return a.cpu((CpuParams)in);
            }
            case MEMORY: {
                ResourceAttacks a = new ResourceAttacks();
                return a.memory((MemoryParams)in);
            }
            case DISK: {
                ResourceAttacks a = new ResourceAttacks();
                return a.disk((DiskParams)in);
            }
            case IO: {
                ResourceAttacks a = new ResourceAttacks();
                return a.io((IOParams)in);
            }
            case SHUTDOWN: {
                StateAttacks b = new StateAttacks();
                return b.shutdown((ShutdownParams)in);
            }
            case TIMETRAVEL: {
                StateAttacks b = new StateAttacks();
                return b.timeTravel((TimeTravelParams)in);
            }
            case PROCESSKILLER: {
                StateAttacks b = new StateAttacks();
                return b.processKiller((ProcessKiller)in);
            }
            case BLACKHOLE: {
                NetworkAttacks c = new NetworkAttacks();
                return c.blackhole((BlackHoleParams)in);
            }
            case DNS: {
                NetworkAttacks c = new NetworkAttacks();
                return c.dns((DnsParams)in);
            }
            case LATENCY: {
                NetworkAttacks c = new NetworkAttacks();
                return c.latency((LatencyParams)in);
            }
            case PACKETLOSS: {
                NetworkAttacks c = new NetworkAttacks();
                return c.packetLoss((PacketLossParams)in);
            }
            case HALT: {
                Utils d = new Utils();
                d.halt((HaltParams)in);
                return Collections.emptyMap();
            }
            default:
                throw new IllegalArgumentException("Unsupported action type: " + action);
        }
    }
}
