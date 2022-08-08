package com.walmartlabs.concord.plugins.terraform;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2022 Walmart Inc., Concord Authors
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

import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseTransformer;
import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.walmartlabs.concord.common.IOUtils;
import com.walmartlabs.concord.plugins.terraform.backend.BackendFactoryV1;
import com.walmartlabs.concord.plugins.terraform.docker.DockerService;
import com.walmartlabs.concord.sdk.*;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.*;
import java.lang.reflect.Field;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.*;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;

public abstract class AbstractTerraformTest {
    protected final static String CONCORD_TMP_DIR_KEY = "CONCORD_TMP_DIR";
    protected final static String CONCORD_TMP_DIR_VALUE = "/tmp/concord";
    protected final static String CONCORD_AWS_CREDENTIALS_KEY = "concord-integration-tests";

    private String basedir;

    private AWSCredentials awsCredentials;
    protected Path workDir;
    protected Path dstDir;
    private Path testFile;
    private Path downloadManagerCacheDir;
    protected LockService lockService;
    protected ObjectStorage objectStorage;
    protected BackendFactoryV1 backendManager;
    protected OKHttpDownloadManager dependencyManager;
    protected DockerService dockerService;

    @RegisterExtension
    protected static WireMockExtension wireMockRule = WireMockExtension.newInstance()
            .options(wireMockConfig()
                .extensions(new StateBackendTransformer())
                .bindAddress("0.0.0.0")
                .port(12345))
            .build();

    public void abstractSetup() throws Exception {
        basedir = new File("").getAbsolutePath();

        awsCredentials = awsCredentials();
        workDir = workDir();
        testFile = terraformTestFile();
        downloadManagerCacheDir = Paths.get("/tmp/downloadManager/cache");
        Files.createDirectories(downloadManagerCacheDir);

        System.out.println("Using the following:");
        System.out.println();
        System.out.println("AWS Access Key ID: " + awsCredentials.accessKey);
        System.out.println("   AWS Secret Key: " + awsCredentials.secretKey.substring(0, 3) + "****");
        System.out.println("   Terraform file: " + testFile);
        System.out.println("          workDir: " + workDir);
        System.out.println();

        Files.createDirectories(workDir);
        dstDir = workDir;
        Files.copy(testFile, dstDir.resolve("main.tf"));

        lockService = mock(LockService.class);
        dependencyManager = new OKHttpDownloadManager("terraform");
        dockerService = new DockerService(workDir, Collections.emptyList());
    }

    /**
     * Unsophisticated shorthand for digging into a number of nested map. Won't
     * work if there's a List somewhere in the middle. but hte final value can
     * be any object
     */
    @SuppressWarnings("unchecked")
    protected static <T> T findInMap(Map<String, Object> m, String path) {
        String[] keys = path.split("\\.");
        if (keys.length == 1) {
            return (T) m.get(keys[0]);
        }

        return findInMap(MapUtils.assertMap(m, keys[0]), path.substring(path.indexOf(".") + 1));
    }

    protected Map<String, Object> extraVars() {
        Map<String, Object> extraVars = new HashMap<>();
        extraVars.put("aws_access_key", awsCredentials.accessKey);
        extraVars.put("aws_secret_key", awsCredentials.secretKey);
        return extraVars;
    }

    protected List<String> varFiles() throws Exception {
        //
        // We place our user supplied var files in the workspace and they are declared as being relative
        // to the Concord workspace. So we copy them into the workspace at the root so we just refer to
        // them as "varfile0.tfvars" and varfile1.tfvars.
        //
        Path varfile0 = varFile("varfile0.tfvars");
        Files.copy(varfile0, dstDir.resolve(varfile0.getFileName()), StandardCopyOption.REPLACE_EXISTING);
        Path varfile1 = varFile("varfile1.tfvars");
        Files.copy(varfile1, dstDir.resolve(varfile1.getFileName()), StandardCopyOption.REPLACE_EXISTING);

        return new ArrayList<>(Arrays.asList("varfile0.tfvars", "varfile1.tfvars"));
    }

    protected Map<String, Object> gitSsh() throws Exception {
        Map<String, Object> gitSsh = new HashMap<>();
        gitSsh.put(GitSshWrapper.PRIVATE_KEYS_KEY, Collections.singletonList(Files.createTempFile("test", ".key").toAbsolutePath().toString()));
        gitSsh.put(GitSshWrapper.SECRETS_KEY, Collections.singletonList(Collections.singletonMap("secretName", "test")));
        return gitSsh;
    }

    /**
     * @return API host name (no port, not a URL) on usage of docker container (or not)
     */
    protected static String apiHostName() {
        return envToOptional("TF_TEST_HOSTNAME")
                .orElseGet(() -> dockerImage().isPresent() ?  "host.docker.internal" : "localhost");
    }

    public static class StateBackendTransformer extends ResponseTransformer {
        private String lastJsonState;

        @Override
        public com.github.tomakehurst.wiremock.http.Response transform(com.github.tomakehurst.wiremock.http.Request request,
                                                                       com.github.tomakehurst.wiremock.http.Response response,
                                                                       FileSource files,
                                                                       Parameters parameters) {

            if (request.getMethod().equals(RequestMethod.POST)) {
                if (!response.getHeaders().keys().contains("keep_payload")) {
                    fail("Unexpected POST response. No 'keep_payload' header. Double-check default stub(s)");
                }

                HttpHeader hdr = response.getHeaders().getHeader("keep_payload");
                boolean keep_payload = Boolean.parseBoolean(hdr.values().get(0));


                if (keep_payload) {
                    // TODO: persist in a semi-recoverable file?
                    lastJsonState = request.getBodyAsString();
                }
            }

            if (request.getMethod().equals(RequestMethod.GET) && lastJsonState != null) {
                return com.github.tomakehurst.wiremock.http.Response.response()
                        .status(200)
                        .body(lastJsonState)
                        .build();
            }

            return response;
        }

