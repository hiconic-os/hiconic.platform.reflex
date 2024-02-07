package hiconic.rx.platform.wire;

import java.io.File;
import java.util.List;

import com.braintribe.gm.service.wire.common.CommonServiceProcessingWireModule;
import com.braintribe.wire.api.context.WireContextBuilder;
import com.braintribe.wire.api.module.WireModule;
import com.braintribe.wire.api.module.WireTerminalModule;
import com.braintribe.wire.api.util.Lists;

import hiconic.rx.module.api.wire.RxPlatformContract;
import hiconic.rx.platform.wire.contract.RxPlatformConfigContract;
import hiconic.rx.platform.wire.space.RxPlatformSpace;

public class RxPlatformWireModule implements WireTerminalModule<RxPlatformContract> {
	
	private File appDir;
	private RxPlatformConfigContract config;
	
	public RxPlatformWireModule(File appDir) {
		super();
		this.appDir = appDir;
		
		config = new RxPlatformConfigContract() {
			
			@Override
			public File appDir() {
				return appDir;
			}
		};
		
	}

	@Override
	public void configureContext(WireContextBuilder<?> contextBuilder) {
		WireTerminalModule.super.configureContext(contextBuilder);
		contextBuilder.bindContract(RxPlatformContract.class, RxPlatformSpace.class);
		contextBuilder.bindContract(RxPlatformConfigContract.class, config);
	}
	
	@Override
	public List<WireModule> dependencies() {
		return Lists.list(CommonServiceProcessingWireModule.INSTANCE);
	}
	
}
