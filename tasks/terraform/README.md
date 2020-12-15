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

In order to run `TerraformTaskTest` that works against AWS you need to set up
the following envars, so the test can pick up the right resources to execute
correctly:

```
CONCORD_TMP_DIR         = /tmp/concord
AWS_ACCESS_KEY_ID	    = [your_aws_access_key]
AWS_SECRET_ACCESS_KEY	= [your_aws_secret_key]
TF_TEST_FILE	        = /path/to/your/main.tf
PRIVATE_KEY_PATH        = /path/to/your/<aws_pem_file>
```

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
