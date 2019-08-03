package com.walmartlabs.concord.plugins.packer;

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

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.walmartlabs.concord.common.IOUtils;
import com.walmartlabs.concord.sdk.Context;
import com.walmartlabs.concord.sdk.MockContext;

@Ignore
public class PackerTaskTest {

    private final static String CONCORD_TMP_DIR_KEY = "CONCORD_TMP_DIR";
    private final static String CONCORD_TMP_DIR_VALUE = "/tmp/concord";
    private final static String CONCORD_AWS_CREDENTIALS_KEY = "concord-integration-tests";

    private String basedir;

    private AWSCredentials awsCredentials;
    private Path workDir;
    private Path dstDir;
    private Path testFile;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.wireMockConfig()
            .port(12345));

    @Before
    public void setup() throws Exception {
        basedir = new File("").getAbsolutePath();

        awsCredentials = awsCredentials();
        workDir = workDir();
        testFile = packerTestFile();

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
    }

    @Test
    @SuppressWarnings("unchecked")
    public void test() throws Exception {

        PackerTask packerTask = new PackerTask();

        Map<String, Object> args = baseArguments(workDir, dstDir);
        args.put(com.walmartlabs.concord.plugins.packer.Constants.EXTRA_VARS_KEY, extraVars());

        Context ctx = new MockContext(args);
        packerTask.execute(ctx);

        Map<String, Object> result = (Map<String, Object>) ctx.getVariable(
                com.walmartlabs.concord.plugins.packer.Constants.RESULT_KEY);
        assertTrue((boolean) result.get("ok"));

        System.out.println("===================================================================================");

        args = new HashMap<>();
        args.put(com.walmartlabs.concord.sdk.Constants.Request.PROCESS_INFO_KEY, Collections.singletonMap("sessionKey", "xyz"));
        args.put(com.walmartlabs.concord.sdk.Constants.Context.WORK_DIR_KEY, workDir.toAbsolutePath().toString());
        args.put(com.walmartlabs.concord.plugins.packer.Constants.DEBUG_KEY, true);

        ctx = new MockContext(args);
        packerTask.execute(ctx);

        System.out.println("===================================================================================");

        args = new HashMap<>();
        args.put(com.walmartlabs.concord.sdk.Constants.Context.WORK_DIR_KEY, workDir.toAbsolutePath().toString());
        args.put(com.walmartlabs.concord.plugins.packer.Constants.DEBUG_KEY, true);

        ctx = new MockContext(args);
        packerTask.execute(ctx);
    }

    private Map<String, Object> baseArguments(Path workDir, Path dstDir) {
        Map<String, Object> args = new HashMap<>();
        args.put(com.walmartlabs.concord.sdk.Constants.Request.PROCESS_INFO_KEY, Collections.singletonMap("sessionKey", "xyz"));
        args.put(com.walmartlabs.concord.sdk.Constants.Context.WORK_DIR_KEY, workDir.toAbsolutePath().toString());
        args.put(Constants.DEBUG_KEY, true);
        args.put(Constants.DIR_KEY, dstDir.toAbsolutePath().toString());
        return args;
    }

    private Map<String,Object> extraVars() throws Exception {
        Map<String, Object> extraVars = new HashMap<>();
        extraVars.put("aws_access_key", awsCredentials.accessKey);
        extraVars.put("aws_secret_key", awsCredentials.secretKey);
        return extraVars;
    }

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

    private AWSCredentials awsCredentials() {
        AWSCredentials awsCredentials = new AWSCredentials();
        File awsCredentialsFile = new File(System.getProperty("user.home"), ".aws/credentials");
        if (awsCredentialsFile.exists()) {
            Map<String, Properties> awsCredentialsIni = parseIni(awsCredentialsFile);
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

    private Path packerTestFile() {
        String terraformTestFileEnvar = System.getenv("PACKER_TEST_FILE");
        if (terraformTestFileEnvar != null) {
            return Paths.get(System.getenv("PACKER_TEST_FILE"));
        }
        // Use the test packer file we have in the repository
        return new File(basedir, "src/test/packer/packer-test.json").toPath();
    }
}
