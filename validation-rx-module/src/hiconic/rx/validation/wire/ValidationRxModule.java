package hiconic.rx.validation.wire;

import com.braintribe.wire.api.context.WireContextBuilder;
import com.braintribe.wire.api.module.WireTerminalModule;

import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.validation.wire.space.ValidationRxModuleSpace;

public enum ValidationRxModule implements WireTerminalModule<RxModuleContract> {
	INSTANCE;

	@Override
	public void configureContext(WireContextBuilder<?> contextBuilder) {
		WireTerminalModule.super.configureContext(contextBuilder);
		contextBuilder.bindContract(RxModuleContract.class, ValidationRxModuleSpace.class);
	}
}
