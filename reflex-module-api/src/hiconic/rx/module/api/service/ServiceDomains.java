package hiconic.rx.module.api.service;

import java.util.List;

import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.meta.GmMetaModel;
import com.braintribe.model.service.api.ServiceRequest;

public interface ServiceDomains {
	/**
	 * Returns the main {@link ServiceDomain}
	 */
	ServiceDomain main();
	/**
	 * Returns the {@link ServiceDomain} for given domainId if available otherwise null
	 */
	ServiceDomain byId(String domainId);
	
	/**
	 * Enumerates all available ServiceDomains
	 */
	List<? extends ServiceDomain> list();

	List<? extends ServiceDomain> listDomains(EntityType<? extends ServiceRequest> requestType);

	List<? extends ServiceDomain> listDomains(GmMetaModel model);

}
