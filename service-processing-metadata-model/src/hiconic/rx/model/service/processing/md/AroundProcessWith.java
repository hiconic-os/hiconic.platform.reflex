// ============================================================================
package hiconic.rx.model.service.processing.md;

import com.braintribe.model.generic.annotation.meta.Description;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

@Description("Maps a ServiceAroundProcessor from the service-api using the transient 'associate' property. It can be applied to ServiceRequest entity types.")
public interface AroundProcessWith extends InterceptWith {

	EntityType<AroundProcessWith> T = EntityTypes.T(AroundProcessWith.class);

	@Override
	default InterceptionType interceptionType() {
		return InterceptionType.aroundProcess;
	}
}
