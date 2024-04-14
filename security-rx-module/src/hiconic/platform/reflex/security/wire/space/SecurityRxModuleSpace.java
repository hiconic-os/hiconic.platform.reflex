package hiconic.platform.reflex.security.wire.space;

import com.braintribe.model.processing.service.api.InterceptorRegistry;
import com.braintribe.model.securityservice.AuthenticateCredentials;
import com.braintribe.model.securityservice.SecurityRequest;
import com.braintribe.model.securityservice.SimplifiedOpenUserSession;
import com.braintribe.model.securityservice.credentials.ExistingSessionCredentials;
import com.braintribe.model.securityservice.credentials.GrantedCredentials;
import com.braintribe.model.securityservice.credentials.TrustedCredentials;
import com.braintribe.model.securityservice.credentials.UserPasswordCredentials;
import com.braintribe.model.service.api.AuthorizableRequest;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.platform.reflex.security.processor.AuthenticationProcessor;
import hiconic.platform.reflex.security.processor.AuthorizingServiceInterceptor;
import hiconic.platform.reflex.security.processor.SecurityServiceProcessor;
import hiconic.platform.reflex.security.processor.SimpleSecurityServiceProcessor;
import hiconic.rx.module.api.service.ServiceDomainConfiguration;
import hiconic.rx.module.api.service.ServiceDomainConfigurations;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;

@Managed
public class SecurityRxModuleSpace implements RxModuleContract {
	@Import
	private RxPlatformContract platform;

	@Import
	private CredentialProcessorsSpace credentialProcessors;
	
	@Import
	private UserServicesSpace userServices;
	
	@Override
	public void configureServiceDomains(ServiceDomainConfigurations configurations) {
		ServiceDomainConfiguration configuration = configurations.byId("security");
		configuration.register(SecurityRequest.T, securityProcessor());
		configuration.register(SimplifiedOpenUserSession.T, simpleSecurityProcessor());
		configuration.register(AuthenticateCredentials.T, authenticationProcessor());
	}
	
	@Override
	public void registerCrossDomainInterceptors(InterceptorRegistry interceptorRegistry) {
		interceptorRegistry.registerInterceptor("auth").registerForType(AuthorizableRequest.T, authorizingServiceInterceptor());
	}
	
	@Managed
	private AuthorizingServiceInterceptor authorizingServiceInterceptor() {
		AuthorizingServiceInterceptor bean = new AuthorizingServiceInterceptor();
		return bean;
	}
	
	@Managed
	private AuthenticationProcessor authenticationProcessor() {
		AuthenticationProcessor bean = new AuthenticationProcessor();
		bean.register(ExistingSessionCredentials.T, credentialProcessors.existingSession());
		bean.register(GrantedCredentials.T, credentialProcessors.granted());
		bean.register(UserPasswordCredentials.T, credentialProcessors.userPassword());
		bean.register(TrustedCredentials.T, credentialProcessors.trusted());
		return bean;
	}
	
	@Managed
	private SecurityServiceProcessor securityProcessor() {
		SecurityServiceProcessor bean = new SecurityServiceProcessor();
		bean.setEvaluator(platform.evaluator());
		bean.setUserService(userServices.standardUserService());
		bean.setUserSessionService(userServices.userSessionService());
		return bean;
	}
	
	@Managed
	private SimpleSecurityServiceProcessor simpleSecurityProcessor() {
		SimpleSecurityServiceProcessor bean = new SimpleSecurityServiceProcessor();
		return bean;
	}
	
}