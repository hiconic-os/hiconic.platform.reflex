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
	private LazyInitialized<Map<GmMetaModel, List<RxConfiguredModel>>> dependersByModel = new LazyInitialized<>(this::indexDependers);
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
	public RxConfiguredModel byName(String domainId) {
		return models.get(domainId);
	}
	
	@Override
	public RxConfiguredModel byReference(ModelReference reference) {
		return byName(reference.modelName());
	}
	
	@Override
	public ConfiguredModel mainPersistenceModel() {
		return byReference(ModelConfigurations.mainPersistenceModelRef);
	}
	
	public RxConfiguredModel acquire(String modelName) {
		return models.computeIfAbsent(modelName, n -> new RxConfiguredModel(this, n));
	}
	
	public RxConfiguredModel acquire(ModelReference modelReference) {
		return acquire(modelReference.modelName());
	}

	@Override
	public List<RxConfiguredModel> list() {
		return List.copyOf(models.values());
	}

	private Map<GmMetaModel, List<RxConfiguredModel>> indexDependers() {
		Map<GmMetaModel, List<RxConfiguredModel>> index = new HashMap<>();

		for (RxConfiguredModel configuredModel: list()) {
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
