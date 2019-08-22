package com.walmartlabs.concord.plugins.terraform;

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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.walmartlabs.concord.common.IOUtils;
import com.walmartlabs.concord.plugins.terraform.backend.SupportedBackend;
import com.walmartlabs.concord.sdk.*;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.*;

//
// To run this test you need to set the following envars set:
//
// CONCORD_TMP_DIR	= /tmp/concord
// AWS_ACCESS_KEY	= <your_aws_access_key>
// AWS_SECRET_KEY	= <your_aws_secret_key>
// TF_TEST_FILE	    = <path_to>/concord-plugins/tasks/terraform/src/test/terraform/main.tf or another file you want to test
// PRIVATE_KEY_PATH = <your_aws_pem_file>
//
// Alternatively you can use the following:
//
// - An ~/.aws/credentials with a [concord-integration-tests] stanza where the access key id and secret key will be taken from
// - The default src/test/terraform/main.tf test file
// - A ~/.aws/conconrd-integration-tests.pem that will be used as the private key
// - The CONCORD_TMP_DIR envar will be set for you to /tmp/concord
//
// Once setup this should just allow you to run the test.
//
// TODO: need to test destroy, computes are left in AWS
// TODO: split test apart to prepare for testing OCI/GCP
//

@Ignore
public class TerraformTaskTest {

    private final static String CONCORD_TMP_DIR_KEY = "CONCORD_TMP_DIR";
    private final static String CONCORD_TMP_DIR_VALUE = "/tmp/concord";
    private final static String CONCORD_AWS_CREDENTIALS_KEY = "concord-integration-tests";

    private String basedir;

    private AWSCredentials awsCredentials;
    private Path workDir;
    private Path dstDir;
    private Path testFile;
    private LockService lockService;
    private ObjectStorage objectStorage;
    private SecretService secretService;

