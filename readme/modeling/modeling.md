# Models

To create types for a model you have to declare interfaces and enums. The interfaces are automatically implemented and reflected by the GenericModel type-system.

A model artifact has

You can create a model with all the requirements from a given name on the command line with the devrock-sdk:

```
jinni create-model <your-model>
```

## Modeling in General

To create your own model you require a dependency on the root-model:

```xml
<dependency>
    <groupId>com.braintribe.gm</groupId>
    <artifactId>root-model</artifactId>
    <version>[2.0,2.1)</version>
</dependency>
```
The following source code snippets show how to create custom model types compatible with the GenericModel type-system:

* enum [Color](./color.md)
* abstract entity [Shape](./shape.md)
* abstract entity [HasCoordinates](./has-coordinates.md)
* entity [Circle](./circle.md)

## Modeling Service APIs

To create your custom service api you require a dependency on the service-api-model:

```xml
<dependency>
    <groupId>com.braintribe.gm</groupId>
    <artifactId>service-api-model</artifactId>
    <version>[2.0,2.1)</version>
</dependency>
```

The following additional code snippets show how to create custom service request types:

* request entity [Greet](./greet.md)

