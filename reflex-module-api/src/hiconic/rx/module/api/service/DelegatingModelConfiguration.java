package hiconic.rx.module.api.service;

import java.util.function.Consumer;
import java.util.function.Supplier;

import com.braintribe.common.artifact.ArtifactReflection;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.Model;
import com.braintribe.model.meta.GmMetaModel;
import com.braintribe.model.processing.meta.editor.ModelMetaDataEditor;
import com.braintribe.model.processing.service.api.MappingServiceProcessor;
import com.braintribe.model.processing.service.api.ServiceProcessor;
import com.braintribe.model.service.api.ServiceRequest;

public interface DelegatingModelConfiguration extends ModelConfiguration {
	ModelConfiguration modelConfiguration();
	
	@Override
	default void addModel(ArtifactReflection modelArtifactReflection) {
		modelConfiguration().addModel(modelArtifactReflection);
	}
	
	@Override
	default void addModel(GmMetaModel gmModel) {
		modelConfiguration().addModel(gmModel);
	}
	
	@Override
	default void addModel(Model model) {
		modelConfiguration().addModel(model);
	}
	
	@Override
	default void addModel(ModelReference modelReference) {
		modelConfiguration().addModel(modelReference);
	}
	
	@Override
	default void addModelByName(String modelName) {
		modelConfiguration().addModelByName(modelName);
	}
	
	@Override
	default InterceptorBuilder bindInterceptor(String identification) {
		return modelConfiguration().bindInterceptor(identification);
	}
	
	@Override
	default <R extends ServiceRequest> void bindRequest(EntityType<R> requestType,
			Supplier<ServiceProcessor<? super R, ?>> serviceProcessorSupplier) {
		modelConfiguration().bindRequest(requestType, serviceProcessorSupplier);
	}
	
	@Override
	default <R extends ServiceRequest> void bindRequestMapped(EntityType<R> requestType,
			Supplier<MappingServiceProcessor<? super R, ?>> serviceProcessorSupplier) {
		modelConfiguration().bindRequestMapped(requestType, serviceProcessorSupplier);
	}
	
	@Override
	default void configureModel(Consumer<ModelMetaDataEditor> configurer) {
		modelConfiguration().configureModel(configurer);
	}
	
	@Override
	default String modelName() {
		return modelConfiguration().modelName();
	}

}
