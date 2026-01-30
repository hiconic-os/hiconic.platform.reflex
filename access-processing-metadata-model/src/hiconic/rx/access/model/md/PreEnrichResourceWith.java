// ============================================================================
package hiconic.rx.access.model.md;

import com.braintribe.model.generic.annotation.Transient;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.meta.data.EntityTypeMetaData;
import com.braintribe.model.resource.source.ResourceSource;

/**
 * Specifies ResourceEnrichers for given ResourceSource type.
 * <p>
 * This meta-data should be configured on the proper {@link ResourceSource} type.
 */
public interface PreEnrichResourceWith extends EntityTypeMetaData {

	EntityType<PreEnrichResourceWith> T = EntityTypes.T(PreEnrichResourceWith.class);

	@Transient
	<A> A getResourceEnricher();
	void setResourceEnricher(Object resourceEnricher);

}