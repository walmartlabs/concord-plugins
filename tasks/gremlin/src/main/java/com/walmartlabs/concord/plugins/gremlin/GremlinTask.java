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

import com.walmartlabs.concord.sdk.Context;
import com.walmartlabs.concord.sdk.InjectVariable;
import com.walmartlabs.concord.sdk.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.walmartlabs.concord.sdk.ContextUtils.assertString;

/**
 * Created by ppendha on 3/23/19.
 */
@Named("gremlin")
public class GremlinTask implements Task {

    private static final Logger log = LoggerFactory.getLogger(GremlinTask.class);

    private static final String ACTION_KEY = "action";
    private static final String ATTACK_GUID = "attackGuid";

    @InjectVariable("gremlinParams")
    private static Map<String, Object> defaults;

    @Override
    public void execute(Context ctx) {

        Map<String, Object> cfg = createCfg(ctx);
        String apiUrl = cfg.get("apiUrl").toString();
        String appUrl = cfg.get("appUrl").toString();
        Action action = getAction(ctx);

        if (action.toString().equals("HALT")) {
            log.info("Starting '{}' action...", action);
        } else {
            log.info("Starting '{}' attack...", action);
        }

        log.info("Using Gremlin API URL: '{}'...", apiUrl);

        switch (action) {
            case CPU: {
                ResourceAttacks a = new ResourceAttacks();
                a.cpu(ctx, apiUrl, appUrl);
                break;
            }
            case MEMORY: {
                ResourceAttacks a = new ResourceAttacks();
                a.memory(ctx, apiUrl, appUrl);
                break;
            }
            case DISK: {
                ResourceAttacks a = new ResourceAttacks();
                a.disk(ctx, apiUrl, appUrl);
                break;
            }
            case IO: {
                ResourceAttacks a = new ResourceAttacks();
                a.io(ctx, apiUrl, appUrl);
                break;
            }
            case SHUTDOWN: {
                StateAttacks b = new StateAttacks();
                b.shutdown(ctx, apiUrl, appUrl);
                break;
            }
            case TIMETRAVEL: {
                StateAttacks b = new StateAttacks();
                b.timeTravel(ctx, apiUrl, appUrl);
                break;
            }
            case PROCESSKILLER: {
                StateAttacks b = new StateAttacks();
                b.processKiller(ctx, apiUrl, appUrl);
                break;
            }
            case BLACKHOLE: {
                NetworkAttacks c = new NetworkAttacks();
                c.blackhole(ctx, apiUrl, appUrl);
                break;
            }
            case DNS: {
                NetworkAttacks c = new NetworkAttacks();
                c.dns(ctx, apiUrl, appUrl);
                break;
            }
            case LATENCY: {
                NetworkAttacks c = new NetworkAttacks();
                c.latency(ctx, apiUrl, appUrl);
                break;
            }
            case PACKETLOSS: {
                NetworkAttacks c = new NetworkAttacks();
                c.packetLoss(ctx, apiUrl, appUrl);
                break;
            }
            case HALT: {
                String attackGuid = assertString(ctx, ATTACK_GUID);
                Utils d = new Utils();
                d.halt(ctx, apiUrl, attackGuid);
                break;
            }
            default:
                throw new IllegalArgumentException("Unsupported action type: " + action);
        }
    }

    public static Map<String, Object> createCfg(Context ctx) {
        Map<String, Object> m = new HashMap<>(defaults != null ? defaults : Collections.emptyMap());
        put(m, com.walmartlabs.concord.sdk.Constants.Context.WORK_DIR_KEY, ctx);
        return m;
    }

    private static Action getAction(Context ctx) {
        return Action.valueOf(assertString(ctx, ACTION_KEY).trim().toUpperCase());
    }

    private static void put(Map<String, Object> m, String k, Context ctx) {
        Object v = ctx.getVariable(k);
        if (v == null) {
            return;
        }
        m.put(k, v);
    }

    private enum Action {
        CPU,
        MEMORY,
        DISK,
        IO,
        SHUTDOWN,
        TIMETRAVEL,
        PROCESSKILLER,
        BLACKHOLE,
        DNS,
        LATENCY,
        PACKETLOSS,
        HALT
    }
}
