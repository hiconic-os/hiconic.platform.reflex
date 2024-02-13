```java
package example.model;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

/* 
A type can extend multiple other model types using extends. Abstract types are marked with @Abstract annotation.
*/
public interface Circle extends Shape, HasCoordinates {
    // Type literal for construction and reflection
	EntityType<Person> T = EntityTypes.T(Person.class);
	
	double getRadius();
    void setRadius(double radius);
}
```
