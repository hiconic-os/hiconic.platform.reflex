// ============================================================================
package hiconic.rx.model.service.processing.md;

import com.braintribe.model.generic.annotation.meta.Description;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

@Description("Maps a ServicePreProcessor from the service-api using the transient 'associate' property. It can be applied to ServiceRequest entity types.")
public interface PreProcessWith extends InterceptWith {
	EntityType<PreProcessWith> T = EntityTypes.T(PreProcessWith.class);

	@Override
	default InterceptionType interceptionType() {
		return InterceptionType.preProcess;
	}
}
