// ============================================================================
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ============================================================================
package hiconic.platform.reflex.security.wire.space;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import com.braintribe.exception.Exceptions;
import com.braintribe.gm.model.reason.UnsatisfiedMaybeTunneling;
import com.braintribe.model.processing.securityservice.api.UserSessionScoping;
import com.braintribe.model.processing.securityservice.commons.provider.StaticUserSessionHolder;
import com.braintribe.model.processing.securityservice.commons.scope.StandardUserSessionScoping;
import com.braintribe.model.processing.service.api.InterceptorRegistry;
import com.braintribe.model.processing.service.common.context.UserSessionStack;
import com.braintribe.model.securityservice.AuthenticateCredentials;
import com.braintribe.model.securityservice.SecurityRequest;
import com.braintribe.model.securityservice.SimplifiedOpenUserSession;
import com.braintribe.model.securityservice.credentials.ExistingSessionCredentials;
import com.braintribe.model.securityservice.credentials.GrantedCredentials;
import com.braintribe.model.securityservice.credentials.TrustedCredentials;
import com.braintribe.model.securityservice.credentials.UserPasswordCredentials;
import com.braintribe.model.service.api.AuthorizableRequest;
import com.braintribe.model.user.Role;
import com.braintribe.model.user.User;
import com.braintribe.model.usersession.UserSession;
import com.braintribe.model.usersession.UserSessionType;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.platform.reflex.security.processor.AuthenticationProcessor;
import hiconic.platform.reflex.security.processor.AuthorizingServiceInterceptor;
import hiconic.platform.reflex.security.processor.SecurityServiceProcessor;
import hiconic.platform.reflex.security.processor.SimpleSecurityServiceProcessor;
import hiconic.platform.reflex.security.processor.SystemUserScopingWorkerAspect;
import hiconic.rx.module.api.service.ServiceDomainConfiguration;
import hiconic.rx.module.api.service.ServiceDomainConfigurations;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformConfigurator;
import hiconic.rx.module.api.wire.RxPlatformContract;
import hiconic.rx.security.api.SecurityContract;
import hiconic.rx.security.api.SecurityServiceDomain;
import hiconic.rx.security.model.configuration.SecurityConfiguration;

@Managed
public class SecurityRxModuleSpace implements RxModuleContract, SecurityContract {

	@Import
	private RxPlatformContract platform;

	@Import
	private CredentialProcessorsSpace credentialProcessors;

	@Import
	private UserServicesSpace userServices;

	@Override
	public void configureServiceDomains(ServiceDomainConfigurations configurations) {
		ServiceDomainConfiguration configuration = configurations.byId(SecurityServiceDomain.security);
		configuration.bindRequest(SecurityRequest.T, this::securityProcessor);
		configuration.bindRequest(SimplifiedOpenUserSession.T, this::simpleSecurityProcessor);
		configuration.bindRequest(AuthenticateCredentials.T, this::authenticationProcessor);
	}

	@Override
	public void registerCrossDomainInterceptors(InterceptorRegistry interceptorRegistry) {
		// Uses symbols for auth interceptor
		interceptorRegistry.registerInterceptor("auth").registerForType(AuthorizableRequest.T, authorizingServiceInterceptor());
	}

	@Override
	public void configurePlatform(RxPlatformConfigurator configurator) {
		// Uses symbols for system-user-scoping worker interceptor
		configurator.workerAspectRegistry().register("system-user-scoping", systemUserScopingWorkerAspect());
	}

	@Managed
	private SystemUserScopingWorkerAspect systemUserScopingWorkerAspect() {
		SystemUserScopingWorkerAspect bean = new SystemUserScopingWorkerAspect();
		bean.setUserSessionScoping(userSessionScoping());

		return bean;
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

	@Override
	public UserSessionScoping userSessionScoping() {
		StandardUserSessionScoping bean = new StandardUserSessionScoping();
		bean.setRequestEvaluator(platform.evaluator());
		bean.setDefaultUserSessionSupplier(userSessionProvider());
		bean.setUserSessionStack(userSessionStack());
		return bean;
	}

	@Managed
	private StaticUserSessionHolder userSessionProvider() {
		StaticUserSessionHolder bean = new StaticUserSessionHolder();
		bean.setUserSession(internalUserSession(internalUser()));
		return bean;
	}

	@Managed
	private User internalUser() {
		User bean = User.T.create();
		bean.setId("internal");
		bean.setName("internal");

		String roleName = internalRole();
		Role role = Role.T.create();
		role.setId(roleName);
		role.setName(roleName);
		bean.getRoles().add(role);

		return bean;
	}

	public UserSession internalUserSession(User user) {
		UserSession bean = UserSession.T.create();

		Set<String> effectiveRoles = new HashSet<>();
		effectiveRoles.add("$all");
		effectiveRoles.add("$user-" + user.getName());
		for (Role userRole : user.getRoles())
			effectiveRoles.add(userRole.getName());

		Date now = new Date();

		bean.setSessionId(newInternalUserSessionId());
		bean.setType(UserSessionType.internal);
		bean.setCreationInternetAddress("0:0:0:0:0:0:0:1");
		bean.setCreationDate(now);
		bean.setLastAccessedDate(now);
		bean.setUser(user);
		bean.setEffectiveRoles(effectiveRoles);

		return bean;
	}

	private String newInternalUserSessionId() {
		try {
			return userServices.userSessionIdFactory().apply(UserSessionType.internal);
		} catch (Exception e) {
			throw Exceptions.unchecked(e, "Failed to generate an user session id");
		}
	}

	@Managed
	private UserSessionStack userSessionStack() {
		UserSessionStack bean = new UserSessionStack();

		return bean;
	}

	@Override
	public Set<String> adminRoles() {
		return securityConfiguration().getAdminRoles();
	}

	@Override
	public String internalRole() {
		return securityConfiguration().getInternalRole();
	}

	@Override
	@Managed
	public Set<String> adminAndInternalRoles() {
		Set<String> bean = new HashSet<String>(adminRoles());
		bean.add(internalRole());
		return bean;
	}

	@Managed
	private SecurityConfiguration securityConfiguration() {
		return UnsatisfiedMaybeTunneling.getOrTunnel(platform.readConfig(SecurityConfiguration.T));
	}
}