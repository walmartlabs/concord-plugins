package com.walmartlabs.concord.plugins.puppet;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2024 Walmart Inc., Concord Authors
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

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class IgnoringTrustManager implements X509TrustManager {
    private final boolean validateNotAfter;

    public IgnoringTrustManager(boolean validateNotAfter) {
        this.validateNotAfter = validateNotAfter;
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        assertDateValidity(chain);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        assertDateValidity(chain);
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }

    private void assertDateValidity(X509Certificate[] chain) throws CertificateException {
        if (!validateNotAfter) {
            return;
        }

        for (X509Certificate cert : chain) {
            assertDateValidity(cert);
        }
    }

    static void assertDateValidity(X509Certificate cert) throws CertificateException {
        var epochSecond = cert.getNotAfter().toInstant().getEpochSecond();
        var notAfter = LocalDateTime.ofEpochSecond(epochSecond, 0, ZoneOffset.ofHours(0));
        var now = ZonedDateTime.now(ZoneId.of("UTC")).toLocalDateTime();

        if (now.isAfter(notAfter)) {
            var name = cert.getSubjectX500Principal().getName();
            throw new CertificateException("Validity expired for certificate '"
                    + name + "'. Now: " + now + ", notAfter: " + notAfter);
        }
    }

    public static TrustManager[] getManagers(boolean validateNotAfter) {
        TrustManager[] tms = new TrustManager[1];
        tms[0] = new IgnoringTrustManager(validateNotAfter);
        return tms;
    }
}
