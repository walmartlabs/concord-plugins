package com.walmartlabs.concord.plugins.jenkins;

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

import com.walmartlabs.concord.runtime.v2.sdk.Variables;

import java.util.Collections;
import java.util.Map;

import static com.walmartlabs.concord.plugins.jenkins.Utils.merge;
import static com.walmartlabs.concord.plugins.jenkins.Utils.normalizeUrl;

public class JenkinsConfiguration {

    public static JenkinsConfiguration of(Variables in, Map<String, Object> defaults) {
        Variables vars = merge(in, defaults);
        return new JenkinsConfiguration(vars);
    }

    private static final int DEFAULT_CONNECT_TIMEOUT = 30;
    private static final int DEFAULT_WRITE_TIMEOUT = 30;
    private static final int DEFAULT_READ_TIMEOUT = 30;

    private final String baseUrl;
    private final String username;
    private final String apiToken;
    private final long connectTimeout;
    private final long writeTimeout;
    private final long readTimeout;
    private final Map<String, Object> parameters;
    private final String jobName;
    private final boolean sync;
    private final long jobTimeout;
    private final boolean debug;

    public JenkinsConfiguration(Variables v) {
        this.baseUrl = normalizeUrl(v.assertString(Constants.BASE_URL_KEY));
        this.username = v.assertString(Constants.USERNAME_KEY);
        this.apiToken = v.assertString(Constants.API_TOKEN_KEY);
        this.jobName = v.assertString(Constants.JOB_NAME_KEY);
        this.parameters = v.getMap(Constants.PARAMETERS_KEY, Collections.emptyMap());
        this.connectTimeout = v.getNumber(Constants.CONNECTION_TIMEOUT_KEY, DEFAULT_CONNECT_TIMEOUT).longValue();
        this.writeTimeout = v.getInt(Constants.WRITE_TIMEOUT_KEY, DEFAULT_WRITE_TIMEOUT);
        this.readTimeout = v.getInt(Constants.READ_TIMEOUT_KEY, DEFAULT_READ_TIMEOUT);
        this.sync = v.getBoolean(Constants.SYNC_KEY, true);
        this.jobTimeout = v.getNumber(Constants.JOB_TIMEOUT_KEY, -1).longValue();
        this.debug = v.getBoolean(Constants.DEBUG_KEY, false);
    }

    public long getConnectTimeout() {
        return connectTimeout;
    }

    public long getReadTimeout() {
        return readTimeout;
    }

    public long getWriteTimeout() {
        return writeTimeout;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getJobName() {
        return jobName;
    }

    public String getApiToken() {
        return apiToken;
    }

    public String getUsername() {
        return username;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public boolean isSync() {
        return sync;
    }

    public long getJobTimeout() {
        return jobTimeout;
    }

    public boolean isDebug() {
        return debug;
    }
}
