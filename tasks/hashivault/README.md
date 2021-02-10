# Hashicorp Vault Plugin

## Integration Tests

### Testcontainers Configuration

The integration tests use Testcontainers'
[Hashicorp Vault Module](https://www.testcontainers.org/modules/vault/) to test
against a real Vault instance. Some settings may need to be
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
./test.sh
```

Or put the variables in a properties file and give that to the script.

```shell
PARAM_FILE=~/.my-vault-vars.properties ./test.sh
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
PARAM_FILE=~/.my-vault-vars.properties ./test.sh
```
