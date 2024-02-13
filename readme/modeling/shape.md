```java
package example.model;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.annotation.Abstract;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

/* 
If a type has no other super type from a model it derives at least from GenericEntity. Abstract types are marked with @Abstract annotation.
*/
@Abstract
public interface Shape extends GenericEntity {
    // Type literal for construction and reflection
	EntityType<Person> T = EntityTypes.T(Person.class);
	
    // a property
	Color getColor();
	void setColor(Color color);
}
```