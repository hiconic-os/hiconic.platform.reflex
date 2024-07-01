// ============================================================================
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ============================================================================
package hiconic.rx.module.api.wire;

import com.braintribe.codec.marshaller.api.ConfigurableMarshallerRegistry;
import com.braintribe.model.processing.service.api.InterceptorRegistry;
import com.braintribe.model.processing.service.api.ProcessorRegistry;
import com.braintribe.wire.api.space.WireSpace;

import hiconic.rx.module.api.service.ModelConfiguration;
import hiconic.rx.module.api.service.ModelConfigurations;
import hiconic.rx.module.api.service.ServiceDomainConfiguration;
import hiconic.rx.module.api.service.ServiceDomainConfigurations;

/**
 * Wire contract for reflex modules that need to be implemented by an according wire space
 * which is to be injected with the classpath resource META-INF/reflex-module.properties.
 * 
 * The reflex platform accesses modules via this contract while modules can access the platform
 * via {@link RxPlatformContract}.
 * 
 * @author dirk.scheffler
 */
@SuppressWarnings("unused")
public interface RxModuleContract extends WireSpace {

	/**
	 * Configures models by:
	 * <ul>
	 * 	<li>adding models</li>
	 * 	<li>binding processors</li>
	 *  <li>binding interceptors</li>
	 *  <li>configuring meta data on models</li>
	 * </ul>
	 */
	default void configureModels(ModelConfigurations configurations) {
		// implement if needed
	}
	
	/**
	 * Configures the main persistence model acquired by {@link ModelConfigurations#mainPersistenceModel()}
	 * <ul>
	 * 	<li>adding models</li>
	 *  <li>configuring meta data on models</li>
	 * </ul>
	 */
	default void configureMainPersistenceModel(ModelConfiguration configuration) {
		// implement if needed
	}
	
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
		// implement if needed
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
		// implement if needed
	}
	
	/**
	 * Registers interceptors that will run for each service domain
	 */
	default void registerCrossDomainInterceptors(InterceptorRegistry interceptorRegistry) {
		// implement if needed
	}
	
	/**
	 * Registers service processors that will run if a service domain has no specific processor registered
	 */
	default void registerFallbackProcessors(ProcessorRegistry processorRegistry) {
		// implement if needed
	}
	
	/**
	 * Registers marshallers with general availability
	 */
	default void registerMarshaller(ConfigurableMarshallerRegistry registry) {
		// implement if needed
	}
	
	/**
	 * Called when the platform application loaded and initialized all modules
	 */
	default void onApplicationReady() {
		// implement if needed
	}
}
