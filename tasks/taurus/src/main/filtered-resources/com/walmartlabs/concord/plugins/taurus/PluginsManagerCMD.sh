#!/usr/bin/env bash

java -Djava.awt.headless=true $JVM_ARGS -jar $(dirname $0)/../lib/cmdrunner-${cmdRunnerVersion}.jar --tool org.jmeterplugins.repository.PluginManagerCMD "$@"