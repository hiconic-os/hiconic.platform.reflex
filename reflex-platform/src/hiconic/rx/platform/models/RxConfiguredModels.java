package hiconic.rx.platform.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import com.braintribe.cfg.Required;
import com.braintribe.common.attribute.AttributeContext;
import com.braintribe.model.generic.GMF;
import com.braintribe.model.generic.reflection.Model;
import com.braintribe.model.meta.GmMetaModel;
import com.braintribe.model.processing.meta.cmd.CmdResolver;
import com.braintribe.model.processing.meta.oracle.ModelOracle;
import com.braintribe.utils.lcd.LazyInitialized;

import hiconic.rx.module.api.service.ConfiguredModel;
import hiconic.rx.module.api.service.ConfiguredModels;
import hiconic.rx.module.api.service.ModelConfigurations;
import hiconic.rx.module.api.service.ModelReference;

public class RxConfiguredModels implements ConfiguredModels {
	private Map<String, RxConfiguredModel> models = new ConcurrentHashMap<>();
	private Map<String, RxPlatformModel> platformModels = new ConcurrentHashMap<>();
	private LazyInitialized<Map<GmMetaModel, List<AbstractRxConfiguredModel>>> dependersByModel = new LazyInitialized<>(this::indexDependers);
	private Supplier<AttributeContext> systemAttributeContextSupplier;
	private RxCmdResolverManager cmdResolverManager;
	
	@Required
	public void setSystemAttributeContextSupplier(Supplier<AttributeContext> systemAttributeContextSupplier) {
		this.systemAttributeContextSupplier = systemAttributeContextSupplier;
	}
	
	@Required
	public void setCmdResolverManager(RxCmdResolverManager cmdResolverManager) {
		this.cmdResolverManager = cmdResolverManager;
	}

	@Override
	public ConfiguredModel byName(String modelName) {
		RxConfiguredModel rxConfiguredModel = models.get(modelName);
		
		if (rxConfiguredModel != null)
			return rxConfiguredModel;
		
		return platformModels.computeIfAbsent(modelName, this::getPlatformModel);
	}
	
	private RxPlatformModel getPlatformModel(String modelName) {
		Model model = GMF.getTypeReflection().findModel(modelName);
		
		if (model == null)
			return null;
		
		GmMetaModel metaModel = model.getMetaModel();
		
		return new RxPlatformModel(this, metaModel);
	}
	
	@Override
	public ConfiguredModel byReference(ModelReference reference) {
		return byName(reference.modelName());
	}
	
	@Override
	public ConfiguredModel mainPersistenceModel() {
		return byReference(ModelConfigurations.mainPersistenceModelRef);
	}
	
	public RxConfiguredModel acquire(String modelName) {
		if (GMF.getTypeReflection().findModel(modelName) != null) 
			throw new IllegalArgumentException("Configured models must not have a platform model name: " + modelName);

		return (RxConfiguredModel)models.computeIfAbsent(modelName, n -> new RxConfiguredModel(this, n));
	}
	
	public RxConfiguredModel acquire(ModelReference modelReference) {
		return acquire(modelReference.modelName());
	}

	@Override
	public List<AbstractRxConfiguredModel> list() {
		return List.copyOf(models.values());
	}

	private Map<GmMetaModel, List<AbstractRxConfiguredModel>> indexDependers() {
		Map<GmMetaModel, List<AbstractRxConfiguredModel>> index = new HashMap<>();

		for (AbstractRxConfiguredModel configuredModel: list()) {
			configuredModel.modelOracle().getDependencies() //
				.includeSelf() //
				.transitive() //
				.asGmMetaModels() //
				.forEach(m -> index.computeIfAbsent(m, k -> new ArrayList<>()).add(configuredModel));
		}
		return index;
	}
	
	@Override
	public List<? extends ConfiguredModel> listDependers(GmMetaModel model) {
		return dependersByModel.get().getOrDefault(model, Collections.emptyList());
	}

	public CmdResolver systemCmdResolver(ModelOracle modelOracle) {
		return cmdResolver(systemAttributeContextSupplier.get(), modelOracle);
	}

	public CmdResolver cmdResolver(AttributeContext attributeContext, ModelOracle modelOracle) {
		return cmdResolverManager.cmdResolver(attributeContext, modelOracle);
	}

}
