// ============================================================================
package hiconic.rx.model.service.processing.md;

import com.braintribe.model.generic.annotation.meta.Description;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

@Description("Maps a ServicePostProcessor from the service-api using the transient 'associate' property. It can be applied to ServiceRequest entity types.")
public interface PostProcessWith extends InterceptWith {

	EntityType<PostProcessWith> T = EntityTypes.T(PostProcessWith.class);

	@Override
	default InterceptionType interceptionType() {
		return InterceptionType.postProcess;
	}

}
