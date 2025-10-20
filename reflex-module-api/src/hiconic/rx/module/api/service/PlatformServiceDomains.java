package hiconic.rx.module.api.service;

import com.braintribe.model.service.api.CompositeRequest;
import com.braintribe.model.service.api.MulticastRequest;
import com.braintribe.model.service.api.UnicastRequest;

/**
 * @author peter.gazdik
 */
public enum PlatformServiceDomains implements ServiceDomainSymbol {

	/**
	 * This is a pre-defined service domain, which you can use in a simple application where you don't bother creating custom service domains.
	 * <p>
	 * The whole purpose is convenience, it does not contain any requests provided by the platform.
	 *
	 * @see ServiceDomains#main()
	 * @see ServiceDomainConfigurations#main()
	 */
	main,

	/**
	 * Intended to contains common, domain independent requests.
	 * <p>
	 * The platform itself binds processors for {@link CompositeRequest} and {@link UnicastRequest}, though the letter delegates to a
	 * {@link MulticastRequest} whose support must come from a specific module (e.g. casting-via-messaging).
	 *
	 * @see ServiceDomains#system()
	 * @see ServiceDomainConfigurations#system()
	 */
	system,

}
