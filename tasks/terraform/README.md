# Terraform Plugin for Concord

Based on the internal version by:
- [Prasanth Pendam](https://github.com/ppendha)
- [Yury Brigadirenko](https://github.com/brig)

## Testing

In order to run the integration test that works against AWS you to setup the following
envars so the test can pick up the right resources to execute correctly:

CONCORD_TMP_DIR	 = /tmp/concord <br>
AWS_ACCESS_KEY	 = [your_aws_access_key] <br>
AWS_SECRET_KEY	 = [your_aws_secret_key] <br>
TF_TEST_FILE	    = /path/to/your/main.tf <br>
PRIVATE_KEY_PATH = /path/to/your/<aws_pem_file>

Alternatively, you can setup the following:

1) Create an `~/.aws/credentials` file with a `concord-integration-tests` stanza. The access key id and secret key will
be taken from this stanza:

```
[default]
aws_access_key_id=xxx
aws_secret_access_key=xxx

[concord-integration-tests]
aws_access_key_id=xxx
aws_secret_access_key=xxx
```

2) Create a keypair in AWS called `concord-integration-tests` and place the downloaded PEM file here: `~/.aws/concord-integration-tests.pem`. This will be used as the private key for the integration test.

The Terraform file used for the test will default the `src/test/terraform/main.tf` and the value for the `CONCORD_TMP_DIR` envar will be set for you if it is not present.
