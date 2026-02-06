# Concord Plugin: `docs`

The `docs` plugin generates Markdown documentation from Concord YAML flows.  
It extracts flow descriptions, input/output parameters, 
and produces a [mdBook](https://rust-lang.github.io/mdBook/) 
compatible structure - ready for publishing.

## Usage

To use this plugin in your Concord flow:

```yaml
flows:
  generateDocs:
    - task: docs
      in:
        output: "${workDir}/docs"
        bookTitle: "My Project Documentation"
```

Or define it via a separate Concord profile:

```yaml
profiles:
  docs:
    configuration:
      extraDependencies:
        - "mvn://com.walmartlabs.concord.plugins:docs-task:<latest-version>"
    flows:
      renderFlowDocs:
        - task: docs
          in:
            bookTitle: "My Project Documentation"
            output: "${workDir}/docs"
```

## Task input parameters

| Input Key                  | Type    | Required | Description                                                                             |
|----------------------------|---------|----------|-----------------------------------------------------------------------------------------|
| `output`                   | string  | ✓        | Output directory for generated files                                                    |
| `bookTitle`                | string  | ✓        | Title used in the `book.toml` metadata                                                  |
| `includeUndocumentedFlows` | boolean |          | Generate docs for flows without description (default: true )                            |
| `sourceBaseUrl`            | string  |          | Base URL for linking to flow source files (e.g., https://github.com/ORG/REPO/blob/main) |
| `flowsDir`                 | string  |          | Directory containing external Concord flows                                             |

> **Note:** If `flowsDir` is specified, flow imports will not be resolved during documentation generation.

## Example Output

- `src/**/*.md`: flow documentation
- `src/SUMMARY.md`: mdBook-style summary with structure
- `book.toml`: book metadata file
- `flows.json`: flow descriptions in JSON format

```
docs/
├── book.toml
└── src/
    ├── my-flow.yaml.md
    ├── another-flow.yaml.md
    └── SUMMARY.md
```

## Flow description format

Flow descriptions are parsed from comments above each flow definition using the following format:

```yaml
##  
#  <FLOW_DESCRIPTION>  
#  in:  
#    myParam: <PARAM_TYPE>, mandatory|optional, <PARAM_DESCRIPTION>  
#  out:  
#    myParam: <PARAM_TYPE>, mandatory|optional, <PARAM_DESCRIPTION>  
## 
```

Where:

`<FLOW_DESCRIPTION>`:A human-readable description of the flow (optional).

`<PARAM_TYPE>`:
- Basic types: string | int | number | boolean | object | date | any
- Arrays: string[] | int[] | number[] | boolean[] | object[] | date[] | any[]
  *int === number, just an alias*
- Custom types: any string value

`<PARAM_DESCRIPTION>`: Description of the parameter (optional).

### Describing Objects

You can describe nested object parameters using dot notation:

```yaml
##  
#  in:  
#    objectParam: object, mandatory|optional, <PARAM_DESCRIPTION>  
#    objectParam.key1: <PARAM_TYPE>, mandatory|optional, <PARAM_DESCRIPTION>  
#    objectParam.key2: <PARAM_TYPE>, mandatory|optional, <PARAM_DESCRIPTION>  
##  
```
