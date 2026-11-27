# HTTP response compression

RX negotiates gzip through Undertow for servlet, Web API, Web RPC and static responses. Clients advertise `Accept-Encoding: gzip`; clients needing the original transport representation can request `identity`. Application resources and their checksums still describe the uncompressed content.

Compression is enabled without configuration. Optional `WebServerConfiguration.responseCompression` overrides use the `HttpResponseCompression` model:

| Property | Default | Meaning |
| --- | --- | --- |
| `enabled` | `true` | Disable with `false`. |
| `minimumResponseBytes` | `2048` | Minimum **known** uncompressed length, inclusive; zero disables this size threshold. |
| `level` | `1` | Gzip level 0–9; the default favors throughput over maximum compression. |
| `mimeTypes` | Built-in allowlist | Nonempty sets replace the default. Patterns support `type/*` and `type/*+suffix`. |

The built-in allowlist covers text, JSON and XML (including structured suffixes), SVG, JavaScript, YAML, `gm/*`, `multipart/chunked` and `multipart/form-data`.

Unknown-length eligible responses compress immediately, retaining streaming and flush behavior. They are not buffered until they cross the size threshold. Consequently even a small unknown-length response may be compressed. Multipart RPC transfers benefit when carrying models or POMs; batches dominated by already compressed archives can request `identity` to avoid unnecessary CPU work.

Already encoded responses, byte-range requests/responses, `Cache-Control: no-transform`, server-sent events, empty responses and bodyless statuses are excluded. Binary archives and images are excluded by the default MIME allowlist. Configured allowlists cannot override the transport exclusions.

Responses vary on `Accept-Encoding`, preserving existing `Vary` entries. Strong source ETags become weak on compressed representations; identity ETags remain unchanged. HEAD responses carry the negotiated encoding without incorrectly advertising the uncompressed length. Undertow handles streaming, compressed length/chunking and encoding quality values.

The CX servlet implementation buffers up to 16 KiB before choosing compression. RX uses Undertow's native encoding conduit instead, avoiding that extra latency and retaining Undertow's flow control.

Real HTTP tests in `web-endpoints-rx-module-test` cover negotiation, thresholds, headers, exclusions, incremental flushes and a compressed Web API round trip.
