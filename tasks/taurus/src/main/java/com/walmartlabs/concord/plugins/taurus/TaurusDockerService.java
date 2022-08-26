package com.walmartlabs.concord.plugins.taurus;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface TaurusDockerService {
    int start(DockerContainerSpec spec,
              DockerLogCallback outLog,
              DockerLogCallback errLog) throws Exception;

    class DockerContainerSpec {

        private String image;
        private List<String> args;
        private Map<String, String> env;
        private boolean debug;
        private boolean forcePull;
        private Collection<String> extraDockerHosts;
        private int pullRetryCount;
        private long pullRetryInterval;
        private Path pwd;

        public String image() {
            return image;
        }

        public DockerContainerSpec image(String image) {
            this.image = image;
            return this;
        }

        public List<String> args() {
            return args;
        }

        public DockerContainerSpec args(List<String> args) {
            this.args = args;
            return this;
        }

        public Map<String, String> env() {
            return this.env;
        }

        public DockerContainerSpec env(Map<String, String> env) {
            this.env = env;
            return this;
        }

        public boolean debug() {
            return this.debug;
        }

        public DockerContainerSpec debug(boolean debug) {
            this.debug = debug;
            return this;
        }

        public boolean forcePull() {
            return this.forcePull;
        }

        public DockerContainerSpec forcePull(boolean forcePull) {
            this.forcePull = forcePull;
            return this;
        }

        public Collection<String> extraDockerHosts() {
            return this.extraDockerHosts;
        }

        public DockerContainerSpec extraDockerHosts(Collection<String> extraDockerHosts) {
            this.extraDockerHosts = extraDockerHosts;
            return this;
        }

        public int pullRetryCount() {
            return this.pullRetryCount;
        }

        public DockerContainerSpec pullRetryCount(int pullRetryCount) {
            this.pullRetryCount = pullRetryCount;
            return this;
        }

        public long pullRetryInterval() {
            return this.pullRetryInterval;
        }

        public DockerContainerSpec pullRetryInterval(long pullRetryInterval) {
            this.pullRetryInterval = pullRetryInterval;
            return this;
        }

        public Path pwd() {
            return this.pwd;
        }

        public DockerContainerSpec pwd(Path pwd) {
            this.pwd = pwd;
            return this;
        }
    }

    class DockerLogCallback {
        final StringBuilder sb;
        final String logPrefix;
        final boolean silent;

        DockerLogCallback(String logPrefix, boolean silent) {
            this.logPrefix = logPrefix;
            this.silent = silent;
            sb = new StringBuilder();
        }

        public void onLog(String line) {
            if (!silent) {
                log(logPrefix, line);
            }

            sb.append(removeAnsiColors(line))
                    .append(System.lineSeparator());
        }

        private static String removeAnsiColors(String s) {
            return s.replaceAll("\u001B\\[[;\\d]*m", "");
        }

        private static void log(String prefix, String s) {
            System.out.print("\u001b[34mtaurus\u001b[0m " + prefix + ": ");
            System.out.print(s);
            System.out.println();
        }

        public String fullLog() {
            return sb.toString();
        }
    }

}
