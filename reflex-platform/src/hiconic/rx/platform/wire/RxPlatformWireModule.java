package hiconic.rx.platform.wire;

import java.io.File;

import com.braintribe.wire.api.context.WireContextBuilder;
import com.braintribe.wire.api.module.WireTerminalModule;

import hiconic.rx.module.api.wire.RxPlatformContract;
import hiconic.rx.platform.ApplicationProperties;
import hiconic.rx.platform.wire.contract.RxPlatformConfigContract;
import hiconic.rx.platform.wire.space.RxPlatformSpace;

public class RxPlatformWireModule implements WireTerminalModule<RxPlatformContract> {
	
	private RxPlatformConfigContract config;
	
	public RxPlatformWireModule(File appDir, String[] cliArguments, ApplicationProperties properties) {
		super();
		
		config = new RxPlatformConfigContract() {
			
			@Override
			public ApplicationProperties properties() {
				return properties;
			}
			
			@Override
			public File appDir() {
				return appDir;
			}
			
			@Override
			public String[] cliArguments() {
				return cliArguments;
			}
		};
		
	}

	@Override
	public void configureContext(WireContextBuilder<?> contextBuilder) {
		WireTerminalModule.super.configureContext(contextBuilder);
		contextBuilder.bindContract(RxPlatformContract.class, RxPlatformSpace.class);
		contextBuilder.bindContract(RxPlatformConfigContract.class, config);
	}
	
}
