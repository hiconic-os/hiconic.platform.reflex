# Hiconic Vision

_Let's give modeling a chance._

## Overview

The purpose of this document is to:
* show some of the key ideas behind _Hiconic_ on a high level
* highlight its uniqueness and potential
* offer a taste of its power and simplicity

followed by the state of the art discussion which explains what is:
* done
* work in progress
* on the roadmap

_NOTE: For the sake of brevity we only a show small subset of the platform's perks._

<!--
Lastly, we discuss the broader advantages of our approach, including in the context of artificial intelligence.
-->

## Context

### State of SW Development

Let's set the stage by a few (bold) claims about various aspects of current SW development:
* **modeling support** - data types are treated as second class citizens by programming languages
* **model advantages** - data types are underappreciated and underutilized
* **convention over configuration** - modern frameworks aren't convenient enough
* **agility** - developers are being bothered by technical details
* **portability** - code is not portable enough


### Problem outline

To illustrate these points, let's consider the following question: \
**What is the simplest way to create a backend application?**

Such an app is quite basic - it defines a set of requests a client can make (API), and for each request sends back a response.

The developer thus has to do 3 things:
* define the API (requests)
* implement the services
* offer a way for the client to make a request

## The _Hiconic_ Way

Let's see how simple implementing a backend application is with _Hiconic_.

We'll go with a single request - `Greet` - with parameter called `name`, which responds `Hello` to the person with given name (e.g. `Hello John`).

_NOTE: We show a simplified, less technically correct example, compared to a real implementation ([see tutorial](../getting-started/getting-started.md)). It is to avoid technical details, irrelevant at this level, in favor of paradigm clarity.

### Defining the API

Our way to define an API is to model the requests, i.e. create an entity type for each request.

```java
entity Greet extends ServiceRequest {
    String name

    String eval()
}
```

### Implementing the service

This is a straight forward (_Java_) code, packaged as a module:

```java
public class GreetProcessor implements ServiceProcessor<Greet> {
    public String process(Greet request) {
        return "Hello " + request.name;
    }
}
```

_NOTE: We also need to bind this processor to the `Greet` request, which is straight forward but let's ignore that syntax for now._

### Offering an endpoint to the client

Let's say we want to deploy our service as a web application accessible over _REST_.

In our world, an application is basically an empty shell, which defines the platform (e.g. _Reflex_, our microservices platform) and a list of modules. So we do just that:
* greet-module (with our model)
* web-server-module
* rest-endpoint-module

**Aaaand that's it, so easy!**

We can now build the app, deploy it, access via `example.com/api/greet?name=World` and get `"Hello World"` back.

## Dissecting the _Hiconic_ Way

Let's see what we did (or didn't have to do), and how have we addressed the [aspects](#state-of-sw-development) we mentioned at the beginning:

We have **modeled** our API, which has tons of **advantages**, e.g.:
* easy to intercept, extend/modify behavior based on type hierarchy or metadata configuration
* ideal for microservices, forces communication over APIs
* ideal for client, natural fit for asynchronous programming
* in code, evaluating a request instance hides the underlying protocol; you don't worry if it's a local call or a remote one over REST, you don't need to know how to encode it into URL

We have NOT, however, bothered the backend developer with mapping the request to the URL. The mapping is automatic, based on request name and its properties - **convention over configuration**.

As for **agility**, we also didn't define the response's format (e.g. JSON). In fact, it's up to client to describe what they want, JSON is just default.

_NOTE: Yes, our service returned just text, but it is JSON - see the quotes around the text._

**Portability** is achieved by packaging our service as a module, independent of where it will be deployed and how it will be accessed. We went with a REST based web application, but it could be:
* RPC based web application
* command line application
* event listener (Apache Kafka)
* anything else (e.g. smartphone application with generic UI)

Our platform is built for **modeling** with lots of tools and features. We treat models as an extension of the language, entities as separate types (besides classes and interfaces) with their own reflection. This makes any kind of domain-independent task easy to implement with elegant and efficient code (e.g. serializing as JSON or auto-mapping URL parameters).

## State of the Art

Where we are and where we're going:

### Core Technology

* Our codebase is primarily _Java_.
* Core of our tech are the models, with multiple tools and platforms built on top.
* We don't support a dedicated syntax for entities yet. We declare entities as _Java_ interfaces, with classes being implemented automatically.
* Writing generic code for our entities is easy and the code is fast. Serializing to JSON takes ~25% less time than industry standard `jackson-databind`, for example.
* Our entities are more flexible than other frameworks. We support multiple inheritance and cyclic references.
* Currently we only support development with `Eclipse` with our plugins, and a custom build tool implemented on top of `Gradle` (and `Ant` which we'll get rid of).

### Platforms

* **Cortex** is a modular monolith platform that's been developed for years and there are a few commercial instances running live. It is powerful, yet bulky.
* **Reflex**, a modular microservices platform, is a recent initiative. A very lightweight core was written from scratch and lots of features can be directly migrated from _Cortex_. It already supports REST and RPC web endpoints, but can also be packaged as a command line tool.
* Our models are also supported in _JavaScript_/_TypeScript_ for frontend development. We can also envision a pure _Node.js_ implementation. Our modeled requests are a natural fit for asynchronous programming.
* We have a so-called **"step-framework"**, built for _Gradle_, which makes big chunks of our pipeline scripts portable.

### Work in Progress

* Support popular tools like _Gradle_ and IDEs like _IntelliJ IDEA_.
* Migrate more **Cortex** modules to **Reflex** to make it real world ready.
* Explain to people how consequent application of modeling leads to repeated patterns beneficial for both human and artificial intelligence.

### Roadmap

* Codebase cleanup to make it easier for outsiders.
* Release in _Maven Central_.
* Pre-compile entities, rather than generate bytecode at runtime to improve startup time.
* Support native images on _GraalVM_..
* Improve _JavaScript_ support, offer a more lightweight library that loads quickly.
* Support dedicated syntax for entities.
