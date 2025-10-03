// ============================================================================
package hiconic.rx.reflection.model.api;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.annotation.Abstract;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

@Abstract
public interface PlatformReflectionResponse extends GenericEntity {

	EntityType<PlatformReflectionResponse> T = EntityTypes.T(PlatformReflectionResponse.class);
	
}
