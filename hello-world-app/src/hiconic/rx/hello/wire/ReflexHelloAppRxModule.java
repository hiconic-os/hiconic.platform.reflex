package hiconic.rx.hello.wire;

import com.braintribe.wire.api.context.WireContextBuilder;
import com.braintribe.wire.api.module.WireTerminalModule;

import hiconic.rx.hello.wire.space.ReflexHelloAppRxModuleSpace;
import hiconic.rx.module.api.wire.RxModuleContract;

public enum ReflexHelloAppRxModule implements WireTerminalModule<RxModuleContract> {
	INSTANCE;

	@Override
	public void configureContext(WireContextBuilder<?> contextBuilder) {
		WireTerminalModule.super.configureContext(contextBuilder);
		contextBuilder.bindContract(RxModuleContract.class, ReflexHelloAppRxModuleSpace.class);
	}
}
