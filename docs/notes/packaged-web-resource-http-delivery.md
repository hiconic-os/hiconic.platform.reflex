# Packaged web resource HTTP delivery

## Current state

Static web applications backed by files are served by Undertow's `ResourceHandler` with a `FileResourceManager`. The configured transfer threshold is `100` bytes; it is not a buffer size. Above that threshold Undertow can use its file-channel transfer path. Undertow also supplies the usual static-resource HTTP semantics, including range handling.

Indexed packaged web resources currently take a separate path through `PackagedWebResourceServlet`:

- resource lookup uses `RxPackagedResourceResolver` and its inventory;
- the response includes MIME type, content length and an MD5-based ETag when available;
- `If-None-Match` and `HEAD` are handled;
- the body is copied with `InputStream.transferTo`;
- range requests are not handled;
- this path cannot use Undertow's file-transfer optimization.

The servlet is memory-bounded and adequate for small resources, but the two delivery paths have observably different HTTP behavior and can evolve independently.

## Proposed harmonization

Expose packaged resources to the same Undertow `ResourceHandler` through adapters:

- `PackagedResourceManager implements ResourceManager`
- `PackagedResource implements io.undertow.server.handlers.resource.Resource`

The adapters should map the packaged-resource information as follows:

| Undertow concern | Packaged-resource source |
| --- | --- |
| existence and path lookup | `RxPackagedResourceInventory` |
| content length | `Resource.fileSize` |
| content type | `Resource.mimeType` |
| ETag | `Resource.md5` |
| content | `Resource.openStream()` |

`PackagedResource` should implement both full and ranged serving. A ranged stream must start at the requested offset and be bounded to the requested length. Blocking source access must not run on Undertow's I/O thread.

## Transfer strategy

Use a hybrid implementation rather than forcing all packaged resources through one physical mechanism:

1. If the resolved resource has a real filesystem path, delegate to Undertow's filesystem resource implementation. This retains file-channel/zero-copy transfer where possible.
2. For JAR, classpath or other stream-backed resources, transfer through Undertow's shared byte-buffer pool without materializing the whole resource in memory.
3. Range requests use a newly opened, positioned and bounded stream when no seekable channel is available.

The HTTP contract is therefore uniform while the physical transfer remains appropriate to the backing source.

## Intended result

`addStaticFileResource` and `addPackagedWebResources` both register a `ResourceHandler`; only their `ResourceManager` differs. `PackagedWebResourceServlet` can then be removed, avoiding two independent implementations of static HTTP delivery.

## Required tests

Exercise the same contract against filesystem-backed and packaged resources:

- `GET`, `HEAD` and missing resources;
- MIME type and content length;
- ETag and `If-None-Match`/`304`;
- bounded, open-ended and suffix byte ranges;
- invalid or unsatisfiable ranges and `416`;
- empty and large resources;
- bounded memory use while transferring a large stream;
- closing resources after success, failure and client disconnect;
- normalized paths and rejection of traversal attempts;
- preservation of the optimized file-transfer path for filesystem-backed resources.

This is a deferred improvement. It is not required for the current OS Test investigation.
