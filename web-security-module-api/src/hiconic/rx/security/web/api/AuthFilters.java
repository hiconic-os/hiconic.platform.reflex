package hiconic.rx.security.web.api;

import hiconic.rx.web.server.api.FilterSymbol;

/**
 * Collection of {@link FilterSymbol}s for filters that the implementing module registers.
 * 
 * @author peter.gazdik
 */
public enum AuthFilters implements FilterSymbol {

	strictAuthFilter,
	lenientAuthFilter,
	strictAdminAuthFilter;

}
