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

import java.util.List;

import com.braintribe.model.meta.GmMetaModel;

public interface ConfiguredModels {

	/**
	 * Returns the {@link ConfiguredModel} for given model name if available, or null
	 * <p>
	 * In case a name of a model from classpath is used, an actual ConfiguredModel will be created and cached on first call.
	 */
	ConfiguredModel byName(String modelName);

	/**
	 * Equivalent not to {@code byName(reference.name())}}
	 * 
	 * @see #byName(String)
	 */
	ConfiguredModel byReference(ModelSymbol reference);

	ConfiguredModel mainPersistenceModel();

	/** Enumerates all available ServiceDomains */
	List<? extends ConfiguredModel> list();

	List<? extends ConfiguredModel> listDependers(GmMetaModel model);

}
