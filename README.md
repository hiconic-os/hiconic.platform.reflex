# The Hiconic Reflex Platform

Reflex is a lightweight, modular, microservices platform.

[See full documentation](https://hiconic-os.github.io/hiconic.platform.reflex/)

## Application artifact identity

An explicitly launched RX application includes `META-INF/rx-application-artifact.marker` in its own artifact. The platform resolves the marker's originating artifact descriptor and exposes its `ArtifactReflection` through `RxApplicationContract.applicationArtifactReflection()`. Applications assembled from a synthetic wrapper instead set the literal `reflex.application.artifact: "groupId:artifactId"` in merged `conf/properties.yaml`; the platform finds the matching packaged artifact descriptor. The wrapper itself is never the application identity. An embedded application with neither marker nor property has no artifact reflection; an ambiguous or conflicting selection fails.

The RX web server uses `groupId--artifactId/version` as the HTTP `Server` header unless `WebServerConfiguration.serverHeader` is configured. An embedded application without artifact identity uses its `applicationId`. This artifact identity is separate from the runtime `applicationId` used for cluster identity.




## Date encoding for HTTP parameters

Date values in path, query and header parameters, and textual multipart parts, can
be formatted per mapping without changing JSON request or response marshalling:

```java
@HttpPathParam(dateFormat = "yyyy-MM-dd", dateZone = "Europe/Berlin")
Date getStartDate();
```

The same optional attributes are available on `HttpQueryParam`, `HttpHeaderParam`
and `HttpMultipartTextPart`. The annotation attributes map directly to the
`HttpDateEncoding` aspect of the corresponding metadata via `META-INF/gmf.mda`.
Collections apply the mapping to each element.

A local `dateFormat` uses `DateTimeFormatter` syntax and `Locale.ROOT`.
`dateZone` selects the zone used to convert the instant into local fields,
including when the pattern emits an offset. If omitted, the JVM zone is used.
A zone without a local format, an invalid zone or an invalid format is reported
as a `ConfigurationError` Reason.

Without a local format, the existing request-level `HttpDateFormatting` is the
compatibility fallback; parameter conversion now honors its default zone and
locale. Without either configuration, dates retain epoch-millisecond encoding.
The existing request-level settings continue to configure body serialization and
response deserialization; per-parameter settings do not alter those settings.
