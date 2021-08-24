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

public class NetworkAttacks {

    public Map<String, Object> blackhole(BlackHoleParams in) {
        int length = in.length();
        String ipAddresses = in.ipAddresses();
        String device = in.device();
        String hostNames = in.hostNames();
        String egressPorts = in.egressPorts();
        String ingressPorts = in.ingressPorts();
        String protocol = in.protocol();

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

        return Utils.performAttack(in, "blackhole", args);
    }

    public Map<String, Object> dns(DnsParams in) {
        int length = in.length();
        String ipAddresses = in.ipAddresses();
        String device = in.device();
        String protocol = in.protocol();

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

        return Utils.performAttack(in, "dns", args);
    }

    public Map<String, Object> latency(LatencyParams in) {
        int length = in.length();
        String ipAddresses = in.ipAddresses();
        String device = in.device();
        String hostNames = in.hostNames();
        String egressPorts = in.egressPorts();
        String sourcePorts = in.sourcePorts();
        int delay = in.delay(10);
        String protocol = in.protocol();

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

        return Utils.performAttack(in, "latency", args);
    }

    public Map<String, Object> packetLoss(PacketLossParams in) {
        int length = in.length();
        String ipAddresses = in.ipAddresses();
        String device = in.device();
        String hostNames = in.hostNames();
        String egressPorts = in.egressPorts();
        String sourcePorts = in.sourcePorts();
        int percent = in.percent(1);
        String protocol = in.protocol();
        boolean corrupt = in.corrupt();

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

        return Utils.performAttack(in, "packet_loss", args);
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
