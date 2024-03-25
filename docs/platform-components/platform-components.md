# Reflex Platform Components

With separation of data, processing bundling and dependency management in mind the _Reflex_ platform was designed to consist of clearly distinguished components. 

1. [Models](#models) for all purpose data
1. [Modules](#modules) for all purpose processing
1. [Module APIs](#module-apis) for inter-module communication
1. [Applications](#applications) for application bundling

# Models

Models are a first class principle in the _Reflex_ platform to support domain specific and generic coding. While models are meant to be used in any domain there are typical use cases in _Reflex_ like:

* Configuration
* Business Data
* APIs.

Read more about [modeling](../modeling/modeling.md).

# Modules

Modules are the components that contain processing code and are automatically detected and loaded by a _Reflex_ [application](#applications). The processing code of modules is typically concerned with:

* Service Processors for API models
* Service Cross Cutting Processors (e.g. Validation, Authorization)
* Endpoints (e.g. Open API, Graph QL, Posix CLI)

In the case that modules have a general purpose for other modules they may support [modules APIs](#module-apis) for inter-module communication (e.g for dependency injection).

Modules communicate with the _Reflex_ platform and with other modules through IOC contracts and spaces which are mediated by the `wire` framework.

Read more about [modules](../modules/modules.md).

# Module APIs

Module APIs declare contract interfaces for the `wire` IOC framework to support inter-module communication. 

On one side the contract interfaces are bound to implementing spaces by supplying [modules](#modules). An example could be an expert registry.

On the other side the contract interfaces are imported by consuming [modules](#modules). An example could be an expert registration.

Read more about [module APIs](../modules/module-apis.md).

# Applications 

A _Reflex_ application is a zipped bundle containing libraries, configurations and launch shell scripts. The libraries include modules and models, with all their dependencies. 

The application can be directly started with the launch scripts (assuming Java Runtime Environment is present).

When an application starts it automatically detects and loads all its modules.

Read more about [applications](../applications/applications.md).