package hiconic.rx.module.api.service;

import java.util.function.Consumer;

import com.braintribe.common.artifact.ArtifactReflection;
import com.braintribe.model.generic.reflection.Model;
import com.braintribe.model.meta.GmMetaModel;
import com.braintribe.model.processing.meta.editor.ModelMetaDataEditor;
import com.braintribe.model.processing.service.api.ServiceRegistry;

public interface ModelConfiguration extends ServiceRegistry, ConfiguredModelReference {
	
	/**
	 * @param modelName
	 *            A fully qualified model name, i.e. "${groupId}:${artifactId}"``
	 */
	void addModelByName(String modelName);

	/**
	 * @param modelArtifactReflection
	 *            {@link ArtifactReflection} for a model artifact.
	 */
	void addModel(ArtifactReflection modelArtifactReflection);
	
	void addModel(GmMetaModel gmModel);
	
	void addModel(Model model);
	
	void addModel(ConfiguredModelReference modelReference);
	
	void configureModel(Consumer<ModelMetaDataEditor> configurer);
	
}
