package com.walmartlabs.concord.plugins.gremlin;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2020 Walmart Inc., Concord Authors
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

import com.walmartlabs.concord.runtime.v2.sdk.MapBackedVariables;
import com.walmartlabs.concord.runtime.v2.sdk.Variables;

import java.util.*;
import java.util.stream.Collectors;

public class TaskParams implements GremlinClientParams {

    public static TaskParams of(Variables input, Map<String, Object> defaults) {
        Variables variables = merge(input, defaults);

        Action action = new TaskParams(variables).action();
        switch (action) {
            case CPU: {
                return new CpuParams(variables);
            }
            case MEMORY: {
                return new MemoryParams(variables);
            }
            case DISK: {
                return new DiskParams(variables);
            }
            case IO: {
                return new IOParams(variables);
            }
            case SHUTDOWN: {
                return new ShutdownParams(variables);
            }
            case TIMETRAVEL: {
                return new TimeTravelParams(variables);
            }
            case PROCESSKILLER: {
                return new ProcessKiller(variables);
            }
            case BLACKHOLE: {
                return new BlackHoleParams(variables);
            }
            case DNS: {
                return new DnsParams(variables);
            }
            case LATENCY: {
                return new LatencyParams(variables);
            }
            case PACKETLOSS: {
                return new PacketLossParams(variables);
            }
            case HALT: {
                return new HaltParams(variables);
            }
            default:
                throw new IllegalArgumentException("Unsupported action type: " + action);
        }
    }

    private static final String ACTION_KEY = "action";
    private static final String GREMLIN_API_KEY = "apiKey";
    private static final String TEAM_ID_KEY = "teamId";
    private static final String DEBUG_KEY = "debug";

    protected final Variables variables;

    public TaskParams(Variables variables) {
        this.variables = variables;
    }

    public Action action() {
        return assertEnum(variables, ACTION_KEY, Action.class);
    }

    @Override
    public String apiUrl() {
        return variables.assertString("apiUrl");
    }

    @Override
    public String apiKey() {
        return variables.assertString(GREMLIN_API_KEY);
    }

    @Override
    public String teamId() {
        return variables.getString(TEAM_ID_KEY);
    }

    @Override
    public boolean debug() {
        return variables.getBoolean(DEBUG_KEY, false);
    }

    @Override
    public long connectTimeout() {
        return variables.assertNumber("connectTimeout").longValue();
    }

    @Override
    public long readTimeout() {
        return variables.assertNumber("readTimeout").longValue();
    }

    @Override
    public long writeTimeout() {
        return variables.assertNumber("writeTimeout").longValue();
    }

    @Override
    public boolean useProxy() {
        return variables.getBoolean("useProxy", false);
    }

    @Override
    public String proxyHost() {
        return variables.assertString("proxyHost");
    }

    @Override
    public int proxyPort() {
        return variables.assertNumber("proxyPort").intValue();
    }

    public String appUrl() {
        return variables.assertString("appUrl");
    }

    public static class KubernetesParams {

        public static final String CLUSTER_KEY = "cluster";
        public static final String NAMESPACE_KEY = "namespace";
        public static final String DEPLOYMENTS_KEY = "deployments";
        public static final String DAEMON_SETS_KEY = "daemonSets";
        public static final String STATEFUL_SETS_KEY = "statefulSets";
        public static final String PODS_KEY = "pods";

        public enum SelectionType {
            /**
             * Gremlin will target all containers within each pod at runtime
             */
            ALL,

            /**
             * Gremlin will target a single container within each pod at runtime
             */
            ANY
        }

        private final Variables variables;

        public KubernetesParams(Variables variables) {
            this.variables = variables;
        }

        public String cluster() {
            return variables.assertString(CLUSTER_KEY);
        }

        public String namespace() {
            return variables.getString(NAMESPACE_KEY);
        }

        public List<String> deployments() {
            return variables.getList(DEPLOYMENTS_KEY, Collections.emptyList());
        }

        public List<String> daemonSets() {
            return variables.getList(DAEMON_SETS_KEY, Collections.emptyList());
        }

