package com.walmartlabs.concord.plugins.taurus;

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

public final class Constants {

    public static final String BINARY_NAME = "bzt";
    public static final String DEFAULT_EXECUTOR = "jmeter";
    public static final String DEPENDENCY_JMETER = "apache-jmeter.zip";
    public static final String DEPENDENCY_CMD_RUNNER = "cmdrunner-" + Version.getCmdRunnerVersion() + ".jar";
    public static final String DEPENDENCY_CASUTG = "jmeter-plugins-casutg-" + Version.getPluginsCasutgVersion() + ".jar";
    public static final String DEPENDENCY_PLUGINS_MGR = "jmeter-plugins-manager-" + Version.getPluginMgrVersion() + ".jar";
    public static final String PLUGIN_MANAGER_CMD = "PluginsManagerCMD.sh";
    public static final String DEPENDENCY_PRMCTL = "jmeter-plugins-prmctl-" + Version.getPluginsPrmctlVersion() + ".jar";
    public static final String JMETER_TMP_DIR = ".bzt/jmeter-taurus";
    public static final String JMETER_PATH = ".bzt/jmeter-taurus/apache-jmeter-" + Version.getJMeterVersion();
    public static final String JMETER_PATH_BIN = JMETER_PATH + "/bin/";
    public static final String JMETER_PATH_LIB = JMETER_PATH + "/lib/";
    public static final String JMETER_PATH_EXT = JMETER_PATH_LIB + "ext/";

    private Constants() {
    }
}
