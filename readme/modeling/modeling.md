# Models

Models declare entity types and enum types for all purposes. Typical cases for models are:

* API models
* Business Data
* Metadata / Mapping
* Configuration models

You can create a model with all requirements from a given name on the command line with the devrock-sdk:

```
jinni create-model <your-model>
```

## Artifact Dependencies

Add the following dependency to create your model:

```xml
<!-- 
    root-model declares the base types
    modeling in general:
    GenericEntity
    EnumBase
-->
<dependency>
    <groupId>com.braintribe.gm</groupId>
    <artifactId>root-model</artifactId>
    <version>[2.0,2.1)</version>
</dependency>
```
If you build your model as extension to other models you have to depend those as well. For example if you build a service api model you will have to add the following dependency:
```xml
<!-- 
    service-api-model declares the base type
    for service processing:
    ServiceRequest
-->
<dependency>
    <groupId>com.braintribe.gm</groupId>
    <artifactId>service-api-model</artifactId>
    <version>[2.0,2.1)</version>
</dependency>
```
## Declaration of Model Types

To create types for a model you have to declare interfaces and enums. The interfaces are automatically implemented and reflected by the GenericModel type-system.

The following source code examples show how to declare model types compatible with the GenericModel type-system:

### General Rules

* Entity types are sub types of `GenericEntity`
* Entity types can use multiple inheritance
* Abstract entity types are annotated with `@Abstract`
* Enum type have to implement `EnumBase`
* All types have a type literal `T` for reflection access

### Enum Modeling

```java
public enum Color implements EnumBase {
    red, 
    green, 
    blue;

    public static final EnumType T = EnumTypes.T(Color.class);

    @Override
    public EnumType type() { return T; }
}
```

### Entity Modeling 

```java
// An abstract entity
@Abstract
public interface Shape extends GenericEntity {
    EntityType<Person> T = EntityTypes.T(Person.class);

    Color getColor();
    void setColor(Color color);
}

// Another abstract entity
@Abstract
public interface HasCoordinates extends GenericEntity {
    EntityType<Person> T = EntityTypes.T(Person.class);
    
    double getX(); 
    void setX(double x);

    double getY();
    void setY(double y);
}

// Multiple inheritance
public interface Circle extends Shape, HasCoordinates {
    EntityType<Person> T = EntityTypes.T(Person.class);

    double getRadius();
    void setRadius(double radius);
}
```

### Service Request Modeling

```java
// Service Request types derive from ServiceRequest
public interface Greet extends ServiceRequest {
    EntityType<Greet> T = EntityTypes.T(Greet.class);
	
    // Property as in the role of a request argument
    String getName();
    void setName(String name);

    // Eval method declares response type and helps with type-safe evaluation
    @Override
    EvalContext<String> eval(Evaluator<ServiceRequest> evaluator);
}
```

## Using Models

```java
// Instantiation via T literal
Circle circle = Circle.T.create();
circle.setColor(Color.red);
circle.setX(2);
circle.setY(5);
circle.setRadius(7);

Greet greet = Greet.T.create();
greet.setName("friend");

String response = greet.eval(evaluator).get();
```