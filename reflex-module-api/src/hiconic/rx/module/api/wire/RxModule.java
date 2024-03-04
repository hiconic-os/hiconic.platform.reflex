package hiconic.rx.module.api.wire;

import com.braintribe.wire.api.context.WireContextBuilder;
import com.braintribe.wire.api.module.WireTerminalModule;
import com.braintribe.wire.api.tools.Generics;

public interface RxModule<M extends RxModuleContract> extends WireTerminalModule<RxModuleContract> {

	// better?
	default void bindExports(Exports exports) { }
	
	@Override
	default void configureContext(WireContextBuilder<?> contextBuilder) {
		WireTerminalModule.super.configureContext(contextBuilder);
		
		Class<M> moduleSpaceClass = moduleSpaceClass();
		contextBuilder.bindContract(RxModuleContract.class, moduleSpaceClass);
	}
	
	default Class<M> moduleSpaceClass() {
		return (Class<M>)Generics.getGenericsParameter(getClass(), RxModule.class, "M");
	}

}
