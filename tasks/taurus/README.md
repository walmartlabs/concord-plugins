# Taurus Task

Concord plugin for executing automated tests with Taurus.

## Tests

The included tests requires `bzt` to be available on the host executing the tests. See the
[docs](https://gettaurus.org/install/Installation/) for installation.

The included tests depend on a local concord server running on the default port (8001). This can be overridden to any
other HTTP endpoint with a `TAURUS_TEST_API_ENDPOINT` environment variable for testing (be considerate, keep it local!). 

```shell
$ export TAURUS_TEST_API_ENDPOINT=http://someOtherConcord:8001/api/v1/server/ping
$ ./mvnw -pl tasks/taurus clean install -DskipTests=false

...

[INFO] Results:
[INFO] 
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
```
