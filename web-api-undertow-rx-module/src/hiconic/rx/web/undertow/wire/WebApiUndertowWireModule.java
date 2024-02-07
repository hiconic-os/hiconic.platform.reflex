package hiconic.rx.web.undertow.wire;

import com.braintribe.wire.api.context.WireContextBuilder;
import com.braintribe.wire.api.module.WireTerminalModule;

import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.web.undertow.wire.space.WebApiUndertowRxModuleSpace;

public enum WebApiUndertowWireModule implements WireTerminalModule<RxModuleContract> {
	INSTANCE;

	@Override
	public void configureContext(WireContextBuilder<?> contextBuilder) {
		WireTerminalModule.super.configureContext(contextBuilder);
		contextBuilder.bindContract(RxModuleContract.class, WebApiUndertowRxModuleSpace.class);
	}
}
