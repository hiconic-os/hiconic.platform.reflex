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
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.module.WireModule;
import com.braintribe.wire.api.space.WireSpace;

import hiconic.rx.module.api.service.ModelConfiguration;
import hiconic.rx.module.api.service.ModelConfigurations;
import hiconic.rx.module.api.service.ServiceDomainConfiguration;
import hiconic.rx.module.api.service.ServiceDomainConfigurations;

/**
 * Main contract which every Reflex module must implement and which adds configuration and features to the application.
 * <p>
 * A module can access the platform by {@link Import importing} {@link RxPlatformContract}.
 * <p>
 * The Platform loads the modules by calling this contract's methods in multiple rounds, and for each round all methods are called for all the
 * modules. The order is given here:<br>
 * <b>Round 1 - Model Configuration:</b>
 * <ol>
 * <li>{@link #configureMainPersistenceModel(ModelConfiguration)}
 * <li>{@link #configureModels(ModelConfigurations)}
 * </ol>
 * <b>Round 2 - Hardwired Service and Platform Configuration:</b>
 * <ol>
 * <li>{@link #configureMainServiceDomain(ServiceDomainConfiguration)}
 * <li>{@link #configureServiceDomains(ServiceDomainConfigurations)}
 * <li>{@link #registerCrossDomainInterceptors(InterceptorRegistry)}
 * <li>{@link #registerFallbackProcessors(ProcessorRegistry)}
 * <li>{@link #configurePlatform(RxPlatformConfigurator)}
 * </ol>
 * <b>Round 3 - Modeled Service and Platform Configuration:</b>
 * <ol>
 * <li>{@link #onDeploy()}
 * </ol>
 * <b>Round 4 - Application Ready Notification:</b>
 * <ol>
 * <li>{@link #onApplicationReady()}
 * </ol>
 * 
 * <p>
 * IMPLEMENTATION NOTES: Each Reflex module must contain a classpath resource {@code META-INF/reflex-module.properties}, which contains the name of
 * the {@link WireModule} which binds this contract to its implementation.
 */
@SuppressWarnings("unused")
public interface RxModuleContract extends WireSpace {

	/**
	 * Configures models by:
	 * <ul>
	 * <li>adding models</li>
	 * <li>binding processors</li>
	 * <li>binding interceptors</li>
	 * <li>configuring meta data on models</li>
	 * </ul>
	 */
	default void configureModels(ModelConfigurations configurations) {
		// implement if needed
	}

	// TODO how can this be here, we don't even know if we have any persistence.
	default void configureMainPersistenceModel(ModelConfiguration configuration) {
		// implement if needed
	}

	/**
	 * Configures service domains by:
	 * <ul>
	 * <li>adding models</li>
	 * <li>binding processors</li>
	 * <li>binding interceptors</li>
	 * <li>configuring meta data on models</li>
	 * </ul>
	 */
	default void configureServiceDomains(ServiceDomainConfigurations configurations) {
		// implement if needed
	}

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

	/**/
	default void configurePlatform(RxPlatformConfigurator configurator) {
		// implement if needed
	}

	/** Called when the platform application loaded and initialized all modules to allow module to deploy modeled configurations. */
	default void onDeploy() {
		// implement if needed
	}

	/**
	 * Called when the platform application loaded and initialized and deployed all modules
	 */
	default void onApplicationReady() {
		// implement if needed
	}
	
	/**
	 * Called when the platform application received a shutdown signal and should stop processing requests
	 */
	default void onApplicationShutdown() {
		// implement if needed
	}
}
