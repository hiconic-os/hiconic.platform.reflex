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

import com.braintribe.common.artifact.ArtifactReflection;

import hiconic.rx.module.api.wire.RxModuleContract;

/**
 * Main class for configuring models, i.e. acquiring configuration models by name and adding dependencies and metadata to them.
 * <p>
 * Passed to each module during model configuration phase via {@link RxModuleContract#configureModels(ModelConfigurations)}.
 */
public interface ModelConfigurations {
	ModelReference mainPersistenceModelRef = ModelReference.of("rx-platform:configured-main-persistence-model");

	/**
	 * Acquires a {@link ModelConfiguration} with the given name as model name
	 */
	ModelConfiguration byName(String modelName);

	ModelConfiguration byReference(ModelReference reference);

	/**
	 * Acquires a {@link ModelConfiguration} via {@link #byReference} using {@link #mainPersistenceModelRef}
	 */
	ModelConfiguration mainPersistenceModel();

	/**
	 * Acquires "configured" model for given base model.
	 * <p>
	 * Shortcut for {@code extendedModel("configured", baseModel)}
	 * 
	 * @see #extendedModel(String, ArtifactReflection)
	 */
	ModelConfiguration configuredModel(ArtifactReflection baseModel);

	/**
	 * Acquires a {@link ModelConfiguration} for a model named "${baseModel.groupId()}:${prefix}-${baseModel.artifactId()}" with the model of that
	 * artifact as a dependency
	 */
	ModelConfiguration extendedModel(String prefix, ArtifactReflection baseModel);

	ModelConfiguration extendedModel(ModelReference reference, ArtifactReflection baseModel);
}
