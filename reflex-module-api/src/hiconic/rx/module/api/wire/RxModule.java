package hiconic.rx.module.api.wire;

import com.braintribe.wire.api.context.WireContextBuilder;
import com.braintribe.wire.api.module.WireTerminalModule;
import com.braintribe.wire.api.tools.Generics;

/**
 * @see RxModuleContract
 */
public interface RxModule<M extends RxModuleContract> extends WireTerminalModule<RxModuleContract> {

	/**
	 * Binds {@link RxExportContract}s to the actual space.
	 *
	 * @see RxExportContract
	 * @see Exports
	 */
	@SuppressWarnings("unused")
	default void bindExports(Exports exports) {
		// implement if needed
	}

	default String moduleName() {
		return getClass().getName();
	}

	@Override
	default void configureContext(WireContextBuilder<?> contextBuilder) {
		WireTerminalModule.super.configureContext(contextBuilder);

		Class<M> moduleSpaceClass = moduleSpaceClass();
		contextBuilder.bindContract(RxModuleContract.class, moduleSpaceClass);
	}

	default Class<M> moduleSpaceClass() {
		return (Class<M>) Generics.getGenericsParameter(getClass(), RxModule.class, "M");
	}

}
