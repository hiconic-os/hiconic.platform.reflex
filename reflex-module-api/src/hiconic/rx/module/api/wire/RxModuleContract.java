package hiconic.rx.module.api.wire;

import com.braintribe.codec.marshaller.api.ConfigurableMarshallerRegistry;
import com.braintribe.model.processing.service.api.InterceptorRegistry;
import com.braintribe.model.processing.service.api.ProcessorRegistry;
import com.braintribe.wire.api.space.WireSpace;

import hiconic.rx.module.api.service.ServiceDomainConfiguration;
import hiconic.rx.module.api.service.ServiceDomainConfigurations;

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
	 * Configures service domains by:
	 * <ul>
	 * 	<li>adding models</li>
	 * 	<li>binding processors</li>
	 *  <li>binding interceptors</li>
	 *  <li>configuring meta data on models</li>
	 * </ul>
	 */
	default void configureServiceDomains(ServiceDomainConfigurations configurations) {
		
	}

	/**
	 * Configures the main service domain by:
	 * <ul>
	 * 	<li>adding models</li>
	 * 	<li>binding processors</li>
	 *  <li>binding interceptors</li>
	 *  <li>configuring meta data on models</li>
	 * </ul>
	 */
	default void configureMainServiceDomain(ServiceDomainConfiguration configuration) {
		
	}
	
	/**
	 * Registers interceptors that will run for each service domain
	 */
	default void registerCrossDomainInterceptors(InterceptorRegistry interceptorRegistry) {
		
	}
	
	/**
	 * Registers service processors that will run if a service domain has no specific processor registered
	 */
	default void registerFallbackProcessors(ProcessorRegistry processorRegistry) {
		
	}
	
	/**
	 * Registers marshallers with general availability
	 */
	default void registerMarshaller(ConfigurableMarshallerRegistry registry) {
	}
	
	/**
	 * Called when the platform application loaded and initialized all modules
	 */
	default void onApplicationReady() {
		
	}
}
