package hiconic.rx.platform.resource;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.resource.source.ResourceSource;

import hiconic.rx.module.api.resource.ResourceStorage;
import hiconic.rx.module.api.service.ServiceDomain;

/**
 * Platform registry for {@link ResourceStorage}s.
 * 
 * @author peter.gazdik
 */
public class RxResourcesStorages {

	// TODO fill from configuration
	
	/** Returns the {@link ResourceStorage} for given name or <tt>null</tt> if no such storage exists. */
	public ResourceStorage byName(String name) {
		throw new UnsupportedOperationException("Method 'RxResourcesStorages.byName' is not implemented yet!");
	}
	
	/** Resolves default {@link ResourceStorage} for the given {@link ResourceSource} type. */
	public Maybe<ResourceStorage> resolveDefaultStorage(EntityType<? extends ResourceSource> source) {
		throw new UnsupportedOperationException("Method 'RxResourcesStorages.resolveDefaultStorage' is not implemented yet!");
	}

}
