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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.security.auth.x500.X500Principal;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Timeout(value = 2, unit = TimeUnit.MINUTES)
class IgnoringTrustManagerTest {

    @Mock
    X509Certificate cert;

    @Test
    void testValidDate() {
        // datetime in the future
        when(cert.getNotAfter()).thenReturn(getDate(500));

        assertDoesNotThrow(() -> IgnoringTrustManager.assertDateValidity(cert));
    }

    @Test
    void testInvalidDate() {
        // datetime in the past
        when(cert.getNotAfter()).thenReturn(getDate(-500));
        when(cert.getSubjectX500Principal()).thenReturn(new X500Principal("CN=test"));

        var expected = Assertions.assertThrows(CertificateException.class,
                () -> IgnoringTrustManager.assertDateValidity(cert));

        assertTrue(expected.getMessage().contains("Validity expired for certificate"));
    }

    @Test
    void testSkipDateValidation() {
        var tm = new IgnoringTrustManager(false);

        assertDoesNotThrow(() -> tm.checkServerTrusted(new X509Certificate[]{cert}, null));
    }

    private static Date getDate(long offsetSeconds) {
        var nowEpochMillis = ZonedDateTime.now(ZoneId.of("UTC"))
                .plusSeconds(offsetSeconds)
                .toEpochSecond() * 1_000;
        return new Date(nowEpochMillis);
    }
}
