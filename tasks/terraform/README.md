# Terraform Plugin for Concord

Based on the internal version by:
- [Prasanth Pendam](https://github.com/ppendha)
- [Yury Brigadirenko](https://github.com/brig)

## Testing

### TerraformTaskIT

The test uses [testcontainers-concord](https://github.com/concord-workflow/testcontainers-concord)
and should be self-contained.

Run

```
./mvnw -pl :terraform-task clean install -DskipTests
./mvnw -pl :terraform-task integration-test -Pit
```

### TerraformTaskTest

In order to run `TerraformTaskTestV1` that works against AWS you need to set up
the following envars, so the test can pick up the right resources to execute
correctly:

```
CONCORD_TMP_DIR         = /tmp/concord
AWS_ACCESS_KEY_ID       = [your_aws_access_key]
AWS_SECRET_ACCESS_KEY   = [your_aws_secret_key]
TF_TEST_FILE            = /path/to/your/main.tf         # optional
PRIVATE_KEY_PATH        = /path/to/your/<aws_pem_file>  # optional
TF_TEST_DOCKER_IMAGE    = dockerImage/withDeps:latest   # optional
TF_TEST_HOSTNAME        = my-laptop.local               # optional
TF_TOOL_URL             = https://...                   # optional
```

__NOTE: Runtime-v2 test(s) make API call to the state backend both inside the
docker container and outside. `localhost` doesn't work for that on Docker for Mac
so it's best to provide a specific hostname. Just the hostname may work, but it may
require a search domain (e.g. `myhost.local`) depending on your network setup (corp
vpn, home router settings)__

Alternatively, you can set up the following:

1) Create an `~/.aws/credentials` file with a `concord-integration-tests`
   stanza. The access key id and secret key will be taken from
   the `concord-integration-tests` stanza:

```
[default]
aws_access_key_id=xxx
aws_secret_access_key=xxx

[concord-integration-tests]
aws_access_key_id=xxx
aws_secret_access_key=xxx
```

If you do have the standard `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY`
envars set they will be used as a fallback if the `~/.aws/credentials` file
doesn't exist.

2) Create a keypair in AWS called `concord-integration-tests` and place
   the downloaded PEM file here: `~/.aws/concord-integration-tests.pem`.
   This will be used as the private key for the integration test.

The Terraform file used for the test will default
the `src/test/terraform/main.tf` and the value for the `CONCORD_TMP_DIR`
envar will be set for you if it is not present.

### Payload Test

Use [`testPayload.sh`](testPayload.sh) to test the task against a "real" Concord
instance. It's still best for this to run locally in
[Docker containers](https://concord.walmart.com/docs/getting-started/install/docker.html).

This is, currently, the only way to test the `dockerImage` option.

`testPayload.sh` can be configured with a number of environment variables.

```shell
SERVER_URL=http://localhost:8001 \
  ORG=Default \
  PROJECT=terraform-test \
  TF_DOCKER_IMAGE=zenika/terraform-aws-cli:latest \
  RUNTIME=v2 \
  TF_VERSION=1.2.6 \
  TF_FILE=./src/test/terraform/minimal_aws/main.tf \
  ENTRY_POINT=fullTestCustomConfig \
  EXTA_VARS_FILE=~/.aws/credentials.tfvars \
  ./testPayload.sh
```

Optionally, the variables can be stored in a properties file. This makes usage
a bit cleaner by storing the less-often changed variables out of sight.

Examples properties file `~/.aws/.dev.aws.props`
```properties
SERVER_URL=http://localhost:8001
ORG=Default
PROJECT=terraform-test
TF_DOCKER_IMAGE=hub.docker.prod.walmart.com/zenika/terraform-aws-cli:latest
TF_FILE=./src/test/terraform/minimal_aws/main.tf
EXTA_VARS_FILE=~/.aws/credentials.tfvars
```

Set a `PARAM_FILE` env var to let th script know where to load them from.

```shell
$ PARAM_FILE=.dev.aws.props ENTRY_POINT=fullTestCustomConfig RUNTIME=v1 ./testPayload.sh
```
