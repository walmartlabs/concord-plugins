# codecoverage

A plugin for Concord runtime-v2 that adds code coverage capabilities.

## Usage

To use the plugin, add the following dependency to your Concord process:

```yaml
configuration:
  dependencies:
    - mvn://com.walmartlabs.concord.plugins:codecoverage:<VERSION>
```

## Generating HTML report with LCOV

The plugin produces a file in [the LCOV format](https://github.com/linux-test-project/lcov).

1. Download coverage info: `/api/v1/process/${INSTANCE_ID}/attachment/coverage.info`
2. Download and unzip process flows: `/api/v1/process/${INSTANCE_ID}/attachment/flows.zip`
3. Generate HTML with: `genhtml "coverage.info" --output-directory "html"`
