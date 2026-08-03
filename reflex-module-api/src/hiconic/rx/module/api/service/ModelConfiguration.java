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
package hiconic.rx.module.api.service;

import java.util.function.Consumer;
import java.util.function.Supplier;

import com.braintribe.common.artifact.ArtifactReflection;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.Model;
import com.braintribe.model.meta.GmMetaModel;
import com.braintribe.model.processing.meta.editor.ModelMetaDataEditor;
import com.braintribe.model.processing.service.api.MappingServiceProcessor;
import com.braintribe.model.processing.service.api.Service;
import com.braintribe.model.processing.service.api.ServiceProcessor;
import com.braintribe.model.service.api.ServiceRequest;

public interface ModelConfiguration extends ModelSymbol {

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

	void addModel(ModelSymbol modelReference);

	void configureModel(Consumer<ModelMetaDataEditor> configurer);

	<R extends ServiceRequest> void bindRequest(EntityType<R> requestType, Supplier<ServiceProcessor<? super R, ?>> serviceProcessorSupplier);

	/**
	 * Binds a processor with an explicit metadata conflict priority. A lower-priority platform fallback can therefore
	 * be superseded unambiguously by a regular module binding.
	 */
	default <R extends ServiceRequest> void bindRequest(EntityType<R> requestType,
			Supplier<ServiceProcessor<? super R, ?>> serviceProcessorSupplier, double conflictPriority) {
		bindRequest(requestType, serviceProcessorSupplier);
	}

	/** Binds the request to a lazily resolved, platform-registered processor component. */
	<R extends ServiceRequest> void bindRequest(EntityType<R> requestType, String serviceProcessorKey);

	default <R extends ServiceRequest> void bindRequestBySymbol(EntityType<R> requestType, ServiceProcessorSymbol serviceProcessorSymbol) {
		bindRequest(requestType, serviceProcessorSymbol.name());
	}

	/**
	 * Binds {@link MappingServiceProcessor}s, i.e. {@link ServiceProcessor}s where dispatching is configured by annotating methods with
	 * {@link Service}.
	 */
	<R extends ServiceRequest> void bindRequestMapped(EntityType<R> requestType,
			Supplier<MappingServiceProcessor<? super R, ?>> serviceProcessorSupplier);

	InterceptorBuilder bindInterceptor(String identification);

	/**
	 * Declares the execution order independently from interceptor registration. A later call replaces the previously declared order. Identifiers
	 * which are not registered on this configured model are ignored; registered interceptors not mentioned here retain their registration order
	 * behind the explicitly ordered interceptors.
	 */
	void orderInterceptors(String... identifications);

	default void orderInterceptors(InterceptorSymbol... identifications) {
		String[] names = new String[identifications.length];
		for (int i = 0; i < identifications.length; i++)
			names[i] = identifications[i].name();
		orderInterceptors(names);
	}
	
	default InterceptorBuilder bindInterceptor(InterceptorSymbol identification) {
		return bindInterceptor(identification.name());
	}
}
