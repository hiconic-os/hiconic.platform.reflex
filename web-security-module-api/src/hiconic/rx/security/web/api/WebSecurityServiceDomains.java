package hiconic.rx.security.web.api;

import hiconic.rx.module.api.service.ServiceDomainSymbol;

/** Service domains contributed by the web-security module. */
public interface WebSecurityServiceDomains {

	/** HTTP- and cookie-oriented authentication and authorization services. */
	ServiceDomainSymbol webAuth = () -> "web-auth";

}
