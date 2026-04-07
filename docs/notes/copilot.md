## 1. Foundational Technologies

### 1.1 Generic Model (GM)

The entire Reflex platform is built on top of the **Generic Model** (GM) technology (artifacts from `com.braintribe.gm`). GM provides:

- **Interface-based POJO entities**: Entity types are declared as Java interfaces extending `GenericEntity`. They are automatically implemented at runtime by the GM type system. Each entity type carries a static type literal `T` of type `EntityType<T>` for reflection access.
- **Enum types** implementing `EnumBase` with a corresponding `EnumType T` literal.
- **Reflection**: Full runtime type information — properties, types, models — is available through `EntityType`, `Property`, `Model`, and related reflection APIs.
- **GmMetaModel**: A reflective model-of-models. A `GmMetaModel` instance describes all entity types, enum types, and properties of a model. Models can declare dependencies on other models, forming a model dependency graph.
- **Metadata**: Arbitrary metadata can be attached to entity types, properties, and models. Metadata is resolved contextually (e.g., by user roles) through `CmdResolver` (Cascading MetaData Resolver).
- **Evaluable requests**: `ServiceRequest` subtypes can declare an `eval(Evaluator)` method that types the response, enabling location-transparent evaluation.
- **Marshalling**: GM entities can be serialized/deserialized to JSON, YAML, XML, binary, and other formats through a `MarshallerRegistry`.

#### Example: Declaring a service request

```java
public interface Greet extends ServiceRequest {
    EntityType<Greet> T = EntityTypes.T(Greet.class);

    @Mandatory
    String getName();
    void setName(String name);

    @Override
    EvalContext<String> eval(Evaluator<ServiceRequest> evaluator);
}
```

### 1.2 Wire IOC Framework

**Wire** is a lightweight Inversion-of-Control (IOC) / dependency injection framework that Reflex uses instead of Spring's annotation scanning:

- **Contracts**: Interfaces extending `WireSpace` that declare bean methods. Contracts define _what_ is available — they are the API surface.
- **Spaces**: Classes annotated with `@Managed` that implement contracts. Spaces define _how_ beans are created and wired. Methods annotated with `@Managed` return singleton beans managed by the Wire context.
- **Imports**: Fields annotated with `@Import` inject other spaces/contracts, enabling cross-space dependency resolution.
- **WireContext**: A container that creates and manages spaces. Beans are lazily instantiated on first access and singleton-scoped by default.
- **WireModule**: Configures a `WireContext` — binds contracts to implementing spaces and sets up context-level configuration.

Benefits of this explicit wiring approach:

- No classpath scanning or annotation magic
- Full IDE navigability — contracts are regular Java interfaces
- Clear, traceable dependency chains
- Controlled lifecycle (pre/post construct, destroy)

---

## 2. Platform Architecture

### 2.1 Component Types

A Reflex application is composed of four distinct component types:

| Component | Artifact Suffix | Purpose |
|---|---|---|
| **Model** | `-model` | Declares entity types and enums (API models, data models, configuration models, metadata models) |
| **Module** | `-rx-module` | Contains processing logic, registers service processors, interceptors, and platform extensions |
| **Module API** | `-module-api` | Declares `RxExportContract` interfaces for inter-module communication |
| **Application** | `-app` | Aggregates modules and models into a deployable bundle |

### 2.2 Bootstrap and Lifecycle

The platform bootstrap is orchestrated by `RxPlatform`, which:

1. **Sets up logging** (Logback + SLF4J bridge for JUL).
2. **Creates the platform WireContext** (`RxPlatformWireModule`) which binds all platform contracts to their implementing spaces.
3. **Discovers and loads modules** (via `RxModuleLoader`):
   - Scans `META-INF/rx-module.properties` files on the classpath.
   - Analyzes module dependencies (export/import graph).
   - Creates child `WireContext` per module, sorted dependency-first.
