# Akeyless plugin for Concord

Read and write secret data to [Akeyless](https://www.akeyless.io).

## Public Akeyless Account Setup

**NOTE: these opensource instructions are for developers working on the plugin
code in this repo. Enterprise users should refer to their organizations support
instructions for account information**

The included tests rely on auth info for a "real" account on the default public
Akeyless instance ([https://api.akeyless.io](https://api.akeyless.io)). Assuming
an account is available, follow the rest of these instructions.

First create a Role for the access. In the Akeyless UI, go to
[Access Roles](https://console.akeyless.io/access-roles) and click the
`New` button. Enter a name and optional path (default is `/`).

Next, generate an API key. In the Akeyless UI, go to
[Auth Methods](https://console.akeyless.io/auth-methods) and click the `New`
button. Select `API Key` option and enter the desired details (name and TTL are
required). Click the `Save` button and the actual API token value will be displayed.
**This value cannot be recovered. It can only be reset.** Note the value somewhere
secure (e.g. password manager).

Assign the auth method to one or more roles. Return to
[Access Roles](https://console.akeyless.io/access-roles) and click the role to
assign to the new auth method. In the first section, click the `Associate`
button. Select the auth method and click `Save`.

Validate. Now you can use the Akeyless CLI or the integration tests to validate
the credentials work.

```
$ akeyless auth --access-id
Access Id: <enter new api key id>
Access Key: <enter new api key value>
Authentication succeeded.

Token: <actual token will be shown>

$ akeyless list-items --token '<token_value>' --path /
```

## Integration Tests

The integration test are ignored by default. Remove the `@Ignore` annotation,
or just run them from and IDE explicitly. The tests read the required external
data from a properties file specified by an environment variable `IT_PROPERTIES_FILE`

```properties
apiBasePath=https://api.akeyless.io
accessId=
accessKey=
```

To run with maven, provide the environment variable and the `-DskipAkeylessITs=false`
property

```
$ IT_PROPERTIES_FILE=~/my_it_props/akeyless.props mvn -DskipAkeylessITs=false clean install
# or 
$ IT_PROPERTIES_FILE=~/my_it_props/akeyless.props mvn -DskipAkeylessITs=false failsafe:integration-test

[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  01:03 min
[INFO] Finished at: 2022-08-31T11:02:00-05:00
[INFO] ------------------------------------------------------------------------
```

## Testing via Payload

**This should really only be done against a local Concord dev instance since the
payload is quite large. Use discretion when choosing a testing environment.**

Use the included `testPayload.sh` script to execute a test flow with the plugin.

```
$ SERVER_URL=http://localhost:8001 \
  RUNTIME=v1 \
  ACTIVE_PROFILES=apikey \
  ./testPayload.sh

$ SERVER_URL=http://localhost:8001 \
  RUNTIME=v2 \
  ACTIVE_PROFILES=ldap \
  SECRET_BASE_PATH=/custom/base/path \
  API_BASE_PATH=https://my-akeyless-instance:1234/v2 \
  ./testPayload.sh
```

Remote JVM debugging can be enabled with the `DEBUG=true` environment variable if
the server is local.

## Testing via Concord CLI

The included `testV2.yml` is compatible with [Concord CLI](https://concord.walmartlabs.com/docs/cli)
for testing locally.
[Create the required local secrets](https://concord.walmartlabs.com/docs/cli/running-flows.html#secrets),
ensure the version matches the local snapshot version, then run:

```
$ mvn clean install

# apikey profile with default api endpoint is api.akeyless.io
$ concord run --profile apikey testV2.yml

Running a single Concord file: .../concord-plugins/tasks/akeyless/testV2.yml
Starting...
16:31:34.943 [main] DEBUG: Action: CREATESECRET

# customize api endpoint and use ldap auth profile
$ concord run --profile ldap \
    -e "secretBasePath=/custom/base/path" \
    -e "akeylessParams.apiBasePath=https://..." \
    testV2.yml

Running a single Concord file: .../concord-plugins/tasks/akeyless/testV2.yml
Starting...
16:31:34.943 [main] DEBUG: Action: CREATESECRET
```