        @Override
        public String getName() {
            return "state-backend-transformer";
        }
    }

    protected Map<String, Object> baseArguments(Path workDir, Path dstDir, String actionKey) {
        Map<String, Object> args = new HashMap<>();
        args.put(Constants.Request.PROCESS_INFO_KEY, Collections.singletonMap("sessionKey", "xyz"));
        args.put(Constants.Context.WORK_DIR_KEY, workDir.toAbsolutePath().toString());
        args.put(TaskConstants.ACTION_KEY, actionKey);
        args.put(TaskConstants.DEBUG_KEY, true);
        args.put(TaskConstants.STATE_ID_KEY, "testState");
        args.put(TaskConstants.DIR_KEY, dstDir.toAbsolutePath().toString());
        dockerImage().ifPresent(image -> args.put(TaskConstants.DOCKER_IMAGE_KEY, image));
        toolUrl().ifPresent(toolUrl -> args.put(TaskConstants.TOOL_URL_KEY, toolUrl));
        return args;
    }

    private Path varFile(String name) {
        return new File(basedir, "src/test/terraform/" + name).toPath();
    }

    protected String responseTemplate(String name) throws IOException {
        return new String(Files.readAllBytes(new File(basedir, "src/test/terraform/" + name).toPath()));
    }

    //
    // Helpers for using AWS credentials in ~/.aws/credentials
    //

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

    private static class AWSCredentials {
        String accessKey;
        String secretKey;

        AWSCredentials() {
            this.accessKey = "";
            this.secretKey = "";
        }

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
            Map<String, Properties> result = new HashMap<>();
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
            Map<String, String> newEnvironment = new HashMap<>(System.getenv());
            newEnvironment.put(CONCORD_TMP_DIR_KEY, CONCORD_TMP_DIR_VALUE);
            setNewEnvironment(newEnvironment);
        }
        return IOUtils.createTempDir("test");
    }

    private Path terraformTestFile() {
        String terraformTestFileEnvVar = System.getenv("TF_TEST_FILE");
        if (terraformTestFileEnvVar != null) {
            return Paths.get(System.getenv("TF_TEST_FILE"));
        }
        // Use the test terraform file we have in the repository
        return new File(basedir, "src/test/terraform/main.tf").toPath();
    }

    protected static Optional<String> dockerImage() {
        return envToOptional("TF_TEST_DOCKER_IMAGE");
    }

    protected static Optional<String> toolUrl() {
        return envToOptional("TF_TOOL_URL");
    }

    private static Optional<String> envToOptional(String varName) {
        String envVal = System.getenv(varName);

        if (envVal != null && !envVal.isEmpty()) {
            return Optional.of(envVal);
        }

        return Optional.empty();
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
            Map<String, String> cienv = (Map<String, String>) theCaseInsensitiveEnvironmentField.get(null);
            cienv.putAll(newEnvironment);
        } catch (NoSuchFieldException e) {
            Class<?>[] classes = Collections.class.getDeclaredClasses();
            Map<String, String> env = System.getenv();
            for (Class<?> cl : classes) {
                if ("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
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

    static class OKHttpDownloadManager implements DependencyManager, com.walmartlabs.concord.runtime.v2.sdk.DependencyManager {

        private final Path toolDir;

        public OKHttpDownloadManager(String tool) {
            this.toolDir = Paths.get(System.getProperty("user.home"), ".m2/tools/", tool);

            if (Files.exists(toolDir)) {
                return;
            }

            Set<PosixFilePermission> perms =
                    PosixFilePermissions.fromString("rwxr-x---");

            if (Files.exists(toolDir)) {
                return;
            }


            try {
                if (!Files.exists(Files.createDirectories(toolDir, PosixFilePermissions.asFileAttribute(perms)))) {
                    throw new Exception("Failed to crate tool cache directory: " + toolDir);
                }
            } catch (Exception e) {
                throw new RuntimeException("Unable to create cache directory for terraform executable.");
            }
        }

        @Override
        public Path resolve(URI uri) throws IOException {
            String urlString = uri.toString();
            String fileName = urlString.substring(urlString.lastIndexOf('/') + 1);
            Path target = toolDir.resolve(fileName);
            if (!Files.exists(target)) {
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder().url(urlString).build();
                Call call = client.newCall(request);
                Response response = call.execute();
                download(response.body().byteStream(), target.toFile());
            }
            return target;
        }

        //
        // https://stackoverflow.com/questions/309424/how-do-i-read-convert-an-inputstream-into-a-string-in-java
        //
        // surprised this is the fastest way to convert an inputstream to a string
        //
        private void download(InputStream stream, File target) throws IOException {
            byte[] buffer = new byte[1024];
            int length;
            try (OutputStream result = Files.newOutputStream(target.toPath())) {
                while ((length = stream.read(buffer)) != -1) {
                    result.write(buffer, 0, length);
                }
            }
        }
    }

    public static void assertOutput(String regexPattern, String input) {
        String msg = "Expected: " + regexPattern + "\n"
                + "Got: " + input;
        assertEquals(1, grep(regexPattern, input).size(), msg);
    }

    public static List<String> grep(String pattern, String input) {
        List<String> result = new ArrayList<>();

        for (String line : input.split("\n")) {
            if (line.matches(pattern)) {
                result.add(line);
            }
        }

        return result;
    }
}
