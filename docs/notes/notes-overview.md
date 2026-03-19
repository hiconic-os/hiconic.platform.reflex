# Hiconic Reflex Platform — Core Concepts

Reflex (RX) is a lightweight, modular platform for building applications using a model-driven paradigm.

## Basic Overview

Each application consists of the `reflex-platform`, various modules and its own configuration. An application can be a module itself.

Reflex uses an IOC framework called _Wire_. In _Wire_ one defines _Contracts_ (interfaces that extend `WireSpace`) which serve as catalogues of available beans, rather than use annotations on classes. Each contract is implemented by a class called _Space_, which can import other _Contracts_ or _Spaces_.

_Reflex_ modules are integrated together via these _Contracts_. The platform offers various _Contracts_ for modules to access its core components and each module can defined new _Contracts_ for other modules.

_Reflex_ heavily relies on modeling. A model is a collection of entity types (e.g. Person with properties such as name, birthDat.) and enums. Models can be defined as artifacts, with a name indicating their purpose (e.g. `xyz-configuration-model`, `xyz-api-model`), and the _entities_ and _enums_ can have configuration attached to them (e.g. _mandatory_ on a property, _URL path_ on a request type).

Typical entities represent structured data, there is one special entity - `Resource` - that represents binary data. `Resource` also has a property `resourceSource`, which specifies how it is stored (with types like `SqlSource` or `FileSystemSource`). A `Resource` can only be created on the fly, without being stored anywhere, and is then called a _transient_ `Resource`.

A special model - `meta-model` - is a model that denotes other models (type `GmMetaModel`, which should have been called `GmModel`). It describes their types, properties of entities, constants of enums and dependencies to other models. Besides that, it also contains configurations in the form of `MetaData`, which can be attached to every model component (model, entity, property, enum, constant).

`MetaData` types are typically defined in their own model - `xyz-metadata-model`.

Model elements can be configured with Java annotations, which are automatically converted to `MetaData` instances. Some are available by the platform, but custom ones can be defined too, with a descriptor for the conversion to MD (`META-INF/gmf-mda`). See e.g. `web-api-mapping-meta-data-model`.

It is also possible to create a model programmatically, in order to aggregate and configure other existing models. This also allows configuring the same types differently in different contexts (e.g. storing resources in SQL DB for one component and on file-system for another).

`ModelOracle` is an interface that aggregates an information about a structure of models and types, it can find sub-types and super-types for each type, as well as find all the relevant data related to a type, i.e. resolving the single `GmEntityType` instance, as well as possible multiple `GmEntityTypeOverride` instances with additional `MetaData` (and analogously e.g. `GmProperty` and `GmPropertyOverride`)

`CmdResolver` is an interface that resolves `MetaData`, taking into account aggregation (base model element plus all Overrides) inheritance (type plus its supertypes), and parametrizing the resolution with contextual information, e.g. user roles when checking visibility.

A key concept of a _Reflex_ application is _ServiceDomain_, which is a named collection of `ServiceRequests` (base type from `service-api-model`), with relevant configuration (via `MetaData`), e.g. which request is processed by which processor, security, URL mappings, etc.


Various other APIs offered by the platform:
- `ResourceStorage` for binary data persistence, such as file-system, SQL DB or S3.
- `Marshallers` for serializing/deserializing entities (YAML, JSON, binary)
- `StreamPipes` for efficiently working with ephemeral binary data - small stays in memory, bigger is written to disk

## Reflex Modules

The purpose of a _Reflex_ module is to:
- extend the platform itself, e.g. bring an implementation for a `ResourceStorage`.
- implement an API used by other modules, e.g. DB connection, messaging.
- offer a generic feature for other modules to use, e.g. start a servlet container and offer servlet registration
- offer a specific feature such, e.g register service domain with implementation of certain requests

### Extension Mechanism

An API to be used by other modules is exposed as an `RxExportContract` (which is a `WireSpace`), e.g.
```java
public interface ImageWatermarkContract extends RxExportContract {
    ImageWatermarker imageWatermarker();
}
```

This would typically be placed in a `image-conversion-api`, which would be depended by another module, whose service processors uses `ImageWatermarker` internally, like this:
```java
public class MyAppSpace implements MyAppContract {
    @Import
    ImageWatermarkContract  imageWatermark;
    ...
    private ImageProcessor imageProcesor() {
        var bean = new ImageProcessor();
        bean.setImageWatermarker(imageWatermark.imageWatermarker);

        return bean;
    }
}
```