        public List<String> statefulSets() {
            return variables.getList(STATEFUL_SETS_KEY, Collections.emptyList());
        }

        public List<String> pods() {
            return variables.getList(PODS_KEY, Collections.emptyList());
        }

        public SelectionType selectionType() {
            return getEnum(variables, AttackParams.ATTACK_TARGET_TYPE, SelectionType.class, SelectionType.ALL);
        }
    }

    public static class AttackParams extends TaskParams {

        public enum EndpointType {
            HOSTS,
            CONTAINERS,
            KUBERNETES
        }

        private static final String ATTACK_TARGET_TYPE = "targetType";
        private static final String ATTACK_ENDPOINT_TYPE = "endPointType";
        private static final String ATTACK_TARGET_LIST = "targetList";
        private static final String ATTACK_TARGET_TAGS = "targetTags";
        private static final String ATTACK_TARGET_CONTAINER_IDS = "containerIds";
        private static final String ATTACK_TARGET_CONTAINER_LABELS = "containerLabels";
        private static final String ATTACK_TARGET_CONTAINER_COUNT = "containerCount";
        private static final String ATTACK_TARGET_CONTAINER_PERCENT = "containerPercent";
        private static final String TEAM_ID_KEY = "teamId";

        public AttackParams(Variables variables) {
            super(variables);
        }

        public String targetType() {
            return variables.getString(ATTACK_TARGET_TYPE, Constants.GREMLIN_DEFAULT_TARGET_TYPE);
        }

        public EndpointType endPointType() {
            return getEnum(variables, ATTACK_ENDPOINT_TYPE, EndpointType.class, EndpointType.HOSTS);
        }

        public Map<String, String> targetTags() {
            return variables.assertMap(ATTACK_TARGET_TAGS);
        }

        public List<Object> targetList() {
            return variables.assertList(ATTACK_TARGET_LIST);
        }

        public List<Object> containerIds() {
            return variables.assertList(ATTACK_TARGET_CONTAINER_IDS);
        }

        public Integer containerCount(Integer defaultValue) {
            Number result = variables.getNumber(ATTACK_TARGET_CONTAINER_COUNT, defaultValue);
            if (result == null) {
                return null;
            }
            return result.intValue();
        }

        public Integer containerPercent() {
            Number result = variables.getNumber(ATTACK_TARGET_CONTAINER_PERCENT, null);
            if (result == null) {
                return null;
            }
            return result.intValue();
        }

        public Map<String, String> containerLabels() {
            return variables.assertMap(ATTACK_TARGET_CONTAINER_LABELS);
        }

        public KubernetesParams k8s() {
            return new KubernetesParams(new MapBackedVariables(variables.getMap("k8s", Collections.emptyMap())));
        }

        public String teamId() {
            return variables.getString(TEAM_ID_KEY);
        }
    }

    public static class CpuParams extends AttackParams {

        private static final String ATTACK_CPU_CORES = "cores";
        private static final String ATTACK_LENGTH = "length";

        public CpuParams(Variables variables) {
            super(variables);
        }

        public int cores() {
            return variables.assertInt(ATTACK_CPU_CORES);
        }

        public int length() {
            return variables.assertInt(ATTACK_LENGTH);
        }
    }

    public static class MemoryParams extends AttackParams {

        private static final String ATTACK_MEMORY_UNIT_OPTIONS = "unitOption";
        private static final String ATTACK_LENGTH = "length";
        private static final String ATTACK_MEMORY_UNITS = "memoryUnits";
        private static final String ATTACK_MEMORY_PERCENT = "memoryPercent";

        public MemoryParams(Variables variables) {
            super(variables);
        }

        public String unitOption() {
            return variables.assertString(ATTACK_MEMORY_UNIT_OPTIONS);
        }

        public int length() {
            return variables.assertInt(ATTACK_LENGTH);
        }

        public int memoryUnits() {
            return variables.assertInt(ATTACK_MEMORY_UNITS);
        }

        public int memoryPercent() {
            return variables.assertInt(ATTACK_MEMORY_PERCENT);
        }
    }

