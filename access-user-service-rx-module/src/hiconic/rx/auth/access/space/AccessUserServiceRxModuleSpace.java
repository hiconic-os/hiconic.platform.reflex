package hiconic.rx.auth.access.space;

import static com.braintribe.gm.model.reason.UnsatisfiedMaybeTunneling.getOrTunnel;

import com.braintribe.model.crypto.configuration.hashing.HashingConfiguration;
import com.braintribe.model.meta.data.crypto.PropertyCrypting;
import com.braintribe.model.processing.aspect.crypto.CryptoAspect;
import com.braintribe.model.user.Group;
import com.braintribe.model.user.Role;
import com.braintribe.model.user.User;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.access.model.configuration.AccessConfiguration;
import hiconic.rx.access.module.api.AccessContract;
import hiconic.rx.access.module.api.AccessDataModelConfiguration;
import hiconic.rx.auth.access.model.configuration.AccessUserServiceConfiguration;
import hiconic.rx.auth.access.processing.AccessBasedUserService;
import hiconic.rx.db.module.api.DatabaseContract;
import hiconic.rx.locking.api.LockingContract;
import hiconic.rx.module.api.config.RxPlatformConfigurator;
import hiconic.rx.module.api.service.ModelConfigurations;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;
import hiconic.rx.security.api.SecurityContract;
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
	private SecurityContract security;

	@Import
	private AccessContract access;

	@Import
	private DatabaseContract database;

	@Import
	private LockingContract locking;

	@Override
	public void configureModels(ModelConfigurations configurations) {
		String authAccessId = configuration().getAuthAccessId();
		access.protectSystemAccess(authAccessId);

		AccessDataModelConfiguration dataModel = access.accessModelConfigurations().dataModelConfiguration(authAccessId);
		dataModel.configureModel(editor -> editor.onEntityType(User.T)
				.addPropertyMetaData(User.password, userPasswordPropertyCrypting()));
		dataModel.bindAspect("password-crypting").bind(this::cryptoAspect);
	}

	@Override
	public void configurePlatform(RxPlatformConfigurator configurator) {
		securityExtension.registerUserService(authAccessUserService());
	}

	private AccessBasedUserService authAccessUserService() {
		AccessBasedUserService bean = new AccessBasedUserService();
		bean.setAuthAccessId(configuration().getAuthAccessId());
		bean.setSystemSessionFactory(access.systemSessionFactory());
		bean.setPasswordHashing(security.passwordHashing());
		bean.setUserSessionInvalidation(security.userSessionInvalidation());
		bean.setProvisioningStateDataSource(getOrTunnel(database.dataSource(configuration().getProvisioningStateDatabaseId())));
		bean.setProvisioningLocking(locking.locking());
		bean.setNodeId(platform.application().nodeId());

		return bean;
	}

	@Managed
	private CryptoAspect cryptoAspect() {
		CryptoAspect bean = new CryptoAspect();
		bean.setCryptorProvider(security.cryptorProvider());
		return bean;
	}

	@Managed
	private PropertyCrypting userPasswordPropertyCrypting() {
		PropertyCrypting bean = PropertyCrypting.T.create();
		bean.setCryptoConfiguration(userPasswordHashingConfiguration());
		return bean;
	}

	@Managed
	private HashingConfiguration userPasswordHashingConfiguration() {
		HashingConfiguration bean = HashingConfiguration.T.create();
		bean.setAlgorithm("SHA-256");
		bean.setEnableRandomSalt(true);
		bean.setRandomSaltSize(16);
		return bean;
	}

	@Managed
	private AccessUserServiceConfiguration configuration() {
		return getOrTunnel(platform.configuration().readConfig(AccessUserServiceConfiguration.T));
	}

}
