package hiconic.rx.platform.models;

import com.braintribe.common.attribute.AttributeContext;
import com.braintribe.model.processing.meta.cmd.CmdResolver;
import com.braintribe.model.processing.meta.oracle.ModelOracle;
import com.braintribe.utils.collection.impl.AttributeContexts;
import com.braintribe.utils.lcd.LazyInitialized;

import hiconic.rx.module.api.service.ConfiguredModel;

public abstract class AbstractRxConfiguredModel implements ConfiguredModel {

	protected final LazyInitialized<ModelOracle> lazyModelOracle = new LazyInitialized<>(this::buildModelOracle);
	protected final LazyInitialized<CmdResolver> lazySystemCmdResolver = new LazyInitialized<>(this::buildSystemCmdResolver);
	protected final RxConfiguredModels configuredModels;

	protected AbstractRxConfiguredModel(RxConfiguredModels configuredModels) {
		this.configuredModels = configuredModels;
		
	}
	
	protected abstract ModelOracle buildModelOracle();
	
	private CmdResolver buildSystemCmdResolver() {
		return configuredModels.systemCmdResolver(modelOracle());
	}
	
	@Override
	public CmdResolver systemCmdResolver() {
		return lazySystemCmdResolver.get();
	}
	
	@Override
	public CmdResolver cmdResolver(AttributeContext attributeContext) {
		return configuredModels.cmdResolver(attributeContext, modelOracle());
	}
	
	@Override
	public CmdResolver contextCmdResolver() {
		return cmdResolver(AttributeContexts.peek());
	}

	@Override
	public ModelOracle modelOracle() {
		return lazyModelOracle.get();
	}

}
