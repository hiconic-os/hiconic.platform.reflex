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
package hiconic.rx.access.module.processing;

import static com.braintribe.utils.lcd.CollectionTools2.newList;

import java.util.Collection;
import java.util.List;

import com.braintribe.cfg.Required;
import com.braintribe.gm.model.persistence.reflection.api.GetMetaModelForTypes;
import com.braintribe.gm.model.persistence.reflection.api.GetModelEnvironment;
import com.braintribe.gm.model.persistence.reflection.api.PersistenceReflectionRequest;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.essential.InvalidArgument;
import com.braintribe.gm.model.reason.essential.NotFound;
import com.braintribe.model.accessapi.ModelEnvironment;
import com.braintribe.model.generic.GMF;
import com.braintribe.model.generic.reflection.CustomType;
import com.braintribe.model.meta.GmMetaModel;
import com.braintribe.model.processing.service.impl.AbstractDispatchingServiceProcessor;
import com.braintribe.model.processing.service.impl.DispatchConfiguration;
import com.braintribe.model.util.meta.NewMetaModelGeneration;

import hiconic.rx.access.module.api.AccessDomain;
import hiconic.rx.module.api.service.ConfiguredModel;

public class PersistenceReflectionProcessor extends AbstractDispatchingServiceProcessor<PersistenceReflectionRequest, Object> {

	private RxAccesses accesses;

	@Required
	public void setAccesses(RxAccesses accesses) {
		this.accesses = accesses;
	}

	@Override
	protected void configureDispatching(DispatchConfiguration<PersistenceReflectionRequest, Object> dispatching) {
		dispatching.registerReasoned(GetModelEnvironment.T, (c, r) -> getModelEnvironment(r));
		dispatching.registerReasoned(GetMetaModelForTypes.T, (c, r) -> getModelForTypes(r));
	}

	// ###############################################
	// ## . . . . . GetModelEnvironment . . . . . . ##
	// ###############################################

	private Maybe<ModelEnvironment> getModelEnvironment(GetModelEnvironment request) {
		return getModelEnvironment(request.getAccessId());
	}

	private Maybe<ModelEnvironment> getModelEnvironment(String accessId) {
		if (accessId == null)
			return Reasons.build(InvalidArgument.T) //
					.text("GetModelEnvironment.accessId must not be null") //
					.toMaybe();

		AccessDomain rxAccess = accesses.byId(accessId);
		if (rxAccess == null)
			return Reasons.build(NotFound.T) //
					.text("No access found for id: " + accessId) //
					.toMaybe();

		ModelEnvironment modelEnvironment = ModelEnvironment.T.create();

		ConfiguredModel configuredDataModel = rxAccess.configuredDataModel();
		ConfiguredModel configuredServiceModel = rxAccess.configuredServiceModel();

		GmMetaModel dataModel = configuredDataModel.modelOracle().getGmMetaModel();
		GmMetaModel serviceModel = configuredServiceModel.modelOracle().getGmMetaModel();
		modelEnvironment.setDataAccessId(accessId);
		modelEnvironment.setDataModel(dataModel);
		modelEnvironment.setServiceModel(serviceModel);
		modelEnvironment.setServiceModelName(configuredServiceModel.name());

		return Maybe.complete(modelEnvironment);
	}

	// ###############################################
	// ## . . . . . GetMetaModelForTypes . . . . . .##
	// ###############################################

	private Maybe<GmMetaModel> getModelForTypes(GetMetaModelForTypes r) {
		return getModelForTypes(r.getTypeSignatures());
	}

	private Maybe<GmMetaModel> getModelForTypes(Collection<String> typeSignatures) {
		List<CustomType> types = newList();

		for (String typeSignature : typeSignatures) {
			CustomType type = GMF.getTypeReflection().findType(typeSignature);
			if (type == null)
				return Reasons.build(InvalidArgument.T) //
						.text("Unknown GM type: " + typeSignature) //
						.toMaybe();

			types.add(type);
		}

		NewMetaModelGeneration newMmg = new NewMetaModelGeneration();
		GmMetaModel result = newMmg.buildMetaModel("reflex:virtual-model-for-given-types", types);
		result.setVersion("1.0");

		return Maybe.complete(result);
	}
}