4. **Runs the module configuration lifecycle** (4 rounds — see [Section 3.2](#32-rxmodulecontract--the-module-lifecycle-interface)).
5. **Reports startup completion** — lists loaded service domains and startup duration.
6. **Waits for shutdown signal** (for server-mode applications).

On shutdown, `onApplicationShutdown()` is called on each module, and all Wire contexts are closed.

---

## 3. Modules

### 3.1 Module Structure

Every Reflex module consists of:

1. **An `RxModule` enum** — a singleton implementing `RxModule<M>` (where `M` is the module's space) that serves as the module entry point.
2. **A module space** — a `@Managed` class implementing `RxModuleContract`, which contains all bean definitions and registration logic.
3. **A registration file** — `META-INF/rx-module.properties` with a `wire-module` key pointing to the enum class.
4. **Processing classes** — service processors, interceptors, and other logic.

#### Minimal example

```java
// Wire Module
public enum MyRxModule implements RxModule<MyRxModuleSpace> {
    INSTANCE;
}

// Module Space
@Managed
public class MyRxModuleSpace implements RxModuleContract {
    @Override
    public void configureMainServiceDomain(ServiceDomainConfiguration configuration) {
        configuration.bindRequest(MyRequest.T, this::myProcessor);
    }

    @Managed
    private MyProcessor myProcessor() {
        return new MyProcessor();
    }
}
```

```properties
# META-INF/rx-module.properties
wire-module=com.example.wire.MyRxModule
```

### 3.2 RxModuleContract — The Module Lifecycle Interface

`RxModuleContract` is the central interface every module must implement. The platform calls its methods in a strict 4-round lifecycle:

#### Round 1 — Model Configuration

| Method | Purpose |
|---|---|
| `configureMainPersistenceModel(ModelConfiguration)` | Add models/metadata to the shared main persistence model |
| `configureModels(ModelConfigurations)` | Configure arbitrary named/referenced models |

#### Round 2 — Service and Platform Configuration

| Method | Purpose |
|---|---|
| `configureMainServiceDomain(ServiceDomainConfiguration)` | Bind processors/interceptors to the main service domain |
| `configureServiceDomains(ServiceDomainConfigurations)` | Configure arbitrary service domains |
| `registerCrossDomainInterceptors(InterceptorRegistry)` | Register interceptors that apply across all domains |
| `registerFallbackProcessors(ProcessorRegistry)` | Register processors used when no domain-specific one matches |
| `configurePlatform(RxPlatformConfigurator)` | Extend/override core platform services (marshallers, worker manager, resource storage experts) |

#### Round 3 — Deployment

| Method | Purpose |
|---|---|
| `onDeploy()` | Perform deployment-time initialization (e.g., deploy configured resources, register servlets) |

#### Round 4 — Completion

| Method | Purpose |
|---|---|
| `onApplicationReady()` | Called after all modules have deployed; application is fully ready |
| `onApplicationShutdown()` | Called during graceful shutdown |

All modules in the platform are iterated through each round before proceeding to the next round, ensuring a consistent global ordering.

### 3.4 Module Dependency Graph and Export/Import

Modules can define inter-module dependencies through the **Export/Import** mechanism:

#### Exporting a contract

A module exports functionality by:

1. Defining an interface extending `RxExportContract`:
   ```java
   public interface WebServerContract extends RxExportContract {
       void addServlet(String name, String path, HttpServlet servlet);
       // ...
   }
   ```

2. Binding the export in its `RxModule.bindExports()`:
   ```java
   public enum WebServerRxModule implements RxModule<WebServerRxModuleSpace> {
       INSTANCE;

       @Override
       public void bindExports(Exports exports) {
           exports.bind(WebServerContract.class, WebServerRxModuleSpace.class);
       }
   }
   ```

#### Importing a contract

Other modules import the exported contract with Wire's `@Import`:
```java
@Managed
public class MyModuleSpace implements RxModuleContract {
    @Import
    private WebServerContract webServer;

    @Override
    public void onDeploy() {
        webServer.addServlet("myServlet", "/api/*", myServlet());
    }
}
```

#### Dependency resolution

The platform analyzes all modules after discovery:

1. **Reflective scanning**: For each module, the platform scans `@Import` fields on its Wire spaces. Any field whose type is an interface extending `RxExportContract` creates an import dependency.
2. **Graph construction**: Import/export relationships form a directed dependency graph between module nodes.
3. **Cycle detection**: Dependency cycles are detected and rejected with an error.
4. **Topological ordering**: Modules are loaded in dependency order — a module's dependencies are always loaded before it.
5. **Cross-context resolution**: When Module A imports an `RxExportContract` that Module B exports, the platform's `RxExportResolver` transparently resolves the contract from Module B's WireContext into Module A's WireContext.

---

## 4. Service Domains

### 4.1 What is a Service Domain

A **Service Domain** is a named grouping of service requests with:

- A **unique identifier** (`domainId`)
- A **configured model** (`GmMetaModel`) — describes which request types belong to the domain and how they are configured with metadata
- **Bound processors** — mappings from request types to their processor implementations
- **Interceptors** — pre/around/post processing logic that wraps request execution
- A **dedicated evaluator** — for evaluating requests within the domain's context
- A **CmdResolver** — for resolving metadata within the domain's model context
- An optional **default request** — a request to execute when no specific request is supplied (useful for CLI applications)

Service domains are the fundamental organizational unit of the platform. They enable multiple logical services to coexist in a single application, each with its own model, processors, and metadata configuration.

### 4.2 Built-in Service Domains

The platform defines two built-in service domains:

| Domain | ID | Purpose |
|---|---|---|
| **main** | `main` | A convenience domain for simple applications. Modules typically bind their processors here. |
| **system** | `system` | For platform-level, domain-independent requests. The platform itself binds `CompositeRequest` and `UnicastRequest` processors here. |

Additional domains can be created dynamically by any module.

### 4.3 Configuring Service Domains

Modules configure service domains through `ServiceDomainConfiguration` (received in `configureMainServiceDomain` or `configureServiceDomains`):

```java
@Override
public void configureMainServiceDomain(ServiceDomainConfiguration configuration) {
    // Bind a request type to a processor
    configuration.bindRequest(ReverseText.T, this::reverseTextProcessor);

    // Add a model to this domain
    configuration.addModel(MyApiModel.reflection);

    // Bind an interceptor
    configuration.bindInterceptor("my-auth-interceptor")
        .forType(AuthorizedRequest.T)
        .bind(this::authInterceptor);
}
```

`ServiceDomainConfiguration` extends `ModelConfiguration`, so it supports both model composition and processor/interceptor binding. Each domain's model is built incrementally — multiple modules can contribute to the same domain.

### 4.4 Service Request Dispatch

When a `ServiceRequest` is evaluated, the platform dispatches it through a multi-layer chain:

1. **Root Service Processor**: The top-level dispatching processor receives all requests.
2. **Service Domain Dispatcher** (`RxServiceDomainDispatcher`): An interceptor on the root processor that resolves the target service domain:
   - If the request is a `DomainRequest` with an explicit `domainId`, that domain is used.
   - Otherwise, the dispatcher finds the unique domain whose model includes the request's type.
3. **Domain Evaluator**: The request is delegated to the resolved domain's evaluator.
4. **Request Dispatcher** (`RxRequestDispatcher`): Within the domain, the dispatcher:
   - Resolves the processor via `ProcessWith` metadata on the request type's model.
   - Builds an interception chain from `InterceptWith` metadata.
   - Executes the chain: pre-interceptors → around-interceptors → processor → post-interceptors.
5. **Fallback Processor**: If no domain-specific processor matches, the platform's fallback processor (configured by modules via `registerFallbackProcessors`) is tried.

---

## 5. Models

### 5.1 Model Types in the Platform

Models in Reflex serve several distinct purposes:

| Model Type | Artifact Naming | Purpose |
|---|---|---|
| **API Model** | `*-api-model` | Defines `ServiceRequest` subtypes that form a service's public API |
| **Data Model** | `*-model` (general) | Defines business entities used in persistence and data exchange |
| **Configuration Model** | `*-configuration-model` | Defines entity types for external configuration (loaded from YAML) |
| **Metadata Model** | `*-metadata-model` | Defines metadata types that can be attached to models for configuring behavior |

#### Examples from the platform:

- `check-api-model`: Defines `RunChecks`, `RunHealthChecks`, `CheckResponse` — health check service API
- `cli-api-model`: Defines `Help`, `Introduce`, `Options` — CLI-specific requests
- `db-configuration-model`: Defines `Database`, `DatabaseConfiguration` — database connection configuration
- `access-configuration-model`: Defines `Access`, `AccessConfiguration` — data access configuration
- `resource-storage-configuration-model`: Defines `ResourceStorage`, `FileSystemResourceStorage` — binary storage configuration
- `access-processing-metadata-model`: Defines `InterceptAccessWith`, `PreEnrichResourceWith` — access-level metadata

### 5.2 Configured Models

A **Configured Model** (`ConfiguredModel`) is a model that has been assembled and enriched during the platform's configuration phase. It is built incrementally:

1. Modules call `addModel()`, `addModelByName()`, or `addModel(ArtifactReflection)` on `ModelConfiguration` to compose the model from multiple base models.
2. Modules call `configureModel(Consumer<ModelMetaDataEditor>)` to attach metadata.
3. After all modules have configured, `finalizeModelConfiguration()` applies all metadata, locks the model, and builds the `ModelOracle`.

A configured model provides:

- **ModelOracle**: Type-safe navigation of the model's type hierarchy.
- **CmdResolver**: Contextual metadata resolution, potentially varying by user roles or other `AttributeContext` attributes.

Models can be referenced symbolically with `ModelSymbol`, allowing modules to refer to models by name without direct artifact dependencies.

### 5.3 Model Metadata and CmdResolver

The platform's metadata system (inherited from GM) allows arbitrary metadata to be attached at the model, entity type, property, or enum level. The `CmdResolver` (Cascading MetaData Resolver) resolves metadata contextually:

```java
ConfiguredModel model = platform.configuredModels().byName("my-model");
CmdResolver resolver = model.systemCmdResolver();

// Resolve metadata on a specific entity type
ProcessWith processWith = resolver.getMetaData()
    .entityType(MyRequest.T)
    .meta(ProcessWith.T)
    .exclusive();
```

The `CmdResolver` supports contextualization via `AttributeContext`, enabling role-based or session-based metadata variation. The platform provides:

- `systemCmdResolver()`: Resolved with the system's `AttributeContext` (internal/admin context).
- `contextCmdResolver()`: Resolved with the current thread's `AttributeContext` (e.g., the calling user's session).

---

## 6. Service Processing

### 6.1 Service Processors

A **Service Processor** handles a specific type of `ServiceRequest` and produces a response. Processors implement either:

- `ServiceProcessor<R, T>`: Standard processor with `T process(ServiceRequestContext, R)`.
- `ReasonedServiceProcessor<R, T>`: Returns `Maybe<T>` for structured error handling.

Processors are bound to request types within a service domain:

```java
configuration.bindRequest(MyRequest.T, this::myProcessor);
```

The platform supports two binding styles:
- **Direct binding**: One processor per request type.
- **Mapped binding** (`bindRequestMapped`): Uses `MappingServiceProcessor` for dispatching to sub-handlers based on request subtype.

### 6.2 Interceptors

**Interceptors** (`ServiceInterceptorProcessor`) wrap service request processing with cross-cutting concerns (authorization, logging, validation, etc.). They are configured with a fluent builder:

```java
configuration.bindInterceptor("my-logging-interceptor")
    .forType(ServiceRequest.T)          // applies to all requests
    .after("auth-interceptor")           // runs after auth
    .before("response-interceptor")      // runs before response processing
    .bind(this::loggingInterceptor);
```

Interceptors can be:

- **Domain-scoped**: Bound within a specific service domain configuration.
- **Cross-domain**: Registered via `registerCrossDomainInterceptors(InterceptorRegistry)` to apply across all domains.

The interceptor chain is resolved dynamically from `InterceptWith` metadata on the domain's model, allowing metadata-driven interception configuration.

### 6.3 Reasoned Processing (Maybe)

Reflex uses the `Maybe<T>` type (from `com.braintribe.gm`) for structured error handling instead of exceptions:

- `Maybe.complete(value)`: Successful result.
- `Reasons.build(ErrorType.T).text("message").toMaybe()`: A typed, structured error (a `Reason`).

`Reason` types are modeled entities (e.g., `InvalidArgument`, `IoError`, `ConfigurationError`), enabling:

- Machine-readable error classification
- Error aggregation and nesting
- Propagation through `maybe.propagateReason()`

```java
public class MyProcessor implements ReasonedServiceProcessor<MyRequest, MyResponse> {
    @Override
    public Maybe<? extends MyResponse> processReasoned(ServiceRequestContext ctx, MyRequest request) {
        if (request.getName() == null)
            return Reasons.build(InvalidArgument.T)
                .text("Name is required").toMaybe();

        return Maybe.complete(createResponse(request));
    }
}
```

### 6.4 Evaluators

An **Evaluator** (`Evaluator<ServiceRequest>`) is the primary interface for invoking service requests programmatically. The platform provides:

- `evaluator()`: Standard evaluator using the current thread's context.
- `systemEvaluator()`: Evaluator using the system/admin context.
- `evaluator(AttributeContext)`: Evaluator for a specific attribute context.

Each service domain also has its own scoped `evaluator()`.

Requests are evaluated fluently:

```java
String result = Greet.T.create()
    .name("World")
    .eval(evaluator)
    .get();
```

## 8. Configuration

### 8.1 Configuration Models

External configuration in Reflex is **modeled** — configuration entities are regular GM entity types declared in `*-configuration-model` artifacts. Examples:

```java
// Database configuration
public interface Database extends GenericEntity {
    @Mandatory String getName();
    @Mandatory String getUrl();
    String getUser();
    @Confidential String getPassword();
    @Initializer("3") Integer getMaxPoolSize();
    // ...
}

public interface DatabaseConfiguration extends GenericEntity {
    List<Database> getDatabases();
}
```

Modules read configuration via:
```java
@Import private RxPlatformContract platform;

@Override
public void onDeploy() {
    Maybe<DatabaseConfiguration> config = platform.readConfig(DatabaseConfiguration.T);
    // ...
}
```

This approach provides:
- **Type safety**: Configuration is validated against the model at load time.
- **IDE support**: Full autocompletion and refactoring on configuration types.
- **Documentation**: `@Description` and `@Mandatory` annotations document configuration requirements.
- **Defaults**: `@Initializer` provides default values.
- **Confidentiality**: `@Confidential` marks sensitive fields.

### 8.2 Properties and Property Resolution

The platform supports a layered property resolution system through dedicated contracts:

| Contract | Source |
|---|---|
| `RxPropertiesContract` | YAML config files + system properties + environment variables |
| `SystemPropertiesContract` | `System.getProperty(name)` |
| `EnvironmentPropertiesContract` | `System.getenv(name)` |

Properties in YAML files support `${placeholder}` syntax with:
- References to other properties: `${my.other.property}`
- Environment variables: `${env.MY_VAR}`
- System properties: `${some.sys.prop}`
- Decryption: `${decrypt('encrypted-value')}` (using the `RX_DECRYPT_SECRET` property)

The `RxPropertyResolver` resolves properties recursively and reports structured errors (`PropertyNotFound`, `UnresolvedProperty`, `UnresolvedPlaceholder`) via `Maybe<String>`.

### 8.3 YAML-based Configuration

Configuration is loaded from YAML files in the application's `conf/` directory. The platform:

1. Scans `conf/` for files matching a configurable regex pattern.
2. Unmarshalls each file using the GM YAML marshaller into a `Map<String, String>`.
3. Provides these properties to modules through property contracts.

For typed configuration, `readConfig(EntityType<C>)` loads a configuration entity from YAML, validating it against the model.

---

## 9. Resource Storage

The platform provides an abstraction for managing binary resources (files, blobs):

- **`ResourceStorage`**: Interface with `storeResourcePayload`, `getResourcePayload`, `deleteResourcePayload`, and `pipeResourcePayload` operations.
- **`AbstractResourceStorage<P>`**: Base class handling cache control, conditional requests (`FingerprintMismatch`, `ModifiedSince`), and error mapping.
- **`FileSystemResourceStorage`**: Built-in file-system implementation using UUID-named files in timestamped directories.

Resource storage is configured through the `ResourceStorageConfiguration` model:

```yaml
storages:
  - _type: hiconic.rx.resource.model.configuration.FileSystemResourceStorage
    storageId: my-storage
    baseDir: /data/files
```

Modules can register custom storage backends by implementing `ResourceStorageDeploymentExpert` and registering it in `configurePlatform()`.

---

## 10. Data Access (Persistence)

The **Access** subsystem provides a model-driven persistence abstraction:

- **Access Configuration**: An `Access` entity (from `access-configuration-model`) declares an `accessId` and lists of data/service model names.
- **Access Experts**: Implementations of `AccessExpert<A>` that create `IncrementalAccess` instances from access denotation types (e.g., `HibernateAccess`).
- **Access Domains**: Each deployed access creates:
  - A **data model** (configured model with the access's entity types)
  - A **service model** (configured model with persistence-related request types)
  - A **service domain** (with processors for `PersistenceRequest` types: queries, manipulations, etc.)
- **Session Factory**: `PersistenceGmSessionFactory` creates `PersistenceGmSession` instances for programmatic CRUD operations.

The access module exports `AccessContract` (an `RxExportContract`), enabling other modules to:
- Deploy accesses
- Get session factories (context or system scoped)
- Access `AccessDomains` for introspection

The pipeline for a persistence request: HTTP/CLI → service domain dispatch → `RxPersistenceProcessor` → `IncrementalAccess` implementation (e.g., Hibernate).

---

## 11. Application State and Health

The platform tracks application lifecycle through `RxApplicationStateManager`:

### Application States

```
initial → starting → started → stopping → stopped
                        ↓
                       dead
```

| State | Meaning |
|---|---|
| `initial` | Not yet started loading |
| `starting` | Loading and initializing modules |
| `started` | Fully loaded, endpoints exposed |
| `stopping` | Graceful shutdown in progress |
| `stopped` | Shutdown complete |
| `dead` | Fatal state determined by liveness checkers |

### Health Checking

- **Liveness Checkers** (`RxApplicationLivenessChecker`): Return a `Reason` if something is fatally wrong (e.g., deadlock detection).
- **State Change Listeners**: Modules can register `Consumer<RxApplicationState>` listeners for state transitions.
- **Check Framework** (`check-api-model`): A full health check system with:
  - `CheckCoverage`: readiness, vitality, connectivity, functional
  - `CheckLatency`: immediate, moderate, extensive
  - Aggregatable results with `CheckStatus` (ok, warn, fail)
  - Distributed checks across cluster nodes

---

## 12. Endpoints and Transports

Service domains are transport-agnostic — the same service processors can be accessed via multiple endpoints. Endpoints are provided by dedicated modules.

### 12.1 Web / HTTP Endpoint

The `web-server-rx-module` provides an embedded web server (Undertow-based) and exports `WebServerContract`:

```java
public interface WebServerContract extends RxExportContract {
    void addServlet(String name, String path, HttpServlet servlet);
    void addFilter(FilterSymbol name, Filter filter);
    void addFilterMapping(FilterSymbol name, String mapping, DispatcherType type);
    void addEndpoint(String path, Endpoint endpoint);  // WebSocket
    void addStaticFileResource(String path, String rootDir, String... welcomeFiles);
    String publicUrl();
    int getEffectiveServerPort();
    // ...
}
```

Modules use this contract to register servlets and filters during `onDeploy()`.

### 12.2 CLI Endpoint

The `cli-rx-module` provides a command-line interface that:

- Parses POSIX-style CLI arguments into `ServiceRequest` entities using the GM type system.
- Resolves command names to request types (e.g., `reverse-text` → `ReverseText`).
- Supports `@Alias` annotations for short options (`-n` for `--name`).
- Supports `@PositionalArguments` for argument-order-based mapping.
- Provides built-in `Help` and `Introduce` commands for discoverability.
- Routes responses to configurable output channels (stdout, stderr, files).

The CLI service domain (`cli`) is a dedicated service domain where CLI-specific requests live.

### 12.3 Web API (DDRA)

The `web-api-server-rx-module` provides a REST-like HTTP API that maps HTTP requests directly to modeled service requests:

- **URL pattern**: `/api/{domainId}/{RequestTypeName}` (e.g., `/api/main/ReverseText?text=hello`)
- **Automatic parameter mapping**: HTTP query parameters and body content are automatically decoded into `ServiceRequest` properties.
- **Content negotiation**: Supports JSON, YAML, and other formats via `Accept`/`Content-Type` headers.
- **Traversing criteria**: Controls entity graph depth in responses (`shallow` vs `reachable`).

Additionally, the `rest-server-rx-module` provides a RESTful CRUD API for access-based entities with standard HTTP methods mapped to persistence operations.

---

## 13. Applications

A Reflex application is a deployable bundle that aggregates modules, models, and the platform runtime. An application artifact:

1. **Depends on `reflex-platform`** as the core runtime.
2. **Depends on endpoint modules** (e.g., `web-api-undertow-rx-module` for HTTP, `cli-rx-module` for CLI).
3. **Depends on feature modules** (e.g., `hibernate-rx-module` for database persistence).
4. **May itself be a module** — simple applications can implement `RxModuleContract` directly in the app artifact.

The application can be:
- A **web application**: Long-running server process with HTTP endpoints.
- A **CLI application**: Processes command-line arguments, executes a request, and exits.
- A **hybrid**: Both CLI and web capabilities.

The entry point is `RxPlatform.main(String[] args)`, which bootstraps the platform and, for server applications, waits for a shutdown signal.

Application configuration is stored in the `conf/` directory alongside the application bundle, using YAML files that are unmarshalled into configuration model instances.

---

## 14. Artifact Naming Conventions

The repository follows strict naming conventions for artifacts:

| Pattern | Example | Purpose |
|---|---|---|
| `*-model` | `demo-model`, `hello-world-model` | Entity type declarations (general data models) |
| `*-api-model` | `check-api-model`, `cli-api-model` | Service request types (API definitions) |
| `*-configuration-model` | `db-configuration-model`, `access-configuration-model` | Configuration entity types |
| `*-rx-module` | `demo-rx-module`, `cli-rx-module` | Processing modules |
| `*-module-api` | `access-module-api`, `check-module-api` | Inter-module contract declarations (`RxExportContract`) |
| `*-app` | `demo-web-app`, `demo-cli-app` | Application aggregators |

This convention makes it immediately clear what role each artifact plays in the system and ensures clean separation between API, implementation, and configuration.
