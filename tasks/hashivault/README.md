# Hashicorp Vault Plugin

## Integration Tests

### Testcontainers Configuration

The integration tests use Testcontainers'
[Hashicorp Vault Module](https://www.testcontainers.org/modules/vault/) to test
against a real Vault instance and [Nignx Module](https://www.testcontainers.org/modules/nginx/)
to validate TLS certificate handling. Some settings may need to be
[customized](https://www.testcontainers.org/features/configuration/) depending
on what system the tests are run. These settings can be provided via environment
variables or `.testcontainers.properties` file in the user's home directory.

```shell
# docker may need to run the container in privileged mode
export TESTCONTAINERS_RYUK_CONTAINER_PRIVILEGED=true  # default is false

# If you don't need Ryuk to clean up after tests
export TESTCONTAINERS_RYUK_DISABLED=true # default is false

# customize container images if using a private/proxy image repo
export TESTCONTAINERS_RYUK_CONTAINER_IMAGE=my.proxy.repo.com/testcontainers/ryuk:0.3.1
export TESTCONTAINERS_TINYIMAGE_CONTAINER_IMAGE=my.proxy.repo.com/library/alpine:3.5
export VAULT_IMAGE_VERSION=my.proxy.repo.com/library/vault:1.1.3

mvn clean test
```

#### NGINX TLS Certificates

The integration tests use self-signed certificates to validate functionality.
They're included as resource files in the project. If, for some reason, they
need to be replaced then here's how they were originally created.

```shell
# generate the private key to become a local CA
$ openssl genrsa -des3 -out ca.key 2048

# generate CA root cert
$ openssl req -x509 -new -nodes -key ca.key -sha256 -days 99999 -out ca.pem

# generate private key for server certificate
$ openssl genrsa -out server.key 2048

# create CSR
$ openssl req -new -key server.key -out server.csr

# generate server cert
# cert.ext is also a resource in the project
$ openssl x509 -req -in server.csr -CA ca.pem -CAkey ca.key \
    -CAcreateserial -out server.crt -days 99999 -sha256 -extfile cert.ext
```

The above commands can be run in the `test/resources` directory or elsewhere
and then just copy over the `ca.pem`, `server.crt`, and `server.key`.

## Test with Concord CLI

Run the task against a live instance with the
[Concord CLI](https://concord.wlmartlabs.com/docs/cli/running-flows.html) with the
included `test.sh` script.

**Prerequisites**:
1. Maven (`mvn` binary in `PATH`)
2. [Install](https://concord.wlmartlabs.com/docs/cli/index.html) Concord CLI
   executable jar
3. Install the current version (usually a `-SNAPSHOT`) of the tasks parent project
   to local maven repository (e.g. just run `mvn -DskipTests=true clean install`
   in the parent project).

```shell
VAULT_BASE_URL=http://my-vault-svc:8200 \
VAULT_TOKEN=my-token \
VAULT_NS=/my-namespace \
VAULT_PATH=secret/my-secret \
./testCLI.sh
```

Or put the variables in a properties file and give that to the script.

```shell
PARAM_FILE=~/.my-vault-vars.properties ./testCLI.sh
```

## Test with Concord Payload

Use the `testPayload.sh` script to execute a Concord process which calls to the
`hashivault` task. The workflow will write to a given vault path (make sure it
doesn't contain important data) and then read back the data to verify the calls.

```shell
SERVER_URL=http://localhost:8001 \
VAULT_BASE_URL=http://my-vault-svc:8200 \
VAULT_TOKEN=my-token \
VAULT_NS=/my-namespace \
VAULT_PATH=secret/my-secret \
./testPayload.sh
```

Or put the variables in a properties file and give that to the script.

```shell
PARAM_FILE=~/.my-vault-vars.properties ./testPayload.sh
```