    @Before
    public void setup() throws Exception {
        basedir = new File("").getAbsolutePath();

        awsCredentials = awsCredentials();
        workDir = workDir();
        testFile = terraformTestFile();

        System.out.println("Using the following:");
        System.out.println();
        System.out.println("AWS Access Key ID: " + awsCredentials.accessKey);
        System.out.println("   AWS Secret Key: " + awsCredentials.secretKey);
        System.out.println("   Terraform file: " + testFile);
        System.out.println("          workDir: " + workDir);
        System.out.println();

        Files.createDirectories(workDir);
        dstDir = workDir;
        Files.copy(testFile, dstDir.resolve(testFile.getFileName()));

        lockService = mock(LockService.class);
        objectStorage = createObjectStorage(wireMockRule);
        secretService = createSecretService(workDir);
    }

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.wireMockConfig()
            .port(12345));

    @Test
    @SuppressWarnings("unchecked")
    public void test() throws Exception {

        TerraformTask t = new TerraformTask(lockService, objectStorage, secretService);

        Map<String, Object> args = baseArguments(workDir, dstDir, TerraformTask.Action.PLAN.name());
        args.put(Constants.VARS_FILES, varFiles());
        args.put(Constants.EXTRA_VARS_KEY, extraVars());
        args.put(Constants.GIT_SSH_KEY, gitSsh());

        Context ctx = new MockContext(args);
        t.execute(ctx);

        // ---

        Map<String, Object> result = (Map<String, Object>) ctx.getVariable(Constants.RESULT_KEY);
        assertTrue((boolean) result.get("ok"));
        assertNotNull(result.get("planPath"));

        // ---

        verify(lockService, times(1)).projectLock(any(), any());
        verify(lockService, times(1)).projectUnlock(any(), any());

        // ---

        System.out.println("===================================================================================");

        args = baseArguments(workDir, dstDir, TerraformTask.Action.APPLY.name());
        args.put(Constants.DESTROY_KEY, true);
        args.put(Constants.PLAN_KEY, result.get("planPath"));

        ctx = new MockContext(args);
        t.execute(ctx);
        result = (Map<String, Object>) ctx.getVariable(Constants.RESULT_KEY);
        System.out.println(result);

        //
        // Check the output contains our two variables populated with values from our var files
        //
        // Outputs:
        //
        // message = hello!
        // name_from_varfile0 = bob
        // time_from_varfile1 = now
        //
        assertTrue(((String)result.get("output")).contains("name_from_varfile0 = bob"));
        assertTrue(((String)result.get("output")).contains("time_from_varfile1 = now"));

        // ---

        System.out.println("===================================================================================");

        //
        // Simulate having some outputs being created in the terraform state. The output action will not work
        // correctly without outputs present in the terraform state.
        //
        String terraformStateWithOutputs = responseTemplate("terraform.tfstate");
        wireMockRule.stubFor(get("/test").willReturn(aResponse().withBody(terraformStateWithOutputs).withStatus(200)));

        args = baseArguments(workDir, dstDir, TerraformTask.Action.OUTPUT.name());
        ctx = new MockContext(args);
        t.execute(ctx);
    }

    @Test
    public void validateS3BackendConfigurationSerialization() throws Exception {
        SupportedBackend backend = new SupportedBackend(true, "s3", s3BackendParameters(), new ObjectMapper());
        Map<String, Object> args = baseArguments(workDir, dstDir, TerraformTask.Action.PLAN.name());
        backend.init(new MockContext(args), dstDir);

        File overrides = dstDir.resolve("concord_override.tf.json").toFile();
        try (Reader reader = new FileReader(overrides)) {
            JSONObject overridesJson = new JSONObject(new JSONTokener(reader));
            JSONObject s3 = overridesJson.getJSONObject("terraform").getJSONObject("backend").getJSONObject("s3");
            assertEquals("bucket-value", s3.getString("bucket"));
            assertEquals("key-value", s3.getString("key"));
            assertEquals("region-value", s3.getString("region"));
            assertEquals("dynamodb_table-value", s3.getString("dynamodb_table"));
            assertTrue(s3.getBoolean("encrypt"));
        }
    }

    private Map<String,Object> extraVars() throws Exception {
        Map<String, Object> extraVars = new HashMap<>();
        extraVars.put("aws_access_key", awsCredentials.accessKey);
        extraVars.put("aws_secret_key", awsCredentials.secretKey);
        return extraVars;
    }

    private List<String> varFiles() throws Exception {
        //
        // We place our user supplied var files in the workspace and they are declared as being relative
        // to the Concord workspace. So we copy them into the workspace at the root so we just refer to
        // them as "varfile0.tfvars" and varfile1.tfvars.
        //
        Path varfile0 = varFile("varfile0.tfvars");
        Files.copy(varfile0, dstDir.resolve(varfile0.getFileName()));
        Path varfile1 = varFile("varfile1.tfvars");
        Files.copy(varfile1, dstDir.resolve(varfile1.getFileName()));

        return new ArrayList<>(Arrays.asList("varfile0.tfvars", "varfile1.tfvars"));
    }

    private Map<String,Object> gitSsh() throws Exception {
        Map<String, Object> gitSsh = new HashMap<>();
        gitSsh.put(GitSshWrapper.PRIVATE_KEYS_KEY, Collections.singletonList(Files.createTempFile("test", ".key").toAbsolutePath().toString()));
        gitSsh.put(GitSshWrapper.SECRETS_KEY, Collections.singletonList(Collections.singletonMap("secretName", "test")));
        return gitSsh;
    }

    private static ObjectStorage createObjectStorage(WireMockRule wireMockRule) throws Exception {
        String osAddress = "http://localhost:" + wireMockRule.port() + "/test";
        wireMockRule.stubFor(get("/test").willReturn(aResponse().withStatus(404)));
        wireMockRule.stubFor(post("/test").willReturn(aResponse().withStatus(200)));

        ObjectStorage os = mock(ObjectStorage.class);
        when(os.createBucket(any(), anyString())).thenReturn(ImmutableBucketInfo.builder()
                .address(osAddress)
                .build());

        return os;
    }

    private static SecretService createSecretService(Path workDir) throws Exception {
        String pemFileEnvar = System.getenv("PRIVATE_KEY_PATH");
        Path dst = Files.createTempFile(workDir, "private", ".key");
        if (pemFileEnvar != null) {
            Files.copy(Paths.get(pemFileEnvar), dst, StandardCopyOption.REPLACE_EXISTING);
        } else {
            // Look for an ~/.aws/concord-integration-tests.pem file
            File pemFile = new File(System.getProperty("user.hom"), ".aws/" + CONCORD_AWS_CREDENTIALS_KEY + ".pem");
            if (pemFile.exists()) {
                Files.copy(pemFile.toPath(), dst, StandardCopyOption.REPLACE_EXISTING);
            }
        }

        Map<String, String> m = Collections.singletonMap("private", workDir.relativize(dst).toString());

        SecretService ss = mock(SecretService.class);
        when(ss.exportKeyAsFile(any(), any(), any(), any(), any(), any())).thenReturn(m);

        return ss;
    }

    private Map<String, Object> baseArguments(Path workDir, Path dstDir, String actionKey) {
        Map<String, Object> args = new HashMap<>();
        args.put(com.walmartlabs.concord.sdk.Constants.Request.PROCESS_INFO_KEY, Collections.singletonMap("sessionKey", "xyz"));
        args.put(com.walmartlabs.concord.sdk.Constants.Context.WORK_DIR_KEY, workDir.toAbsolutePath().toString());
        args.put(Constants.ACTION_KEY, actionKey);
        args.put(Constants.DEBUG_KEY, true);
        args.put(Constants.STATE_ID_KEY, "testState");
        args.put(Constants.DIR_KEY, dstDir.toAbsolutePath().toString());
        return args;
    }

    private Path varFile(String name) {
        return new File(basedir, "src/test/terraform/" + name).toPath();
    }

    private String responseTemplate(String name) throws IOException {
        return new String(Files.readAllBytes(new File(basedir, "src/test/terraform/" + name).toPath()));
    }

    //
    // - task: terraform
    //     in:
    //       backend:
    //         s3:
    //           bucket: "${bucket}"
    //           key: "${key}"
    //           region: "${region}"
    //           encrypt: ${encrypt}
    //           dynamodb_table: "${dynamodb_table}"
    //
    private Map<String,Object> s3BackendParameters() {
        Map<String, Object> map = new HashMap();
        map.put("bucket", "bucket-value");
        map.put("key", "key-value");
        map.put("region", "region-value");
        map.put("encrypt", true);
        map.put("dynamodb_table", "dynamodb_table-value");
        return map;
    }

    //
    // Helpers for using AWS credentials in ~/.aws/credentials
    //

    private AWSCredentials awsCredentials() {
        AWSCredentials awsCredentials = new AWSCredentials();
        File awsCredentialsFile = new File(System.getProperty("user.home"), ".aws/credentials");
        if (awsCredentialsFile.exists()) {
            Map<String,Properties> awsCredentialsIni = parseIni(awsCredentialsFile);
            if (awsCredentialsIni != null) {
                Properties concordAwsCredentials = awsCredentialsIni.get(CONCORD_AWS_CREDENTIALS_KEY);
                awsCredentials.accessKey = concordAwsCredentials.getProperty("aws_access_key_id");
                awsCredentials.secretKey = concordAwsCredentials.getProperty("aws_secret_access_key");
            }
        }

        if (awsCredentials.accessKey.isEmpty() && awsCredentials.secretKey.isEmpty()) {
            awsCredentials.accessKey = System.getenv("AWS_ACCESS_KEY");
            if (awsCredentials.accessKey == null) {
                awsCredentials.accessKey = System.getenv("AWS_ACCESS_KEY_ID");
            }
            awsCredentials.secretKey = System.getenv("AWS_SECRET_KEY");
            if (awsCredentials.secretKey == null) {
                awsCredentials.secretKey = System.getenv("AWS_SECRET_ACCESS_KEY");
            }
        }

        if (awsCredentials.accessKey.isEmpty() && awsCredentials.secretKey.isEmpty()) {
            throw new RuntimeException(String.format("An AWS access key id and secret key must be set using envars or the ~/.aws/credentials file with the %s profile.", CONCORD_AWS_CREDENTIALS_KEY));
        }

        return awsCredentials;
    }

    private static class AWSCredentials {
        String accessKey;
        String secretKey;

        @Override
        public String toString() {
            return "AWSCredentials{" +
                    "accessKey='" + accessKey + '\'' +
                    ", secretKey='" + secretKey + '\'' +
                    '}';
        }
    }

    private static Map<String, Properties> parseIni(File file) {
        try (Reader reader = new FileReader(file)) {
            Map<String, Properties> result = new HashMap();
            new Properties() {

                private Properties section;

                @Override
                public Object put(Object key, Object value) {
                    String header = (key + " " + value).trim();
                    if (header.startsWith("[") && header.endsWith("]")) {
                        return result.put(header.substring(1, header.length() - 1), section = new Properties());
                    } else {
                        return section.put(key, value);
                    }
                }

            }.load(reader);
            return result;
        } catch (IOException e) {
            return null;
        }
    }

    //
    // Helpers for setting envars and setting CONCORD_TMP_DIR envar
    //

    private Path workDir() throws Exception {
        String concordTmpDir = System.getenv(CONCORD_TMP_DIR_KEY);
        if (concordTmpDir == null) {
            // Grab the old environment and add the CONCORD_TMP_DIR value to it and reset it
            Map<String,String> newEnvironment = new HashMap();
            newEnvironment.putAll(System.getenv());
            newEnvironment.put(CONCORD_TMP_DIR_KEY, CONCORD_TMP_DIR_VALUE);
            setNewEnvironment(newEnvironment);
        }
        return IOUtils.createTempDir("test");
    }

    private Path terraformTestFile() {
        String terraformTestFileEnvar = System.getenv("TF_TEST_FILE");
        if (terraformTestFileEnvar != null) {
            return Paths.get(System.getenv("TF_TEST_FILE"));
        }
        // Use the test terraform file we have in the repository
        return new File(basedir, "src/test/terraform/main.tf").toPath();
    }

    private static void setNewEnvironment(Map<String, String> newEnvironment) throws Exception {
        try {
            Class<?> processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment");
            Field theEnvironmentField = processEnvironmentClass.getDeclaredField("theEnvironment");
            theEnvironmentField.setAccessible(true);
            Map<String, String> env = (Map<String, String>) theEnvironmentField.get(null);
            env.putAll(newEnvironment);
            Field theCaseInsensitiveEnvironmentField = processEnvironmentClass.getDeclaredField("theCaseInsensitiveEnvironment");
            theCaseInsensitiveEnvironmentField.setAccessible(true);
            Map<String, String> cienv = (Map<String, String>)     theCaseInsensitiveEnvironmentField.get(null);
            cienv.putAll(newEnvironment);
        } catch (NoSuchFieldException e) {
            Class[] classes = Collections.class.getDeclaredClasses();
            Map<String, String> env = System.getenv();
            for(Class cl : classes) {
                if("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
                    Field field = cl.getDeclaredField("m");
                    field.setAccessible(true);
                    Object obj = field.get(env);
                    Map<String, String> map = (Map<String, String>) obj;
                    map.clear();
                    map.putAll(newEnvironment);
                }
            }
        }
    }
}
