package hiconic.rx.platform.models;

import com.braintribe.cfg.Required;
import com.braintribe.common.artifact.ArtifactReflection;

import hiconic.rx.module.api.service.ModelConfigurations;

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
	public RxConfiguredModel configuredModel(ArtifactReflection modelArtifactReflection) {
		return extendedModel("configured", modelArtifactReflection);
	}

	@Override
	public RxConfiguredModel extendedModel(String prefix, ArtifactReflection modelArtifactReflection) {
		RxConfiguredModel configuredModel = byName(modelArtifactReflection.groupId() + ":" + prefix + "-" + modelArtifactReflection.name());
		// even if this called multiple times it will be added only once
		configuredModel.addModel(modelArtifactReflection);
		return configuredModel;
	}

}
