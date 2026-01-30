// ============================================================================
package hiconic.rx.access.module.api;

import com.braintribe.model.resource.Resource;

/**
 * An expert for enriching Resources by providing or extending the resource's {@link Resource#getSpecification() specification}.
 */
public interface ResourceEnricher {

	/** @return true iff resource was enriched */
	boolean enrich(Resource resource);

}
