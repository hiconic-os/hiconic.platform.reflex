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
import com.braintribe.model.processing.service.api.ServiceProcessor;
import com.braintribe.model.service.api.ServiceRequest;

public interface ModelConfiguration extends ModelReference {

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

	void addModel(ModelReference modelReference);

	void configureModel(Consumer<ModelMetaDataEditor> configurer);

	<R extends ServiceRequest> void bindRequest(EntityType<R> requestType, Supplier<ServiceProcessor<? super R, ?>> serviceProcessorSupplier);

	<R extends ServiceRequest> void bindRequestMapped(EntityType<R> requestType,
			Supplier<MappingServiceProcessor<? super R, ?>> serviceProcessorSupplier);

	InterceptorBuilder bindInterceptor(String identification);
}
