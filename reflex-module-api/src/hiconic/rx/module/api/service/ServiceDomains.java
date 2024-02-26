package hiconic.rx.module.api.service;

import java.util.List;

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
}
