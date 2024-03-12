package hiconic.rx.platform.wire;

import java.io.File;

import com.braintribe.wire.api.context.WireContextBuilder;
import com.braintribe.wire.api.module.WireTerminalModule;

import hiconic.rx.module.api.wire.RxContractSpaceResolverConfigurator;
import hiconic.rx.module.api.wire.RxPlatformContract;
import hiconic.rx.module.api.wire.RxProcessLaunchContract;
import hiconic.rx.platform.conf.ApplicationProperties;
import hiconic.rx.platform.conf.SystemProperties;
import hiconic.rx.platform.loading.RxConfigurableContractSpaceResolver;
import hiconic.rx.platform.wire.contract.RxPlatformConfigContract;
import hiconic.rx.platform.wire.space.RxPlatformSpace;

public class RxPlatformWireModule implements WireTerminalModule<RxPlatformContract> {
	
	private RxPlatformConfigContract config;
	private final RxConfigurableContractSpaceResolver configurableContractSpaceResolver = new RxConfigurableContractSpaceResolver();
	
	public RxPlatformWireModule(String[] cliArguments, ApplicationProperties applicationProperties, SystemProperties systemProperties) {
		super();
		
		config = new RxPlatformConfigContract() {
			
			@Override
			public ApplicationProperties properties() {
				return applicationProperties;
			}
			
			@Override
			public File appDir() {
				return systemProperties.appDir();
			}
			
			@Override
			public String launchScriptName() {
				return systemProperties.launchScript();
			}
			
			@Override
			public String[] cliArguments() {
				return cliArguments;
			}
			
			@Override
			public RxContractSpaceResolverConfigurator contractSpaceResolverConfigurator() {
				return configurableContractSpaceResolver;
			}
		};
	}

	@Override
	public void configureContext(WireContextBuilder<?> contextBuilder) {
		WireTerminalModule.super.configureContext(contextBuilder);
		contextBuilder.bindContract(RxPlatformContract.class, RxPlatformSpace.class);
		contextBuilder.bindContract(RxProcessLaunchContract.class, RxPlatformSpace.class);
		contextBuilder.bindContract(RxPlatformConfigContract.class, config);
		contextBuilder.bindContracts(configurableContractSpaceResolver);
	}
	
}
