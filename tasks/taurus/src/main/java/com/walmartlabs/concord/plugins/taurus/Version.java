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

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class Version {
    private static final String JMETER_VERSION;
    private static final String PLUGINMRG_VERSION;
    private static final String CMD_RUNNER_VERSION;
    private static final String CASUTG_VERSION;
    private static final String PRMCTL_VERSION;

    static {
        Properties props = new Properties();
        try {
            InputStream in = Version.class.getResourceAsStream("version.properties");
            props.load(in);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        JMETER_VERSION = props.getProperty("jmeterVersion");
        PLUGINMRG_VERSION = props.getProperty("pluginMgrVersion");
        CMD_RUNNER_VERSION = props.getProperty("cmdRunnerVersion");
        CASUTG_VERSION = props.getProperty("pluginsCasutgVersion");
        PRMCTL_VERSION = props.getProperty("pluginsPrmctlVersion");
    }

    public static String getJMeterVersion() {
        return JMETER_VERSION;
    }

    public static String getPluginMgrVersion() {
        return PLUGINMRG_VERSION;
    }

    public static String getCmdRunnerVersion() {
        return CMD_RUNNER_VERSION;
    }

    public static String getPluginsCasutgVersion() {
        return CASUTG_VERSION;
    }

    public static String getPluginsPrmctlVersion() {
        return PRMCTL_VERSION;
    }
}
