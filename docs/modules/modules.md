# Reflex Modules

A _Reflex_ module has to declare a number of things to be recognized and loadable by the _Reflex_ platform:

* Artifact [Dependencies](#artifact-dependencies)
* Dependency Injection
  * _Reflex_ Wire [Module](#reflex-wire-module)
  * _Reflex_ Module [Space](#reflex-module-space)

You can create a module with all the requirements from a given name on the command line with the devrock-sdk:

```
jinni create-reflex-module <your-rx-module>
```

# Artifact Dependencies

Add the following dependency to your artifact descriptor:

```xml
<!-- 
    The Reflex-module-api brings the essential interfaces
    rquired to declare a Reflex module:
    RxModuleContract
    RxModule 
-->
<dependency>
    <groupId>hiconic.platform.reflex</groupId>
    <artifactId>reflex-module-api</artifactId>
    <version>[1.0,1.1)</version>
</dependency>
```

Additionally you can depend on model artifacts and any other library required for the internal implementation of the module's functionality.

> Note: Currently you have to be compatible with the overall dependencies of the platform and other modules that are aggregated as an application.

# Reflex Wire Module

A _Reflex_ module has to define a wire module for dependency. It basically names the [module space](#reflex-module-space) which will do all the required registrations against the platform or other modules:

Source file: `example/module/wire/ExampleRxModule.java`

```java
package example.module.wire;

import example.module.wire.space.ExampleRxModuleSpace;
import hiconic.rx.module.api.wire.RxModule;

// RxModule definition with space binding via generics
public enum ExampleRxModule implements RxModule<ExampleRxModuleSpace> {
	INSTANCE;
}
```

For automatic detection and loading by the _Reflex_ platform the wire module needs to be announced in a properties file:

Source file: `META-INF/rx-module.properties`

```properties
wire-module=example.module.wire.ExampleRxModule
```
# Reflex Module Space

The module space is the place to register experts for the platform or other modules. Cases for experts of the platform are:

* Service Processors
* Service Interceptors
* Marshallers

Source file: `example/module/wire/space/ExampleRxModuleSpace.java`

```java
package example.module.wire.space;

import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.module.api.service.ServiceDomainConfiguration;
import hiconic.rx.module.api.service.ServiceDomainConfigurations;
import hiconic.rx.module.api.wire.RxModuleContract;

@Managed
public class ExampleRxModuleSpace implements RxModuleContract {

    @Override
    public void configureMainServiceDomain(ServiceDomainConfiguration configuration) {
        // Register your processors for the main service domain
        configuration.register(Greet.T, greetProcessor());
    }

    @Override
    public void configureServiceDomains(ServiceDomainConfigurations configurations) {
        // TODO: reigster your processors for arbitrary service domains
    }

    @Managed
    private GreetProcessor greetProcessor() {
        GreetProcessor bean = new GreetProcessor()
        bean.setGreetWord("Ola");
        return bean;
    }
}
```

# Example Processor

Source file: `example/module/processing/GreetProcessor.java`
```java
package example.module.processing;

import com.braintribe.model.processing.service.api.ServiceProcessor;
import com.braintribe.model.processing.service.api.ServiceRequestContext;

import example.model.api.Greet;

public class GreetProcessor implements ServiceProcessor<Greet, String> {
    private String greetWord = "Hello";

    public void setGreetWord(String greetWord) {
        this.greetWord = greetWord;
    }

    @Override
    public String process(ServiceRequestContext context, Greet request) {
        String name = request.getName();
        
        if (name == null)
            name = "nobody";

        return greetWord + " " + name + "!";
    }
}
```

