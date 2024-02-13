```java
package example.model;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.annotation.Abstract;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

/* 
A type can derive from another model type using extends. Abstract types are marked with @Abstract annotation.
*/
@Abstract
public interface HasCoordinates extends GenericEntity {
    // Type literal for construction and reflection
	EntityType<Person> T = EntityTypes.T(Person.class);
	
	double getX();
	void setX(double x);

	double getY();
	void setY(double y);
}
```