package hiconic.rx.module.api.service;

import com.braintribe.common.artifact.ArtifactReflection;

public interface ModelConfigurations {
	ModelReference mainPersistenceModelRef = ModelReference.of("rx-platform:configured-main-persistence-model");
	
	/**
	 * Acquires a {@link ModelConfiguration} with the given name as model name 
	 */
	ModelConfiguration byName(String modelName);
	
	ModelConfiguration byReference(ModelReference reference);
	
	/**
	 * Acquires a {@link ModelConfiguration} via {@link #byReference(String)} using {@link #mainPersistenceModelRef}  
	 */
	ModelConfiguration mainPersistenceModel();

	/**
	 * Acquires a {@link ModelConfiguration} via {@link #extendedModel(String, ArtifactReflection) extendedModel("configured", baseModel)} 
	 */
	ModelConfiguration configuredModel(ArtifactReflection baseModel);
	
	/**
	 * Acquires a {@link ModelConfiguration} for a model named "${baseModel.groupId()}:${prefix}-${baseModel.artifactId()}"
	 * with the model of that artifact as a dependency
	 */
	ModelConfiguration extendedModel(String prefix, ArtifactReflection baseModel);
	
	ModelConfiguration extendedModel(ModelReference reference, ArtifactReflection baseModel);
}
