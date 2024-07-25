package com.walmartlabs.concord.plugins.msteams.it;

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

import com.walmartlabs.concord.plugins.msteams.Utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class AbstractIT {

    /**
     * Loads default parameter vars for calling task or common functions.
     * <p />
     * Must be in JSON format. Default path is <code>MODULE_DIR/it_vars/default.json</code>,
     * and can be customized with <code>MSTEAMS_IT_VARS</code> environment variable
     * <p />
     * Sensitive vars to include:
     * <ul>
     *     <li><code>webhookTypeId</code></li>
     *     <li><code>tenantId</code></li>
     *     <li><code>rootWebhookUrl</code></li>
     *     <li><code>clientId</code></li>
     *     <li><code>clientSecret</code></li>
     *     <li><code>rootApi</code></li>
     *     <li><code>accessTokenApi</code></li>
     *     <li><code>teamId</code></li>
     *     <li><code>webhookId</code></li>
     *     <li><code>channelId</code></li>
     * </ul>
     * <p />
     * Optional:
     * <ul>
     *     <li><code>useProxy</code></li>
     *     <li><code>proxyAddress</code></li>
     *     <li><code>proxyPort</code></li>
     * </ul>
     */
    protected Map<String, Object> defaultVars() {
        var varsPath = Paths.get(Optional.ofNullable(System.getenv("MSTEAMS_IT_VARS"))
                .orElse("it_vars/default.json"));

        if (!Files.exists(varsPath)) {
            throw new IllegalStateException("Test vars not found: " + varsPath);
        }

        var type = Utils.mapper().getTypeFactory().constructMapType(HashMap.class, String.class, Object.class);

        try (var is = Files.newInputStream(varsPath)) {
            return Utils.mapper().readValue(is, type);
        } catch (IOException e) {
            throw new IllegalStateException("Error reading sensitive files for IT", e);
        }
    }

}
