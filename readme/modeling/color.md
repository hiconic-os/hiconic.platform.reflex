```java
package example.model;

import com.braintribe.model.generic.base.EnumBase;
import com.braintribe.model.generic.reflection.EnumType;
import com.braintribe.model.generic.reflection.EnumTypes;

/* Enums derive from EnumBase to mark them as model enums and equip them with type support */
public enum Color implements EnumBase {
    // enum constants
	red, green, blue;
	
    // Type literal for reflection
	public static final EnumType T = EnumTypes.T(Color.class);
	
    // Instance level access to the type
	@Override
	public EnumType type() { return T; }
}
```