    public static class DiskParams extends AttackParams {

        private static final String ATTACK_LENGTH = "length";
        private static final String ATTACK_DIR = "dir";
        private static final String ATTACK_DISK_PERCENT = "percent";
        private static final String ATTACK_WORKERS = "workers";
        private static final String ATTACK_BLOCK_SIZE = "blockSize";

        public DiskParams(Variables variables) {
            super(variables);
        }

        public int length() {
            return variables.assertInt(ATTACK_LENGTH);
        }

        public String dir() {
            return variables.assertString(ATTACK_DIR);
        }

        public int percent() {
            return variables.assertInt(ATTACK_DISK_PERCENT);
        }

        public int workers(int defaultValue) {
            return variables.getInt(ATTACK_WORKERS, defaultValue);
        }

        public int blockSize(int defaultValue) {
            return variables.getInt(ATTACK_BLOCK_SIZE, defaultValue);
        }
    }

    public static class IOParams extends AttackParams {

        private static final String ATTACK_LENGTH = "length";
        private static final String ATTACK_DIR = "dir";
        private static final String ATTACK_WORKERS = "workers";
        private static final String ATTACK_BLOCK_SIZE = "blockSize";
        private static final String ATTACK_IO_MODE = "mode";
        private static final String ATTACK_IO_BLOCK_COUNT = "blockCount";

        public IOParams(Variables variables) {
            super(variables);
        }

        public int length() {
            return variables.assertInt(ATTACK_LENGTH);
        }

        public String dir() {
            return variables.assertString(ATTACK_DIR);
        }

        public int workers(int defaultValue) {
            return variables.getInt(ATTACK_WORKERS, defaultValue);
        }

        public int blockSize(int defaultValue) {
            return variables.getInt(ATTACK_BLOCK_SIZE, defaultValue);
        }

        public String mode() {
            return variables.assertString(ATTACK_IO_MODE);
        }

        public int blockCount(int defaultValue) {
            return variables.getInt(ATTACK_IO_BLOCK_COUNT, defaultValue);
        }
    }

    public static class ShutdownParams extends AttackParams {

        private static final String ATTACK_SHUTDOWN_DELAY = "delay";
        private static final String ATTACK_SHUTDOWN_REBOOT = "reboot";

        public ShutdownParams(Variables variables) {
            super(variables);
        }

        public int delay(int defaultValue) {
            return variables.getInt(ATTACK_SHUTDOWN_DELAY, defaultValue);
        }

        public boolean reboot() {
            return variables.getBoolean(ATTACK_SHUTDOWN_REBOOT, true);
        }
    }

    public static class TimeTravelParams extends AttackParams {

        private static final String ATTACK_LENGTH = "length";
        private static final String ATTACK_TIME_TRAVEL_OFFSET = "offset";
        private static final String ATTACK_TIME_TRAVEL_NTP = "ntp";

        public TimeTravelParams(Variables variables) {
            super(variables);
        }

        public int length() {
            return variables.assertInt(ATTACK_LENGTH);
        }

        public int offset(int defaultValue) {
            return variables.getInt(ATTACK_TIME_TRAVEL_OFFSET, defaultValue);
        }

        public boolean ntp() {
            return variables.getBoolean(ATTACK_TIME_TRAVEL_NTP, false);
        }
    }

    public static class ProcessKiller extends AttackParams {

        private static final String ATTACK_LENGTH = "length";
        private static final String ATTACK_PROCESS_KILLER_INTERVAL = "interval";
        private static final String ATTACK_PROCESS_KILLER_PROCESS = "process";
        private static final String ATTACK_PROCESS_KILLER_GROUP = "group";
        private static final String ATTACK_PROCESS_KILLER_USER = "user";
        private static final String ATTACK_PROCESS_KILLER_NEWEST = "newest";
        private static final String ATTACK_PROCESS_KILLER_OLDEST = "oldest";
        private static final String ATTACK_PROCESS_KILLER_EXACT = "exact";
        private static final String ATTACK_PROCESS_KILLER_KILLCHILDREN = "killChildren";
        private static final String ATTACK_PROCESS_KILLER_FULLMATCH = "fullMatch";

