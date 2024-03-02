package hiconic.rx.module.api.wire;

import java.util.Collections;
import java.util.List;

import com.braintribe.wire.api.context.WireContextBuilder;
import com.braintribe.wire.api.module.WireTerminalModule;
import com.braintribe.wire.api.tools.Generics;

public interface RxModule<M extends RxModuleContract> extends WireTerminalModule<RxModuleContract> {

	default List<? extends RxModule<?>> extending() {
		return Collections.emptyList();
	}
	
	@Override
	default void configureContext(WireContextBuilder<?> contextBuilder) {
		WireTerminalModule.super.configureContext(contextBuilder);
		
		Class<M> moduleSpaceClass = (Class<M>)Generics.getGenericsParameter(getClass(), RxModule.class, "M");
		contextBuilder.bindContract(RxModuleContract.class, moduleSpaceClass);
	}

}
