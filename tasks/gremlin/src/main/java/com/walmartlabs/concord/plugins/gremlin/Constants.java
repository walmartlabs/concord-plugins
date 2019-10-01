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

import java.util.Arrays;
import java.util.List;

public class Constants {

    public static final List<String> GREMLIN_VALID_PROTOCOLS = Arrays.asList("TCP", "UDP", "ICMP");
    public static final String GREMLIN_DEFAULT_TARGET_TYPE = "Exact";
    public static final String GREMLIN_DEFAULT_ENDPOINT_TYPE = "hosts";
    public static final List<String> GREMLIN_VALID_UNIT_OPTION = Arrays.asList("GB", "MB", "PERCENT");

    private Constants() {
    }
}
