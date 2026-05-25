# japicmp MCP Server

A [Model Context Protocol (MCP)](https://modelcontextprotocol.io/) server that exposes [japicmp](https://github.com/siom79/japicmp) as an AI tool. Connect it to Claude Desktop (or any MCP-compatible client) and ask your AI assistant to compare the APIs of any two Maven artifact versions — directly from Maven Central.

## What it does

The server provides two tools:

- **`compareJavaApi`** — downloads two JAR versions from Maven Central and compares their APIs
- **`compareLocalJars`** — compares two local JAR files on disk

Both run a full japicmp comparison and return a Markdown compatibility report with semver classification, a summary table, and detailed per-class breakdowns.

### Example

> "Compare `com.github.siom79.japicmp:japicmp` version 0.25.0 with 0.26.0 using japicmp"

```
Here are the results of comparing japicmp 0.25.0 → 0.26.0:

  ---
  Verdict: MAJOR (breaking changes)
  
  The release is not binary-compatible due to changes in japicmp.cmp.JApiCmpArchive.

  ---
  Breaking changes in JApiCmpArchive
  
  Three methods changed their return types to Optional<T>:

  ┌────────────┬─────────────────┬──────────────────┐
  │   Method   │ Old return type │ New return type  │
  ├────────────┼─────────────────┼──────────────────┤
  │ getBytes() │ byte[]          │ Optional         │
  ├────────────┼─────────────────┼──────────────────┤
  │ getFile()  │ File            │ Optional<File>   │
  ├────────────┼─────────────────┼──────────────────┤
  │ getName()  │ String          │ Optional<String> │
  └────────────┴─────────────────┴──────────────────┘

  These are binary and source incompatible — any code calling these methods needs to be updated to unwrap the Optional.

  ---
  Other modified classes (no compatibility changes)
  
  The following classes were modified but remain fully binary-, source-, and serialization-compatible:

  - japicmp.config.Options — field N_A modifier changed to public static final
  - japicmp.model.JApiBehavior
  - japicmp.model.JApiClass
  - japicmp.model.JApiField

  ---
  Summary: The only breaking change is the JApiCmpArchive return type wrapping in Optional. If you use that class directly, update call sites to handle Optional
  results.                                     |
```

The report identifies that `JApiCmpArchive.getFile()`, `getBytes()`, and `getName()` changed their return types to `Optional<T>` — a **binary-incompatible** change requiring a semver MAJOR bump.

## Prerequisites

- Java 25+
- Maven 3.9+

## Build

```bash
mvn package
```

This produces `target/japicmp-mcp-server-0.0.2-SNAPSHOT.jar`.

## Integration with Claude

Add the server to your `.mcp.json`:

```json
{
  "mcpServers": {
    "japicmp": {
      "command": "java",
      "args": ["-jar", "/path/to/japicmp-mcp-server-0.0.2-SNAPSHOT.jar"]
    }
  }
}
```

Then restart your CLI. Both tools will be available automatically.

## Tool reference

### `compareJavaApi`

Compares two Maven artifacts for API compatibility using japicmp. Returns a Markdown report.

| Parameter | Type | Description |
|-----------|------|-------------|
| `oldGroupId` | string | GroupId of the old artifact (e.g. `com.example`) |
| `oldArtifactId` | string | ArtifactId of the old artifact |
| `oldVersion` | string | Version of the old artifact (e.g. `1.0.0`) |
| `newGroupId` | string | GroupId of the new artifact |
| `newArtifactId` | string | ArtifactId of the new artifact |
| `newVersion` | string | Version of the new artifact |
| `onlyModified` | boolean | Only include modified API elements in the report |
| `onlyBinaryIncompatible` | boolean | Only include binary-incompatible changes in the report |

Both artifacts must be available on Maven Central.

### `compareLocalJars`

Compares two local JAR files for API compatibility using japicmp. Returns a Markdown report.

| Parameter | Type | Description |
|-----------|------|-------------|
| `oldJarPath` | string | Absolute path to the old JAR file |
| `newJarPath` | string | Absolute path to the new JAR file |
| `onlyModified` | boolean | Only include modified API elements in the report |
| `onlyBinaryIncompatible` | boolean | Only include binary-incompatible changes in the report |

Both paths must be absolute and point to readable `.jar` files.

### Example

> "Compare `/home/user/Downloads/japicmp-0.24.0.jar` with `/home/user/Downloads/japicmp-0.25.0.jar` using japicmp"

```
## Compatibility Report: japicmp 0.24.0 → 0.25.0

Verdict: MAJOR (breaking changes)

Breaking change in japicmp.compat.CompatibilityChanges:
  Removed: CompatibilityChanges(JarArchiveComparator)
  Added:   CompatibilityChanges(JarArchiveComparator, JarArchiveComparatorOptions)

New method in japicmp.util.FileHelper (compatible):
  Added: static public String guessVersion(File)
```

## Technology stack

| Component | Version |
|-----------|---------|
| Java | 25 |
| Spring Boot | 4.0.6 |
| Spring AI MCP Server | 1.0.0 |
| japicmp | 0.26.0 |
| Transport | stdio |

## License

Same as the parent [japicmpweb](https://github.com/siom79/japicmp) project.
