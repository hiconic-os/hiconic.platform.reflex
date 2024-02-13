```java
package example.model.api;

import com.braintribe.model.generic.eval.EvalContext;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.service.api.ServiceRequest;

/* Service Request types derive directly or indirectly from ServiceRequest */
public interface Greet extends ServiceRequest {
    // Type literal for construction and reflection
	EntityType<Greet> T = EntityTypes.T(Greet.class);
	
    // Determines an argument property of this request
	String getName();
	void setName(String name);
	
    /* A service request has a response type which can be determined with the eval method to make evaluation type safe */
	@Override
	EvalContext<String> eval(Evaluator<ServiceRequest> evaluator);
}
```