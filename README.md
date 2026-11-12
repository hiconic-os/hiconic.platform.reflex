# The Hiconic Reflex Platform

Reflex is a lightweight, modular, microservices platform.

[See full documentation](https://hiconic-os.github.io/hiconic.platform.reflex/)

## Application artifact identity

An explicitly launched RX application includes `META-INF/rx-application-artifact.marker` in its own artifact. The platform resolves the marker's originating artifact descriptor and exposes its `ArtifactReflection` through `RxApplicationContract.applicationArtifactReflection()`. Applications assembled from a synthetic wrapper instead set the literal `reflex.application.artifact: "groupId:artifactId"` in merged `conf/properties.yaml`; the platform finds the matching packaged artifact descriptor. The wrapper itself is never the application identity. A missing or ambiguous selection fails when the reflection is requested.

The RX web server uses `groupId--artifactId/version` as the HTTP `Server` header unless `WebServerConfiguration.serverHeader` is configured. This artifact identity is separate from the runtime `applicationId` used for cluster identity.



