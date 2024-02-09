package hiconic.rx.module.api.wire;

import com.braintribe.codec.marshaller.api.ConfigurableMarshallerRegistry;
import com.braintribe.model.processing.meta.configured.ConfigurationModelBuilder;
import com.braintribe.model.processing.meta.editor.ModelMetaDataEditor;
import com.braintribe.model.processing.service.api.ServiceInterceptorProcessor;
import com.braintribe.model.processing.service.api.ServiceProcessor;
import com.braintribe.model.processing.service.common.ConfigurableDispatchingServiceProcessor;
import com.braintribe.wire.api.space.WireSpace;

/**
 * Wire contract for reflex modules that need to be implemented by an according wire space
 * which is to be injected with the classpath resource META-INF/reflex-module.properties.
 * 
 * The reflex platform accesses modules via this contract while modules can access the platform
 * via {@link RxPlatformContract}.
 * 
 * <p>
 * Example for module implementation
 * 
 * <pre>
 * package example;
 * 
 * import hiconic.rx.module.api.wire.RxModuleContract;
 * import hiconic.rx.module.api.wire.RxPlatformContract;
 * import com.braintribe.wire.api.annotation.Import;
 * 
 * public class MyReflexWireModule implements RxModuleContract {
 *   &#64;Import
 *   private RxPlatformContract platform;
 *   
 *   &#64;Override
 *   public void addApiModels(ConfigurationModelBuilder builder) {
 *     builder.addDependencyByName("groupId:my-model");
 *   }
 * }
 * </pre>
 * 
 * <p>
 * Example for module injection
 * 
 * <pre>
 * wire-module=example.MyReflexWireModule
 * </pre>
 * 
 * @author dirk.scheffler
 *
 */
public interface RxModuleContract extends WireSpace {
	/**
	 * Bind {@link ServiceProcessor
	 * service processors} and {@link ServiceInterceptorProcessor service
	 * interceptors} on request types of the main service domain.
	 */
	default void registerProcessors(ConfigurableDispatchingServiceProcessor dispatching) {
	}

	/**
	 * Registers marshallers with general availability
	 */
	default void registerMarshaller(ConfigurableMarshallerRegistry registry) {
	}
	
	/**
	 * Adds models to the configuration model of the main service domain
	 */
	default void addApiModels(ConfigurationModelBuilder builder) {
	}

	/**
	 * Configures meta data on the configuration model of the main service domain
	 */
	default void configureApiModel(ModelMetaDataEditor editor) {
	}
	
	/**
	 * Called when the platform application loaded and initialized all modules
	 */
	default void onApplicationReady() {
		
	}
	
}
