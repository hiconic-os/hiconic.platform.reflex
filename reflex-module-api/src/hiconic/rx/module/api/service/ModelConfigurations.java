package hiconic.rx.module.api.service;

import com.braintribe.common.artifact.ArtifactReflection;

public interface ModelConfigurations {
	/**
	 * Acquires a {@link ModelConfiguration} with the given name as model name 
	 */
	ModelConfiguration byName(String modelName);

	/**
	 * Acquires a {@link ModelConfiguration} via {@link #extendedModel(String, ArtifactReflection) extendedModel("configured", modelArtifactReflection)} 
	 */
	ModelConfiguration configuredModel(ArtifactReflection modelArtifactReflection);
	
	/**
	 * Acquires a {@link ModelConfiguration} for a model named "${modelArtifactReflection.groupId()}:${prefix}-${modelArtifactReflection.artifactId()}"
	 * with the model of that artifact as a dependency
	 */
	ModelConfiguration extendedModel(String prefix, ArtifactReflection modelArtifactReflection);
}
