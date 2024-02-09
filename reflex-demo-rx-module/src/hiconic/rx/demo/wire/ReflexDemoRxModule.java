package hiconic.rx.demo.wire;

import com.braintribe.wire.api.context.WireContextBuilder;
import com.braintribe.wire.api.module.WireTerminalModule;

import hiconic.rx.demo.wire.space.ReflexDemoRxModuleSpace;
import hiconic.rx.module.api.wire.RxModuleContract;

public enum ReflexDemoRxModule implements WireTerminalModule<RxModuleContract> {
	INSTANCE;

	@Override
	public void configureContext(WireContextBuilder<?> contextBuilder) {
		WireTerminalModule.super.configureContext(contextBuilder);
		contextBuilder.bindContract(RxModuleContract.class, ReflexDemoRxModuleSpace.class);
	}
}
