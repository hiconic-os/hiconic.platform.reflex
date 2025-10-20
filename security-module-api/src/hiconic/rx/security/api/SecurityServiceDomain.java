package hiconic.rx.security.api;

import com.braintribe.model.securityservice.AuthenticateCredentials;
import com.braintribe.model.securityservice.SecurityRequest;
import com.braintribe.model.securityservice.SimplifiedOpenUserSession;

import hiconic.rx.module.api.service.ServiceDomainSymbol;

/**
 * @author peter.gazdik
 */
public enum SecurityServiceDomain implements ServiceDomainSymbol {

	/**
	 * Domain for security-related requests, e.g.
	 * <ul>
	 * <li>{@link SecurityRequest}
	 * <li>{@link SimplifiedOpenUserSession}
	 * <li>{@link AuthenticateCredentials}
	 * <ul>
	 */
	security

}
