package hiconic.rx.platform.cli.wire;

import com.braintribe.wire.api.context.WireContextBuilder;
import com.braintribe.wire.api.module.WireTerminalModule;

import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.platform.cli.wire.space.CliRxModuleSpace;

public enum CliRxModule implements WireTerminalModule<RxModuleContract> {
	INSTANCE;

	@Override
	public void configureContext(WireContextBuilder<?> contextBuilder) {
		WireTerminalModule.super.configureContext(contextBuilder);
		contextBuilder.bindContract(RxModuleContract.class, CliRxModuleSpace.class);
	}
}
