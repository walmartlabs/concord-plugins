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

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by ppendha on 4/15/19.
 */
public final class Version {
    private static final String VERSION;

    static {
        Properties props = new Properties();
        try {
            InputStream in = Version.class.getClassLoader().getResourceAsStream("version.properties");
            props.load(in);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        VERSION = props.getProperty("version");
    }

    public static String getVersion() {
        return VERSION;
    }
}
