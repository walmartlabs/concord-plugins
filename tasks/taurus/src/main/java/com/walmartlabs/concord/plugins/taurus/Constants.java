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
    public static final String DEFAULT_JMETER_URL = "https://www.apache.org/dist/jmeter/binaries/apache-jmeter-5.5.zip";
    public static final String DEPENDENCY_CMD_RUNNER = "mvn://kg.apc:cmdrunner:jar:" + Version.getCmdRunnerVersion();
    public static final String DEPENDENCY_CASUTG = "mvn://kg.apc:jmeter-plugins-casutg:jar:" + Version.getPluginsCasutgVersion();
    public static final String DEPENDENCY_PLUGINS_MGR = "mvn://kg.apc:jmeter-plugins-manager:jar:" + Version.getPluginMgrVersion();
    public static final String PLUGIN_MANAGER_CMD = "PluginsManagerCMD.sh";
    public static final String DUMMY_PLUGIN_MANAGER_CMD = "DummyPluginsManagerCMD.sh";
    public static final String DEPENDENCY_PRMCTL = "mvn://kg.apc:jmeter-plugins-prmctl:jar:" + Version.getPluginsPrmctlVersion();
    public static final String JMETER_TMP_DIR = ".bzt/jmeter-taurus";
    public static final String JMETER_PATH = ".bzt/jmeter-taurus/apache-jmeter-" + Version.getJMeterVersion();
    public static final String JMETER_PATH_BIN = JMETER_PATH + "/bin/";
    public static final String JMETER_PATH_LIB = JMETER_PATH + "/lib/";
    public static final String JMETER_PATH_EXT = JMETER_PATH_LIB + "ext/";

    private Constants() {
    }
}
