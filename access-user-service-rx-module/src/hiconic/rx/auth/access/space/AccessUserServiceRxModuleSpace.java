package hiconic.rx.auth.access.space;

import static com.braintribe.gm.model.reason.UnsatisfiedMaybeTunneling.getOrTunnel;

import com.braintribe.model.user.Group;
import com.braintribe.model.user.Role;
import com.braintribe.model.user.User;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.access.model.configuration.AccessConfiguration;
import hiconic.rx.access.module.api.AccessContract;
import hiconic.rx.auth.access.model.configuration.AccessUserServiceConfiguration;
import hiconic.rx.auth.access.processing.AccessBasedUserService;
import hiconic.rx.module.api.config.RxPlatformConfigurator;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;
import hiconic.rx.security.api.SecurityExtensionContract;
import hiconic.rx.security.api.UserService;

/**
 * Brings a {@link UserService} implementation based on an Access.
 * <p>
 * The access has to be configured in a standard way via {@link AccessConfiguration#getAccesses()}, its data model must include the user-model
 * ({@link User}, {@link Role}, {@link Group}), and its id must be given via {@link AccessUserServiceConfiguration#getAuthAccessId()}.
 */
@Managed
public class AccessUserServiceRxModuleSpace implements RxModuleContract {

	@Import
	private RxPlatformContract platform;

	@Import
	private SecurityExtensionContract securityExtension;

	@Import
	private AccessContract access;

	@Override
	public void configurePlatform(RxPlatformConfigurator configurator) {
		securityExtension.registerUserService(authAccessUserService());
	}

	private AccessBasedUserService authAccessUserService() {
		AccessBasedUserService bean = new AccessBasedUserService();
		bean.setAuthAccessId(configuration().getAuthAccessId());
		bean.setSystemSessionFactory(access.systemSessionFactory());

		return bean;
	}

	@Managed
	private AccessUserServiceConfiguration configuration() {
		return getOrTunnel(platform.configuration().readConfig(AccessUserServiceConfiguration.T));
	}

}