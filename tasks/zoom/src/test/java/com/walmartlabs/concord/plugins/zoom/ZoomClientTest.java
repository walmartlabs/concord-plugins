package com.walmartlabs.concord.plugins.zoom;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2026 Walmart Inc., Concord Authors
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

import com.walmartlabs.concord.runtime.v2.sdk.MapBackedVariables;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZoomClientTest {

    @Test
    void verifySslDefaultsToTrue() {
        TaskParams params = new TaskParams(new MapBackedVariables(Map.of()));

        assertTrue(params.verifySsl());
    }

    @Test
    void verifySslCanBeExplicitlyDisabled() {
        TaskParams params = new TaskParams(new MapBackedVariables(Map.of(TaskParams.VAR_VERIFY_SSL, false)));

        assertFalse(params.verifySsl());
    }

    @Test
    void createsConnectionManagersForBothSslModes() {
        var verifying = ZoomClient.createConnManager(true);
        var nonVerifying = ZoomClient.createConnManager(false);

        assertNotNull(verifying);
        assertNotNull(nonVerifying);

        verifying.close();
        nonVerifying.close();
    }
}
