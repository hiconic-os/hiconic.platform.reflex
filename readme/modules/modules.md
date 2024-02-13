A module has to depend on its required models and on the reflex-module-api using this dependency:

```xml
<dependency>
    <groupId>hiconic.platform.reflex</groupId>
    <artifactId>reflex-module-api</artifactId>
    <version>[1.0,1.1)</version>
</dependency>
```

A Module has to define a wire module for the RxModuleContract and has to implement this contract in an according wire space