        public ProcessKiller(Variables variables) {
            super(variables);
        }

        public int length() {
            return variables.assertInt(ATTACK_LENGTH);
        }

        public int interval(int defaultValue) {
            return variables.getInt(ATTACK_PROCESS_KILLER_INTERVAL, defaultValue);
        }

        public String process() {
            return variables.assertString(ATTACK_PROCESS_KILLER_PROCESS);
        }

        public String group() {
            return variables.getString(ATTACK_PROCESS_KILLER_GROUP);
        }

        public String user() {
            return variables.getString(ATTACK_PROCESS_KILLER_USER);
        }

        public boolean newest() {
            return variables.getBoolean(ATTACK_PROCESS_KILLER_NEWEST, false);
        }

        public boolean oldest() {
            return variables.getBoolean(ATTACK_PROCESS_KILLER_OLDEST, false);
        }

        public boolean exact() {
            return variables.getBoolean(ATTACK_PROCESS_KILLER_EXACT, false);
        }

        public boolean killChildren() {
            return variables.getBoolean(ATTACK_PROCESS_KILLER_KILLCHILDREN, false);
        }

        public boolean fullMatch() {
            return variables.getBoolean(ATTACK_PROCESS_KILLER_FULLMATCH, false);
        }
    }

    public static class BlackHoleParams extends AttackParams {

        private static final String ATTACK_IP_ADDRESSES = "ipAddresses";
        private static final String ATTACK_DEVICE = "device";
        private static final String ATTACK_HOST_NAMES = "hostnames";
        private static final String ATTACK_EGRESS_PORTS = "egressPorts";
        private static final String ATTACK_INGRESS_PORTS = "ingressPorts";
        private static final String ATTACK_PROTOCOL = "protocol";
        private static final String ATTACK_LENGTH = "length";

        public BlackHoleParams(Variables variables) {
            super(variables);
        }

        public int length() {
            return variables.assertInt(ATTACK_LENGTH);
        }

        public String ipAddresses() {
            return variables.assertString(ATTACK_IP_ADDRESSES);
        }

        public String device() {
            return variables.getString(ATTACK_DEVICE);
        }

        public String hostNames() {
            return variables.getString(ATTACK_HOST_NAMES);
        }

        public String egressPorts() {
            return variables.getString(ATTACK_EGRESS_PORTS);
        }

        public String ingressPorts() {
            return variables.getString(ATTACK_INGRESS_PORTS);
        }

        public String protocol() {
            return variables.getString(ATTACK_PROTOCOL);
        }
    }

    public static class DnsParams extends AttackParams {

        private static final String ATTACK_IP_ADDRESSES = "ipAddresses";
        private static final String ATTACK_DEVICE = "device";
        private static final String ATTACK_PROTOCOL = "protocol";
        private static final String ATTACK_LENGTH = "length";

        public DnsParams(Variables variables) {
            super(variables);
        }

        public int length() {
            return variables.assertInt(ATTACK_LENGTH);
        }

        public String ipAddresses() {
            return variables.assertString(ATTACK_IP_ADDRESSES);
        }

        public String device() {
            return variables.getString(ATTACK_DEVICE);
        }

        public String protocol() {
            return variables.getString(ATTACK_PROTOCOL);
        }
    }

    public static class LatencyParams extends AttackParams {

        private static final String ATTACK_IP_ADDRESSES = "ipAddresses";
        private static final String ATTACK_DEVICE = "device";
        private static final String ATTACK_HOST_NAMES = "hostnames";
        private static final String ATTACK_EGRESS_PORTS = "egressPorts";
        private static final String ATTACK_PROTOCOL = "protocol";
        private static final String ATTACK_SOURCE_PORTS = "sourcePorts";
        private static final String ATTACK_LATENCY_DELAY = "delay";
        private static final String ATTACK_LENGTH = "length";

        public LatencyParams(Variables variables) {
            super(variables);
        }

