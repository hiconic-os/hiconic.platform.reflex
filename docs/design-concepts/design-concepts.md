## Design Concepts

## Concept

While the _Reflex_ platform is built around modeled services it is kept abstract beyond that. The actual functionality is left to modules. Such modules will be automatically found on the classpath and loaded from there. Modules can act in different roles and domains such as:

* service processor
* service endpoint projection
* configurator
* authorization
* validation
* serialization
* persistence

## Configuration

Configuration of the _Reflex_ platform and its modules is based on wire IOC and configuration models following a strict pattern of separation of concerns. The central goal is to reduce the requirement configuration where possible and at the same time having a flexible and scalable way for configuration when it is required by the complexity of a growing project.

While annotations play a role they are not the central principle of configuration in order to keep the models and services portable towards different technologies for transport, remote invocation, persistence, serialization. Furthermore the system supports modeled metadata on service and data models in order to enrich model types with freely customizable information for mapping, validation, etc.

IOC plays the role of a decoupled "third" power that connects models, services, endpoints, serializers for certain scenarios yet keeping the system flexible to project it otherwise. Configuration models allow to type-safely input information from outside of the code into the IOC wiring process. 

Dependency injection is done explicitly using interface based wiring contracts that offer functionality from either the platform or modules. The benefits of this wiring approach are:

* less magic and obscurity
* understandability
* control
* flexibility