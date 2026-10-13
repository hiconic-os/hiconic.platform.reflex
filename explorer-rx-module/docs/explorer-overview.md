# Structure

## Terminal App

Required local files:
```
web-app/
    tribefire-explorer/ (the entire app)
        TODO there is no way to configure RX services URL
    tribefire-services/webpages/
        currently copied from cortex base tribefire-services
        TODO remove images (and others) that are not needed
```

## Explorer-specific Handlers

### Static Resources

| URL path | File System Path |
| --- | --- |
| /tribefire-explorer | web-app/tribefire-explorer |
| /tribefire-services/webpages | web-app/tribefire-services/webpages |


### Servlets

| path | Description |
| --- | --- |
| /tribefire-explorer/symbolTranslation | `SymbolTranslationServlet`| 
| /tribefire-services | `AliveServlet` - explorer checks if server is running|
| /tribefire-services/login | `LoginRxServlet` | 
| /tribefire-services/login/auth | `AuthRxServlet` |

### Configuration

| Property | Description |
| --- | --- |
| `TRIBEFIRE_EXPLORER_URL` | URL of the explorer, used by the links on the home page and passed to the explorer in its runtime configuration. Default for the home page links: `/tribefire-explorer`. Set it if a reverse proxy serves the explorer under a different path, e.g. `/explorer`. |

### Generic Endpoints

|Name| RX module | URL path | Description |
| --- | --- | --- | --- |
| Web API | `web-api-server-rx-module` | /tribefire-services/api/* |
| Web RPC | `web-rpc-server-rx-module` | /tribefire-services/rpc/* |
| Websocket | `websocket-server-rx-module` | /tribefire-services/websocket/* |

### Service Domains

* explorer (`explorer-rx-module`)
    * `AvailableAccessesRequest` (AvailableAccessesProcessor)
        * returns `HardwiredAccess` instances
    * `CurrentUserInformationRequest` (CurrentUserInformationProcessor)
        * based on UserSessionAspect

* persistence (`access-rx-module`)
    * `PersistenceReflectionRequest` (PersistenceReflectionProcessor)
        * `GetModelEnvironment`
        * `GetModelAndWorkbenchEnvironment`
        * `GetMetaModelForTypes`

* system (`reflex-platform`)
    * `CompositeRequest`

### Accesses

* cortex (explorer-rx-module)
    * type: `ReadOnlySmoodAccess`
    * Currently just reflects available accesses



