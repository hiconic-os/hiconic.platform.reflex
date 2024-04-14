// ============================================================================
package hiconic.rx.model.service.processing.md;

import com.braintribe.model.generic.annotation.Abstract;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.meta.data.EntityTypeMetaData;

@Abstract
public interface InterceptWith extends EntityTypeMetaData, HasTransientAssociate {

	EntityType<InterceptWith> T = EntityTypes.T(InterceptWith.class);

	InterceptionType interceptionType();
}
