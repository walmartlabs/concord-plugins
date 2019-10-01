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

public class NetworkAttacks {

    private static final Logger log = LoggerFactory.getLogger(GremlinTask.class);
    private static final String ATTACK_IP_ADDRESSES = "ipAddresses";
    private static final String ATTACK_DEVICE = "device";
    private static final String ATTACK_HOST_NAMES = "hostnames";
    private static final String ATTACK_EGRESS_PORTS = "egressPorts";
    private static final String ATTACK_INGRESS_PORTS = "ingressPorts";
    private static final String ATTACK_PROTOCOL = "protocol";
    private static final String ATTACK_SOURCE_PORTS = "sourcePorts";
    private static final String ATTACK_LATENCY_DELAY = "delay";
    private static final String ATTACK_PACKET_LOSS_CORRUPT = "corrupt";
    private static final String ATTACK_PACKET_LOSS_PERCENT = "percent";
    private static final String ATTACK_TARGET_TYPE = "targetType";
    private static final String ATTACK_LENGTH = "length";
    private static final String ATTACK_GUID = "attackGuid";
    private static final String ATTACK_DETAILS = "attackDetails";
    private static final String ATTACK_ENDPOINT_TYPE = "endPointType";

    public void blackhole(Context ctx, String apiUrl, String appUrl) {
        int length = assertInt(ctx, ATTACK_LENGTH);
        String ipAddresses = assertString(ctx, ATTACK_IP_ADDRESSES);
        String device = getString(ctx, ATTACK_DEVICE, null);
        String hostNames = getString(ctx, ATTACK_HOST_NAMES, null);
        String egressPorts = getString(ctx, ATTACK_EGRESS_PORTS, null);
        String ingressPorts = getString(ctx, ATTACK_INGRESS_PORTS, null);
        String protocol = getString(ctx, ATTACK_PROTOCOL, null);
        String targetType = getString(ctx, ATTACK_TARGET_TYPE, Constants.GREMLIN_DEFAULT_TARGET_TYPE);
        String endPointType = getString(ctx, ATTACK_ENDPOINT_TYPE, Constants.GREMLIN_DEFAULT_ENDPOINT_TYPE);

        List<String> args = new ArrayList<>(Arrays.asList("-l", Integer.toString(length), "-i", ipAddresses));

        configureProtocol(protocol, args);

        if (device != null && !device.isEmpty()) {
            args.add("-d");
            args.add(device);
        }

        if (hostNames != null && !hostNames.isEmpty()) {
            args.add("-h");
            args.add(hostNames);
        }

        if (egressPorts != null && !egressPorts.isEmpty()) {
            args.add("-p");
            args.add(egressPorts);
        }

        if (ingressPorts != null && !ingressPorts.isEmpty()) {
            args.add("-n");
            args.add(ingressPorts);
        }

        Map<String, Object> objAttack = Collections.singletonMap("type", "blackhole");
        Map<String, Object> objArgs = Collections.singletonMap("args", args);

        String attackGuid = createAttack(ctx, objAttack, objArgs, targetType, apiUrl, endPointType);

        String attackDetails = getAttackDetails(ctx, apiUrl, attackGuid);
        ctx.setVariable(ATTACK_DETAILS, attackDetails);
        log.info("URL of Gremlin Attack report: " + appUrl + ctx.getVariable(ATTACK_GUID));
    }

    public void dns(Context ctx, String apiUrl, String appUrl) {
        int length = assertInt(ctx, ATTACK_LENGTH);
        String ipAddresses = assertString(ctx, ATTACK_IP_ADDRESSES);
        String device = getString(ctx, ATTACK_DEVICE, null);
        String protocol = getString(ctx, ATTACK_PROTOCOL, null);
        String targetType = getString(ctx, ATTACK_TARGET_TYPE, Constants.GREMLIN_DEFAULT_TARGET_TYPE);
        String endPointType = getString(ctx, ATTACK_ENDPOINT_TYPE, Constants.GREMLIN_DEFAULT_ENDPOINT_TYPE);

        List<String> args = new ArrayList<>(Arrays.asList("-l", Integer.toString(length), "-i", ipAddresses));

        if (protocol != null && !protocol.isEmpty()) {
            List<String> validProtocol = Arrays.asList("TCP", "UDP");
            if (validProtocol.contains(protocol.toUpperCase())) {
                protocol = protocol.toUpperCase();
                args.add("-P");
                args.add(protocol);
            } else {
                throw new IllegalArgumentException("Invalid Protocol. Allowed values are only TCP, UDP");
            }
        }

        if (device != null && !device.isEmpty()) {
            args.add("-d");
            args.add(device);
        }

        Map<String, Object> objAttack = Collections.singletonMap("type", "dns");
        Map<String, Object> objArgs = Collections.singletonMap("args", args);

        String attackGuid = createAttack(ctx, objAttack, objArgs, targetType, apiUrl, endPointType);
        String attackDetails = getAttackDetails(ctx, apiUrl, attackGuid);
        ctx.setVariable(ATTACK_DETAILS, attackDetails);
        log.info("URL of Gremlin Attack report: " + appUrl + ctx.getVariable(ATTACK_GUID));
    }

