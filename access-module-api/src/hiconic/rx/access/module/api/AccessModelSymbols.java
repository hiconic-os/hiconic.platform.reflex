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

import com.braintribe.gm._AccessApiModel_;
import com.braintribe.gm._ResourceApiModel_;
import com.braintribe.gm._ResourceModel_;
import com.braintribe.model.accessapi.PersistenceRequest;
import com.braintribe.model.resource.source.ResourceSource;
import com.braintribe.model.resourceapi.base.ResourceRequest;

import hiconic.rx.module.api.service.ModelSymbol;

public interface AccessModelSymbols {

	/**
	 * Binds processor for {@link PersistenceRequest}
	 * <p>
	 * This model is added to every service domain of every access.
	 */
	ModelSymbol configuredAccessApiModel = ModelSymbol.of("rx:configured-" + _AccessApiModel_.artifactId);

	/**
	 * Binds processor for {@link ResourceRequest}.
	 */
	ModelSymbol configuredResourceApiModel = ModelSymbol.of("rx:configured-" + _ResourceApiModel_.artifactId);

	/**
	 * Configures resource enriching (PreEnrichResourceWith MD) on {@link ResourceSource}.
	 * <p>
	 * To take effect, add to the access's data model.
	 */
	ModelSymbol configuredResourceModel = ModelSymbol.of("rx:configured-" + _ResourceModel_.artifactId);

}
