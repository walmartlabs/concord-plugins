package com.walmartlabs.concord.plugins.ldap.it;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2021 Walmart Inc., Concord Authors
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

import com.walmartlabs.concord.plugins.ldap.LdapTaskCommon;
import com.walmartlabs.concord.plugins.ldap.TaskParams;
import com.walmartlabs.concord.runtime.v2.sdk.MapBackedVariables;
import com.walmartlabs.concord.sdk.MapUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class LdapIT {

    private static final String CONTAINER_CERT_PATH = "/container/service/slapd/assets/certs";

    @ClassRule
    public final static GenericContainer<?> oldap =
            new GenericContainer<>(DockerImageName.parse("osixia/openldap:1.3.0"))
                    .withEnv("LDAP_ADMIN_PASSWORD", "oldap123")
                    .withEnv("LDAP_TLS_CRT_FILENAME", "server.crt")
                    .withEnv("LDAP_TLS_KEY_FILENAME", "server.key")
                    .withEnv("LDAP_TLS_CA_CRT_FILENAME", "ca.crt")
                    // don't require client certificate auth
                    .withEnv("LDAP_TLS_VERIFY_CLIENT", "never")
                    .withCopyFileToContainer(MountableFile.forClasspathResource("testuser.ldif"),
                            "/container/service/slapd/assets/config/bootstrap/ldif/50-bootstrap.ldif")
                    .withCopyFileToContainer(mountRes("ca.pem"), certPath("/ca.crt"))
                    .withCopyFileToContainer(mountRes("server.crt"), certPath("server.crt"))
                    .withCopyFileToContainer(mountRes("server.key"), certPath("server.key"))
                    .withExposedPorts(389, 636)
                    .withCommand("--copy-service");

    private int insecurePort;
    private int securePort;
    private String ldapCert;


    @Before
    public void setup() {
        insecurePort = oldap.getMappedPort(389);
        securePort = oldap.getMappedPort(636);

        ldapCert = oldap.copyFileFromContainer("/container/service/slapd/assets/certs/server.crt", is -> {
            StringBuilder sb = new StringBuilder();

            try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            }

            return sb.toString();
        });
    }

    /**
     * @param file resource to mount
     * @return a {@link MountableFile} from a given resource name
     */
    private static MountableFile mountRes(String file) {
        return MountableFile.forClasspathResource(file);
    }

    /**
     * @param file cert filenam to be copied into an openldap container
     * @return osixia/openldap-compatible certificate path
     */
    private static String certPath(String file) {
        return CONTAINER_CERT_PATH + "/" + file;
    }

    private Map<String, Object> getUser(Map<String, Object> baseParams) {
        baseParams.putIfAbsent("ldapAdServer", "ldap://localhost:" + insecurePort);
        baseParams.putIfAbsent("bindUserDn", "CN=admin,dc=example,dc=org");
        baseParams.putIfAbsent("bindPassword", "oldap123");
        baseParams.putIfAbsent("action", "getUser");
        baseParams.putIfAbsent("searchBase", "DC=example,DC=org");
        baseParams.putIfAbsent("user", "testuser@example.org");
        baseParams.putIfAbsent("dn", "cn=testuser,dc=example,dc=org");

        TaskParams taskParams = TaskParams.of(new MapBackedVariables(baseParams), Collections.emptyMap());
        return new LdapTaskCommon().execute(taskParams);
    }

    @Test
    public void testGetUser() {
        Map<String, Object> result = getUser(new HashMap<>());
        assertTrue(MapUtils.getBoolean(result, "success", false));
    }

    @Test
    public void testCustomCerts() {
        Map<String, Object> params = new HashMap<>();
        params.put("ldapAdServer", "ldaps://localhost:" + securePort);

        // -- attempt with secure port, but no extra certs (expect failure)

        try {
            getUser(params);
            fail("TLS connection with self-signed cert must fail when no cert provided");
        } catch (Exception expected) {
            // TODO: the task could throw a more direct exception so we could catch
            // TODO: it without hoping the error text stays the same.
            assertTrue(expected.getMessage().contains("unable to find valid certification path to requested target"));
        }

        // -- attempt again with CA certs (should succeed)

        Map<String, Object> certificate = new HashMap<>(1);
        certificate.put("text", ldapCert);
        params.put("certificate", certificate);

        Map<String, Object> result = getUser(params);

        assertTrue(MapUtils.getBoolean(result, "success", false));
    }
}