    public void latency(Context ctx, String apiUrl, String appUrl) {
        int length = assertInt(ctx, ATTACK_LENGTH);
        String ipAddresses = assertString(ctx, ATTACK_IP_ADDRESSES);
        String device = getString(ctx, ATTACK_DEVICE, null);
        String hostNames = getString(ctx, ATTACK_HOST_NAMES, null);
        String egressPorts = getString(ctx, ATTACK_EGRESS_PORTS, null);
        String sourcePorts = getString(ctx, ATTACK_SOURCE_PORTS, null);
        int delay = getInt(ctx, ATTACK_LATENCY_DELAY, 10);
        String protocol = getString(ctx, ATTACK_PROTOCOL, null);
        String targetType = getString(ctx, ATTACK_TARGET_TYPE, Constants.GREMLIN_DEFAULT_TARGET_TYPE);
        String endPointType = getString(ctx, ATTACK_ENDPOINT_TYPE, Constants.GREMLIN_DEFAULT_ENDPOINT_TYPE);

        List<String> args = new ArrayList<>(Arrays.asList("-l", Integer.toString(length), "-i", ipAddresses, "-m", Integer.toString(delay)));
        configureProtocol(protocol, args);

        if (device != null && !device.isEmpty()) {
            args.add("-d");
            args.add(device);
        }

        if (hostNames != null && !hostNames.isEmpty()) {
            args.add("-h");
            args.add(hostNames);
        }

        if (egressPorts != null && !egressPorts.isEmpty()) {
            args.add("-p");
            args.add(egressPorts);
        }

        if (sourcePorts != null && !sourcePorts.isEmpty()) {
            args.add("-s");
            args.add(sourcePorts);
        }

        Map<String, Object> objAttack = Collections.singletonMap("type", "latency");
        Map<String, Object> objArgs = Collections.singletonMap("args", args);

        String attackGuid = createAttack(ctx, objAttack, objArgs, targetType, apiUrl, endPointType);
        String attackDetails = getAttackDetails(ctx, apiUrl, attackGuid);
        ctx.setVariable(ATTACK_DETAILS, attackDetails);
        log.info("URL of Gremlin Attack report: " + appUrl + ctx.getVariable(ATTACK_GUID));
    }

    public void packetLoss(Context ctx, String apiUrl, String appUrl) {
        int length = assertInt(ctx, ATTACK_LENGTH);
        String ipAddresses = assertString(ctx, ATTACK_IP_ADDRESSES);
        String device = getString(ctx, ATTACK_DEVICE, null);
        String hostNames = getString(ctx, ATTACK_HOST_NAMES, null);
        String egressPorts = getString(ctx, ATTACK_EGRESS_PORTS, null);
        String sourcePorts = getString(ctx, ATTACK_SOURCE_PORTS, null);
        int percent = getInt(ctx, ATTACK_PACKET_LOSS_PERCENT, 1);
        String protocol = getString(ctx, ATTACK_PROTOCOL, null);
        boolean corrupt = getBoolean(ctx, ATTACK_PACKET_LOSS_CORRUPT, false);
        String targetType = getString(ctx, ATTACK_TARGET_TYPE, Constants.GREMLIN_DEFAULT_TARGET_TYPE);
        String endPointType = getString(ctx, ATTACK_ENDPOINT_TYPE, Constants.GREMLIN_DEFAULT_ENDPOINT_TYPE);

        List<String> args = new ArrayList<>(Arrays.asList("-l", Integer.toString(length), "-i", ipAddresses, "-r", Integer.toString(percent)));
        configureProtocol(protocol, args);

        if (device != null && !device.isEmpty()) {
            args.add("-d");
            args.add(device);
        }

        if (hostNames != null && !hostNames.isEmpty()) {
            args.add("-h");
            args.add(hostNames);
        }

        if (egressPorts != null && !egressPorts.isEmpty()) {
            args.add("-p");
            args.add(egressPorts);
        }

        if (sourcePorts != null && !sourcePorts.isEmpty()) {
            args.add("-s");
            args.add(sourcePorts);
        }

        if (corrupt) {
            args.add("--corrupt");
        }

        Map<String, Object> objAttack = Collections.singletonMap("type", "packet_loss");
        Map<String, Object> objArgs = Collections.singletonMap("args", args);

        String attackGuid = createAttack(ctx, objAttack, objArgs, targetType, apiUrl, endPointType);
        String attackDetails = getAttackDetails(ctx, apiUrl, attackGuid);
        ctx.setVariable(ATTACK_DETAILS, attackDetails);
        log.info("URL of Gremlin Attack report: " + appUrl + ctx.getVariable(ATTACK_GUID));
    }

    private void configureProtocol(String protocol, List<String> args) {
        if (protocol != null && !protocol.isEmpty()) {
            List<String> validProtocol = Constants.GREMLIN_VALID_PROTOCOLS;
            if (validProtocol.contains(protocol.toUpperCase())) {
                protocol = protocol.toUpperCase();
                args.add("-P");
                args.add(protocol);
            } else {
                throw new IllegalArgumentException("Invalid Protocol. Allowed values are only TCP, UDP, ICMP");
            }
        }
    }
}
