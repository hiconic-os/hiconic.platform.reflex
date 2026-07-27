# External tools

The external-tools module makes native command-line programs explicit,
configurable application dependencies. Consumers depend on the neutral
`com.braintribe.systemtools:system-tools-api`; they do not know whether a tool
runs in the application process environment or in a container.

## API

`ExternalToolRegistry` resolves named tools:

- `required(id)` fails while wiring if no mapping exists.
- `optional(id)` returns an unavailable tool when no mapping exists, allowing a
  feature to remain disabled.
- `descriptors()` reflects configured tools and their backends.

For one tool invocation, open a tool-bound execution:

```java
try (ToolExecution execution = tools.required("ghostscript").openExecution()) {
    Path input = execution.workspace().resolve("input.pdf");
    Path output = execution.workspace().resolve("output.pdf");
    // Write input, execute, and consume output before closing the scope.
}
```

For a sequence involving several tools, open one
`ToolExecutionEnvironment.openScope()` and execute all tools inside that scope.

## File-system lifecycle

The configured `fileSystemRoot` contains two deliberately different spaces:

- `workspaces/<execution-id>` belongs to an execution scope and is removed on
  close. Cleanup is exception-safe and does not mask the original failure.
- `external/<space-id>` is externally managed. The tool layer creates the
  directory but never applies a lifecycle policy to its contents. It may hold
  caches, durable outputs, or inputs owned by another subsystem.

Input and output are therefore not lifecycle categories. Either may live in a
scope-owned or externally managed space.

## RX configuration

Applications include `external-tools-rx-module` and contribute one
`ExternalToolsConfiguration`.

Local execution:

```yaml
fileSystemRoot: "${reflex.app.dir}/data/tools"
backend: !hiconic.rx.tools.model.configuration.LocalToolExecutionBackend {}
tools:
- id: ghostscript
  command: gs
```

Docker Compose execution:

```yaml
fileSystemRoot: "${reflex.app.dir}/data/tools"
backend: !hiconic.rx.tools.model.configuration.DockerComposeToolExecutionBackend
  composeFile: "${reflex.app.dir}/toolbox/compose.yaml"
  projectDirectory: "${reflex.app.dir}/toolbox"
  service: tools
  containerFileSystemRoot: /tool-fs
tools:
- id: ghostscript
  command: gs
```

The Compose service bind-mounts the host `fileSystemRoot` at
`containerFileSystemRoot`. The backend projects workspace paths in arguments
and the working directory into that container path and invokes
`docker compose exec -T`. Thus an application can run and debug directly in an
IDE while native tools remain isolated in a reproducible toolbox image.

The container must already be running. Its startup remains an application
composition concern, not a hidden side effect of opening a tool.

## CX compatibility

CX consumers may continue constructing the same processor with the existing
`CommandExecution` and environment-variable/path lookup. Migrated processors
prefer an injected `ExternalTool`, while their legacy setters and execution
branches remain intact. The new RX configuration and Docker backend therefore
do not alter CX wiring or startup.

## Reproducibility boundary

The modeled mapping makes tool identity, backend, command, and shared
file-system boundary inspectable. Full byte-for-byte reproducibility additionally
requires publishing the toolbox image under an immutable version or digest.
Mutable image tags and unpinned operating-system package repositories must not
be treated as a final production lock.
