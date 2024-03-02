# Hiconic Reflex Platform Components

With separation of data, processing bundling and dependency management in mind the reflex platform was designed to consist of clearly distinguished components. 

1. [Models](#models) for all purpose data
1. [Modules](#modules) for all purpose processing
1. [Module APIs](#module-apis) for inter-module communication
1. [Applications](#applications) for application bundling

# Models

Models are a first class principle in the reflex platform to support domain specific and generic coding. While models are meant to be used in any domain there are typical use cases in reflex like:

* Configuration
* Meta Data
* Business Data
* APIs.

Read more about [modeling](../modeling/modeling.md).

# Modules

Modules are the components that contain processing code and are automatically detected and loaded from the classpath by a reflex [application](#applications). The processing code of Modules is typically concerned with:

* Service Processors for API models
* Service Cross Cutting Processors (e.g. Validation, Authorization)
* Endpoints (e.g. Open API, Graph QL, Posix CLI)

In the case that modules have a general purpose for other modules they may support [modules APIs](#module-apis) for module intercommunication (e.g for expert registration).

Modules communicate with the reflex platform and with other modules through a IOC contracts and spaces which are mediated by the `wire` framework.

Read more about [modules](../modules/modules.md).

# Module APIs

Module APIs declare wire contract interfaces and related interfaces and classes to support intercommunication between modules

Read more about [module-apis](../modules/module-apis.md).

# Applications 

A reflex application is a zip packaged bundle containing libraries, configurations and launch shell scripts. The libraries contain modules, models and transitive dependencies of both. 

Based on the presence of a Java SKD the application can be directly started with the launch scripts.

When an application is started it automatically detects and loads all it module dependencies to create its individual functionality.

Read more about [application bundling](../applications/applications.md).