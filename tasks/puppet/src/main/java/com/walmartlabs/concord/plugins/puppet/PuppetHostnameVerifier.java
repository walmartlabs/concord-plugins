package com.walmartlabs.concord.plugins.puppet;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import java.net.MalformedURLException;
import java.net.URL;

public class PuppetHostnameVerifier implements HostnameVerifier {

    private static final Logger log = LoggerFactory.getLogger(PuppetHostnameVerifier.class);

    private final URL allowedUrl;

    public PuppetHostnameVerifier(String allowedUrl) throws MalformedURLException {
        this.allowedUrl = new URL(allowedUrl);
    }

    @Override
    public boolean verify(String hostname, SSLSession sslSession) {
        if (hostname.equals(allowedUrl.getHost())) {
            return true;
        }

        log.info("Unexpected hostname '{}'. Denying connection. Allowed hostname is '{}'", hostname, allowedUrl.getHost());
        return false;
    }
}
