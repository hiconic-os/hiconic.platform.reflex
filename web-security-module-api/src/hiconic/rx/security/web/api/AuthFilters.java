package hiconic.rx.security.web.api;

import hiconic.rx.web.server.api.FilterSymbol;

/**
 * Collection of {@link FilterSymbol}s for filters that the implementing module registers.
 * 
 * @author peter.gazdik
 */
public enum AuthFilters implements FilterSymbol {

	/** Requires authentication and returns a reasoned HTTP failure. */
	strictAuthFilter,
	/** Requires authentication and applies browser UI navigation on failure. */
	strictUiAuthFilter,
	/** Establishes authentication context without rejecting an anonymous request. */
	lenientAuthFilter,
	/** Requires an administrative role and returns a reasoned HTTP failure. */
	strictAdminAuthFilter,
	/** Requires an administrative role and applies browser UI navigation on failure. */
	strictAdminUiAuthFilter;

}
