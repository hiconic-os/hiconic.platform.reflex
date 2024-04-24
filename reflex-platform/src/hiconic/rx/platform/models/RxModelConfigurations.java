package hiconic.rx.platform.models;

import com.braintribe.cfg.Required;
import com.braintribe.common.artifact.ArtifactReflection;

import hiconic.rx.module.api.service.ModelConfiguration;
import hiconic.rx.module.api.service.ModelConfigurations;
import hiconic.rx.module.api.service.ModelReference;

public class RxModelConfigurations implements ModelConfigurations {
	private RxConfiguredModels configuredModels;

	@Required
	public void setConfiguredModels(RxConfiguredModels configuredModels) {
		this.configuredModels = configuredModels;
	}
	
	public RxConfiguredModels getConfiguredModels() {
		return configuredModels;
	}
	
	@Override
	public RxConfiguredModel byName(String modelName) {
		return configuredModels.acquire(modelName);
	}
	
	@Override
	public RxConfiguredModel byReference(ModelReference reference) {
		return configuredModels.acquire(reference.modelName());
	}

	@Override
	public RxConfiguredModel configuredModel(ArtifactReflection baseModel) {
		return extendedModel("configured", baseModel);
	}

	@Override
	public RxConfiguredModel extendedModel(String prefix, ArtifactReflection baseModel) {
		RxConfiguredModel configuredModel = byName(baseModel.groupId() + ":" + prefix + "-" + baseModel.name());
		// even if this called multiple times it will be added only once
		configuredModel.addModel(baseModel);
		return configuredModel;
	}
	
	@Override
	public RxConfiguredModel extendedModel(ModelReference reference, ArtifactReflection baseModel) {
		RxConfiguredModel configuredModel = byReference(reference);
		configuredModel.addModel(baseModel);
		return configuredModel;
	}
	
	@Override
	public ModelConfiguration mainPersistenceModel() {
		return byReference(mainPersistenceModelRef);
	}



}
