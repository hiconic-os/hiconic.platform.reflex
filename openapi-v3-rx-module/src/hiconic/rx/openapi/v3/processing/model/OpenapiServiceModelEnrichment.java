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
package hiconic.rx.openapi.v3.processing.model;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.meta.data.prompt.Hidden;
import com.braintribe.model.meta.data.prompt.Visible;
import com.braintribe.model.meta.selector.DisjunctionSelector;
import com.braintribe.model.meta.selector.UseCaseSelector;
import com.braintribe.model.processing.meta.editor.ModelMetaDataEditor;
import com.braintribe.model.service.api.AuthorizableRequest;
import com.braintribe.model.service.api.DomainRequest;
import com.braintribe.model.service.api.ServiceRequest;

/** Default OpenAPI-only view of the common service request properties. */
public final class OpenapiServiceModelEnrichment {

	private OpenapiServiceModelEnrichment() {
	}

	public static void configure(ModelMetaDataEditor editor) {
		UseCaseSelector urlencoded = useCase("openapi:application/x-www-form-urlencoded");
		UseCaseSelector multipart = useCase("openapi:multipart/form-data");

		DisjunctionSelector flatMimeTypes = DisjunctionSelector.T.create();
		flatMimeTypes.getOperands().add(urlencoded);
		flatMimeTypes.getOperands().add(multipart);

		Hidden hiddenInFlatOpenapi = Hidden.T.create();
		hiddenInFlatOpenapi.setSelector(flatMimeTypes);

		Visible visibleWithSessionId = Visible.T.create();
		visibleWithSessionId.setSelector(useCase("openapi:include-session-id"));
		visibleWithSessionId.setConflictPriority(1d);
		visibleWithSessionId.setImportant(true);

		editor.onEntityType(GenericEntity.T) //
				.addPropertyMetaData(GenericEntity.globalId, hiddenInFlatOpenapi) //
				.addPropertyMetaData(GenericEntity.id, hiddenInFlatOpenapi) //
				.addPropertyMetaData(GenericEntity.partition, hiddenInFlatOpenapi);

		editor.onEntityType(AuthorizableRequest.T) //
				.addPropertyMetaData(AuthorizableRequest.sessionId, hiddenInFlatOpenapi) //
				.addPropertyMetaData(AuthorizableRequest.sessionId, visibleWithSessionId);

		editor.onEntityType(DomainRequest.T).addPropertyMetaData(DomainRequest.domainId, hiddenInFlatOpenapi);
		editor.onEntityType(ServiceRequest.T).addPropertyMetaData("metaData", hiddenInFlatOpenapi);
	}

	private static UseCaseSelector useCase(String useCase) {
		UseCaseSelector selector = UseCaseSelector.T.create();
		selector.setUseCase(useCase);
		return selector;
	}
}
