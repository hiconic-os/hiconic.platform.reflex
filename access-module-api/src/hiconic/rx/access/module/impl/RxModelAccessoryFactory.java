package hiconic.rx.access.module.impl;

import com.braintribe.model.meta.GmMetaModel;
import com.braintribe.model.processing.meta.cmd.CmdResolver;
import com.braintribe.model.processing.meta.oracle.ModelOracle;
import com.braintribe.model.processing.session.api.managed.ManagedGmSession;
import com.braintribe.model.processing.session.api.managed.ModelAccessory;
import com.braintribe.model.processing.session.api.managed.ModelAccessoryFactory;

import hiconic.rx.access.module.api.AccessContract;
import hiconic.rx.access.module.api.AccessDomain;
import hiconic.rx.module.api.service.ConfiguredModel;
import hiconic.rx.module.api.service.ServiceDomain;
import hiconic.rx.module.api.wire.RxPlatformContract;

/**
 * ModelAccessoryFactory backed by {@link RxPlatformContract} and {@link AccessContract}, used for backwards compatibility with Cortex platform.
 * 
 * @author peter.gazdik
 */
public class RxModelAccessoryFactory implements ModelAccessoryFactory {

	private final RxPlatformContract platform;
	private final AccessContract access;
	private final boolean system;

	private RxModelAccessoryFactory(RxPlatformContract platform, AccessContract access, boolean system) {
		this.platform = platform;
		this.access = access;
		this.system = system;
	}

	public static RxModelAccessoryFactory createSystemMaf(RxPlatformContract platform, AccessContract access) {
		return new RxModelAccessoryFactory(platform, access, true);
	}

	public static RxModelAccessoryFactory createContextMaf(RxPlatformContract platform, AccessContract access) {
		return new RxModelAccessoryFactory(platform, access, false);
	}

	//
	// getForModel
	//

	@Override
	public ModelAccessory getForModel(String modelName) {
		ConfiguredModel configuredModel = platform.configuredModels().byName(modelName);
		if (configuredModel == null)
			throw new IllegalArgumentException("No configured model found with name:  [" + modelName + "]");
		return new ConfiguredModelModelAccessory(configuredModel);
	}

	//
	// getForAccess
	//

	@Override
	public ModelAccessory getForAccess(String accessId) {
		AccessDomain accessDomain = access.accessDomains().byId(accessId);
		if (accessDomain == null)
			throw new IllegalArgumentException("No access domain found with id:  [" + accessId + "]");
		return new ConfiguredModelModelAccessory(accessDomain.configuredDataModel());
	}

	class ConfiguredModelModelAccessory implements ModelAccessory {
		private final ConfiguredModel configuradModel;

		public ConfiguredModelModelAccessory(ConfiguredModel configuradModel) {
			this.configuradModel = configuradModel;
		}
		@Override
		public CmdResolver getCmdResolver() {
			return system ? configuradModel.systemCmdResolver() : configuradModel.contextCmdResolver();
		}
		@Override
		public ManagedGmSession getModelSession() {
			throw new UnsupportedOperationException("Method 'RxModelAccessoryFactory.AccessDomainModelAccessory.getModelSession' is not supported!");
		}
		@Override
		public GmMetaModel getModel() {
			return getOracle().getGmMetaModel();
		}
		@Override
		public ModelOracle getOracle() {
			return configuradModel.modelOracle();
		}
	}

	//
	// getForServiceDomain
	//

	@Override
	public ModelAccessory getForServiceDomain(String serviceDomainId) {
		ServiceDomain serviceDomain = platform.serviceDomains().byId(serviceDomainId);
		if (serviceDomain == null)
			throw new IllegalArgumentException("No service domain found with id:  [" + serviceDomainId + "]");
		return new ServiceDomainModelAccessory(serviceDomain);
	}

	class ServiceDomainModelAccessory implements ModelAccessory {
		private final ServiceDomain serviceDomain;

		public ServiceDomainModelAccessory(ServiceDomain serviceDomain) {
			this.serviceDomain = serviceDomain;
		}
		@Override
		public CmdResolver getCmdResolver() {
			return system ? serviceDomain.systemCmdResolver() : serviceDomain.contextCmdResolver();
		}
		@Override
		public ManagedGmSession getModelSession() {
			throw new UnsupportedOperationException("Method 'RxModelAccessoryFactory.ServiceDomainModelAccessory.getModelSession' is not supported!");
		}
		@Override
		public GmMetaModel getModel() {
			return getOracle().getGmMetaModel();
		}
		@Override
		public ModelOracle getOracle() {
			return serviceDomain.modelOracle();
		}
	}

}
