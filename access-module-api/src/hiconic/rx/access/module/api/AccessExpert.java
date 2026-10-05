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
package hiconic.rx.access.module.api;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.model.access.IncrementalAccess;
import com.braintribe.model.processing.meta.editor.ModelMetaDataEditor;

import hiconic.rx.access.model.configuration.Access;
import hiconic.rx.module.api.service.ConfiguredModel;

public interface AccessExpert<A extends Access> {

	/**
	 * Configures the data model of given access with meta data specific to this kind of access.
	 * <p>
	 * Called when the configured models are finalized, i.e. after all modules have configured their models, so the expert must be registered
	 * during the model configuration phase at the latest.
	 */
	default //
	void configureDataModel(A access, ModelMetaDataEditor editor) {
		// no specific configuration by default
	}

	Maybe<IncrementalAccess> deploy(A access, ConfiguredModel dataModel);
}
