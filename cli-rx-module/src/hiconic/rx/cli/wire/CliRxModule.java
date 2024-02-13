package hiconic.rx.cli.wire;

import com.braintribe.wire.api.context.WireContextBuilder;
import com.braintribe.wire.api.module.WireTerminalModule;

import hiconic.rx.cli.wire.space.CliRxModuleSpace;
import hiconic.rx.module.api.wire.RxModuleContract;

public enum CliRxModule implements WireTerminalModule<RxModuleContract> {
	INSTANCE;

	@Override
	public void configureContext(WireContextBuilder<?> contextBuilder) {
		WireTerminalModule.super.configureContext(contextBuilder);
		contextBuilder.bindContract(RxModuleContract.class, CliRxModuleSpace.class);
	}
}
