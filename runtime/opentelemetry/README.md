# opentelemetry

A plugin for Concord runtime-v2 that adds tracing capabilities using OpenTelemetry.
Process traces are collected after the process finishes and are sent to a configured collector.

## Usage

To use the plugin, add the following dependency to your Concord process:

```yaml
configuration:
  dependencies:
    - mvn://com.walmartlabs.concord.plugins:opentelemetry:<VERSION>
```

The plugin is configured using `defaultTaskVariables`. For example, using
[project-level configuration](https://concord.walmartlabs.com/docs/getting-started/projects.html#configuration):

```yaml

```json
{
    "defaultTaskVariables": {
        "opentelemetry": {
            "enabled": true,
            "endpoint": "http://localhost:4318/v1/traces"
        }
    }
}
```

The `endpoint` is the address of the OpenTelemetry collector that will receive the traces.

Once the plugin is configured, the traces will be sent to the collector after any process finishes (or fails).