        public int length() {
            return variables.assertInt(ATTACK_LENGTH);
        }

        public String ipAddresses() {
            return variables.assertString(ATTACK_IP_ADDRESSES);
        }

        public String device() {
            return variables.getString(ATTACK_DEVICE);
        }

        public String hostNames() {
            return variables.getString(ATTACK_HOST_NAMES);
        }

        public String egressPorts() {
            return variables.getString(ATTACK_EGRESS_PORTS);
        }

        public String protocol() {
            return variables.getString(ATTACK_PROTOCOL);
        }

        public String sourcePorts() {
            return variables.getString(ATTACK_SOURCE_PORTS);
        }

        public int delay(int defaultValue) {
            return variables.getInt(ATTACK_LATENCY_DELAY, defaultValue);
        }
    }

    public static class PacketLossParams extends AttackParams {

        private static final String ATTACK_IP_ADDRESSES = "ipAddresses";
        private static final String ATTACK_DEVICE = "device";
        private static final String ATTACK_HOST_NAMES = "hostnames";
        private static final String ATTACK_EGRESS_PORTS = "egressPorts";
        private static final String ATTACK_PROTOCOL = "protocol";
        private static final String ATTACK_SOURCE_PORTS = "sourcePorts";
        private static final String ATTACK_PACKET_LOSS_CORRUPT = "corrupt";
        private static final String ATTACK_PACKET_LOSS_PERCENT = "percent";
        private static final String ATTACK_LENGTH = "length";

        public PacketLossParams(Variables variables) {
            super(variables);
        }

        public int length() {
            return variables.assertInt(ATTACK_LENGTH);
        }

        public String ipAddresses() {
            return variables.assertString(ATTACK_IP_ADDRESSES);
        }

        public String device() {
            return variables.getString(ATTACK_DEVICE);
        }

        public String hostNames() {
            return variables.getString(ATTACK_HOST_NAMES);
        }

        public String egressPorts() {
            return variables.getString(ATTACK_EGRESS_PORTS);
        }

        public String protocol() {
            return variables.getString(ATTACK_PROTOCOL);
        }

        public String sourcePorts() {
            return variables.getString(ATTACK_SOURCE_PORTS);
        }

        public int percent(int defaultValue) {
            return variables.getInt(ATTACK_PACKET_LOSS_PERCENT, defaultValue);
        }

        public boolean corrupt() {
            return variables.getBoolean(ATTACK_PACKET_LOSS_CORRUPT, false);
        }
    }

    public static class HaltParams extends AttackParams {

        private static final String ATTACK_GUID = "attackGuid";

        public HaltParams(Variables variables) {
            super(variables);
        }

        public String attackGuid() {
            return variables.assertString(ATTACK_GUID);
        }
    }

    private static Variables merge(Variables variables, Map<String, Object> defaults) {
        Map<String, Object> variablesMap = new HashMap<>(defaults != null ? defaults : Collections.emptyMap());
        variablesMap.putAll(variables.toMap());
        return new MapBackedVariables(variablesMap);
    }

    public static <E extends Enum<E>> E assertEnum(Variables variables, String name, Class<E> enumData) {
        E result = getEnum(variables, name, enumData, null);
        if (result != null) {
            return result;
        }
        throw new IllegalArgumentException("Mandatory variable '" + name + "' is required");
    }

    public static <E extends Enum<E>> E getEnum(Variables variables, String name, Class<E> enumData, E defaultValue) {
        String v = variables.getString(name);
        if (v == null) {
            return defaultValue;
        }

        String normalizedValue = v.trim().toUpperCase();
        for (Enum<E> enumVal : enumData.getEnumConstants()) {
            if (enumVal.name().equals(normalizedValue)) {
                return Enum.valueOf(enumData, normalizedValue);
            }
        }
        throw new IllegalArgumentException("Invalid variable '" + name + "' type, expected one of: " +
                Arrays.stream(enumData.getEnumConstants()).map(Enum::name).collect(Collectors.toList()) +
                ", got: " + v);
    }

    public enum Action {
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