This way there is no dependency on a specific implementation, which might come from another module, which binds a `Space` to this `Contract`.
```java
public enum MyWatermarkRxModule implements RxModule<MyWatermarkRxModuleSpace> {
	INSTANCE;
	@Override
	public void bindExports(Exports exports) {
		exports.bind(ImageWatermarkContract.class, MyWatermarkSpace.class);
	}
}
```
```java
@Managed
public class MyWatermarkRxModuleSpace implements RxModuleContract, ImageWatermarkContract {
    @Managed
    MyImageWatermarker imageWatermarker() {
        var bean = new MyImageWatermarker();
        ...
        return bean;
    }
}
```
Extending upon the platform itself is easier, one does it via the contracts from `reflex-module-api` which every module depends.
```java
@Managed
public class JdbcResourceStorageRxModuleSpace implements RxModuleContract {
	@Import
	private RxPlatformContract platform;
	@Import
	private DatabaseContract database;

	@Override
	public void configurePlatform(RxPlatformConfigurator configurator) {
		configurator.registerResourceStorageDeploymentExpert( //
				hiconic.rx.resource.jdbc.model.configuration.JdbcResourceStorage.T, SqlSource.T, this::deployJdbcResourceStorage);
	}
	@Managed
	private Maybe<ResourceStorage> deployJdbcResourceStorage(hiconic.rx.resource.jdbc.model.configuration.JdbcResourceStorage storageDenotation) {
	    ...
	}
}
```

### Module Lifecycle

When modules are being loaded, their various methods are called in a specific order, in order to configure the platform and deploy the components. This is documented in detail on `RxModuleContract`.

The most important thing, however, is that there are two phases to the module loading - the one while models are being configured and cannot be accessed (e.g. via `ModelOracle`), and the one after, where they cannot be configured anymore.

Model configuring methods:
- configureMainPersistenceModel(ModelConfiguration)
- configureModels(ModelConfigurations)
- configureMainServiceDomain(ServiceDomainConfiguration)
- configureServiceDomains(ServiceDomainConfigurations)
- registerCrossDomainInterceptors(InterceptorRegistry)
- registerFallbackProcessors(ProcessorRegistry)
- configurePlatform(RxPlatformConfigurator)

Post-Model configuring methods:
- onDeploy()
- onApplicationReady()

Note that the system detects a model-configuration related violation internally and the app would not even start.

### Modeled Configuration



## Service Domains

_Service Domain_ is defined by its name and its model, which contains `ServiceRequests` types with `MetaData` specifying the corresponding `ServiceProcessors` and interceptors. The corresponding metadata (e.g. `ProcessWith`) are defined in `service-processing-metadata-model`.

However, since _Service Domains_ are such a key concept, they have their configuration has its own lifecycle method, with an API for a more convenient configuration.

Example:
```java
@Managed
public class CoolFeatureRxModuleSpace implements RxModuleContract {
	@Override
	public void configureServiceDomains(ServiceDomainConfigurations configurations) {
		ServiceDomainConfiguration coolFeatureDomainConfiguration = configurations.byId("cool-feature-domain-id");
		coolFeatureDomainConfiguration.bindRequest(AbstractCoolFeatureRequest.T, this::coolFeatureProcessor);
	}
}
```
This creates a new (retrieves an existing) _Service Domain_ by its id, with its own configuration model, which initially only depends on `service-api-model`. The `bindRequest` method then adds the model of `AbstractCoolFeatureRequest` as a dependency, and configures `ProcessWith` metadata on this type.

Note that we typically implement a Process for an entire hierarchy of requests, rather than a single request, which we then configure on the base type, such as `AbstractCoolFeatureRequest`.

### Evaluators

Assuming there is a `DoCoolThingWithPdf` request that extends `AbstractCoolFeatureRequest`, we can now anywhere in the code evaluate the request:

```java
DoCoolThingWithPdf request = DoCoolThingWithPdf.T.create();
request.setPdf(pdfResource);
CoolPdfResult resultMaybe = evaluator.eval(request).get();
```

or do the same thing `reasoned`, which offers much better [error handling](#error-handling);

```java
DoCoolThingWithPdf request = DoCoolThingWithPdf.T.create();
request.setPdf(pdfResource);
Maybe<CoolPdfResult> resultMaybe = evaluator.eval(request).getReasoned();

if (resultMaybe.isUnsatisfied()) {
	// log issue
	return;
}

CoolPdfResult result = resultMaybe.get();
```

The evaluator is available from the _Reflex_ platform via its `RxPlatformContract`, which can be imported to any module.

## Accesses



## Error handling

// TODO explain maybe