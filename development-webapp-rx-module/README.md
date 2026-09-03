# Development web application RX module

This optional module makes web applications contributed by the active runtime
classpath available when an RX application is started directly from an IDE.
It deliberately follows the IDE's effective classpath instead of an application
distribution assembled by a separate build.

Applications opt in by depending on this module. At deployment time it reads all
`HICONIC-CONF/webapp-dependencies.properties` resources, downloads the declared
archive parts through `jinni download-artifacts`, extracts them into
`<reflex.app.dir>/build/development-runtime/web-apps`, and registers the extracted
directories with the web server. The declaration format is the same one used by
the RX application assembler, for example:

```properties
tribefire.app.explorer:tribefire-explorer#[3.0,3.1)/war=/explorer;welcome=ClientEntryPointRx.html
```

The content-addressed cache is reused across launches. Set
`RX_DEVELOPMENT_WEBAPPS_REFRESH=true` to rebuild it, or
`RX_DEVELOPMENT_WEBAPPS_DISABLED=true` to disable the feature explicitly.

An assembled application contains `packaged-solutions.txt`; in that environment
the module remains inactive because its web applications have already been
materialized by the application build.
