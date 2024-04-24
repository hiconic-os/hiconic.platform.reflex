package hiconic.rx.platform.models;

import java.util.function.Consumer;

import com.braintribe.common.attribute.AttributeContext;
import com.braintribe.gm._RootModel_;
import com.braintribe.model.meta.GmMetaModel;
import com.braintribe.model.processing.meta.cmd.CmdResolver;
import com.braintribe.model.processing.meta.editor.BasicModelMetaDataEditor;
import com.braintribe.model.processing.meta.editor.ModelMetaDataEditor;
import com.braintribe.model.processing.meta.oracle.BasicModelOracle;
import com.braintribe.model.processing.meta.oracle.ModelOracle;
import com.braintribe.utils.collection.impl.AttributeContexts;
import com.braintribe.utils.lcd.LazyInitialized;

import hiconic.rx.module.api.service.ConfiguredModel;
import hiconic.rx.module.api.service.ModelConfiguration;

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
