package hiconic.rx.demo.app.wire;

import com.braintribe.wire.api.context.WireContextBuilder;
import com.braintribe.wire.api.module.WireTerminalModule;

import hiconic.rx.demo.app.wire.space.ReflexDemoAppRxModuleSpace;
import hiconic.rx.module.api.wire.RxModuleContract;

public enum ReflexDemoAppWireModule implements WireTerminalModule<RxModuleContract> {
	INSTANCE;

	@Override
	public void configureContext(WireContextBuilder<?> contextBuilder) {
		WireTerminalModule.super.configureContext(contextBuilder);
		contextBuilder.bindContract(RxModuleContract.class, ReflexDemoAppRxModuleSpace.class);
	}
}
