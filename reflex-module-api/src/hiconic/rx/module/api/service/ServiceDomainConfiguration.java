package hiconic.rx.module.api.service;

import java.util.function.Consumer;
import java.util.function.Supplier;

import com.braintribe.common.artifact.ArtifactReflection;
import com.braintribe.model.generic.reflection.Model;
import com.braintribe.model.meta.GmMetaModel;
import com.braintribe.model.processing.meta.editor.ModelMetaDataEditor;
import com.braintribe.model.processing.service.api.ServiceRegistry;
import com.braintribe.model.service.api.ServiceRequest;

public interface ServiceDomainConfiguration extends ServiceRegistry {

	/**
	 * @param modelName
	 *            A fully qualified model name, i.e. "${groupId}:${artifactId}"``
	 */
	ServiceDomainConfiguration addModelByName(String modelName);

	/**
	 * @param modelArtifactReflection
	 *            {@link ArtifactReflection} for a model artifact.
	 */
	ServiceDomainConfiguration addModel(ArtifactReflection modelArtifactReflection);
	
	ServiceDomainConfiguration addModel(GmMetaModel gmModel);
	
	ServiceDomainConfiguration addModel(Model model);
	
	void configureModel(Consumer<ModelMetaDataEditor> configurer);
	
	void addDefaultRequestSupplier(Supplier<? extends ServiceRequest> defaultRequestSupplier);
}
