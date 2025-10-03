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
import java.util.Set;

import com.braintribe.cfg.Required;
import com.braintribe.gm.model.persistence.reflection.api.GetMetaModelForTypes;
import com.braintribe.gm.model.persistence.reflection.api.GetModelAndWorkbenchEnvironment;
import com.braintribe.gm.model.persistence.reflection.api.GetModelEnvironment;
import com.braintribe.gm.model.persistence.reflection.api.PersistenceReflectionRequest;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.essential.InvalidArgument;
import com.braintribe.logging.Logger;
import com.braintribe.model.accessapi.ModelEnvironment;
import com.braintribe.model.generic.GMF;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.meta.GmMetaModel;
import com.braintribe.model.processing.service.common.context.UserSessionAspect;
import com.braintribe.model.processing.service.impl.AbstractDispatchingServiceProcessor;
import com.braintribe.model.processing.service.impl.DispatchConfiguration;
import com.braintribe.model.usersession.UserSession;
import com.braintribe.model.util.meta.NewMetaModelGeneration;
import com.braintribe.model.workbench.WorkbenchConfiguration;
import com.braintribe.utils.StringTools;
import com.braintribe.utils.collection.impl.AttributeContexts;

import hiconic.rx.module.api.service.ConfiguredModel;

public class PersistenceReflectionProcessor extends AbstractDispatchingServiceProcessor<PersistenceReflectionRequest, Object> {

	private static final Logger log = Logger.getLogger(PersistenceReflectionProcessor.class);

	private RxAccesses accesses;

	@Required
	public void setAccesses(RxAccesses accesses) {
		this.accesses = accesses;
	}

	@Override
	protected void configureDispatching(DispatchConfiguration<PersistenceReflectionRequest, Object> dispatching) {
		dispatching.registerReasoned(GetModelEnvironment.T, (c, r) -> getModelEnvironment(r));
		dispatching.registerReasoned(GetModelAndWorkbenchEnvironment.T, (c, r) -> getModelAndWorkbenchEnvironment(r));
		dispatching.registerReasoned(GetMetaModelForTypes.T, (c, r) -> getModelForTypes(r));
	}

	// ###############################################
	// ## . . GetModelAndWorkbenchEnvironment . . . ##
	// ###############################################

	private Maybe<? extends ModelEnvironment> getModelAndWorkbenchEnvironment(GetModelAndWorkbenchEnvironment r) {
		return getModelAndWorkbenchEnvironment(r.getAccessId(), r.getFoldersByPerspective());
	}

	private Maybe<ModelEnvironment> getModelAndWorkbenchEnvironment(String accessId, @SuppressWarnings("unused") Set<String> workbenchPerspectiveNames) {
		Maybe<ModelEnvironment> modelEnvironmentMaybe = getModelEnvironment(accessId);
		if (modelEnvironmentMaybe.isUnsatisfied())
			return modelEnvironmentMaybe.propagateReason();

		ModelEnvironment modelEnvironment = modelEnvironmentMaybe.get();

		// TODO add support for custom workbench

		// String workbenchAccessId = modelEnvironment.getWorkbenchModelAccessId();
		// if (workbenchAccessId != null) {
		// if (workbenchPerspectiveNames != null) {
		// List<WorkbenchPerspective> perspectives = queryWorkbenchPerspectives(workbenchAccessId, workbenchPerspectiveNames);
		// modelEnvironment.setPerspectives(perspectives);
		// } else {
		// Set<Folder> workbenchRootFolders = queryWorkbenchRootFolders(workbenchAccessId);
		// modelEnvironment.setWorkbenchRootFolders(workbenchRootFolders);
		// }
		//
		// WorkbenchConfiguration workbenchConfiguration = queryWorkbenchConfiguration(workbenchAccessId);
		// modelEnvironment.setWorkbenchConfiguration(workbenchConfiguration);
		// }

		// Add support for automatically detecting the locale for the workbench based on the user session locale
		WorkbenchConfiguration workbenchConfiguration = modelEnvironment.getWorkbenchConfiguration();
		if (workbenchConfiguration != null) {
			String wbLocale = workbenchConfiguration.getLocale();
			if ("auto".equalsIgnoreCase(wbLocale)) {
				UserSession userSession = AttributeContexts.peek().findOrNull(UserSessionAspect.class);
				if (userSession != null) {
					String propLocale = userSession.locale();
					if (!StringTools.isBlank(propLocale)) {
						log.trace(() -> "Setting locale " + propLocale + " for session " + userSession.getSessionId());
						workbenchConfiguration.setLocale(propLocale);
					}
				}
			}
		}

		return Maybe.complete(modelEnvironment);
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

		RxAccess rxAccess = accesses.getAccess(accessId);

		ModelEnvironment modelEnvironment = ModelEnvironment.T.create();

		ConfiguredModel configuredDataModel = rxAccess.configuredDataModel();
		ConfiguredModel configuredServiceModel = rxAccess.configuredServiceModel();

		GmMetaModel dataModel = configuredDataModel.modelOracle().getGmMetaModel();
		GmMetaModel serviceModel = configuredServiceModel.modelOracle().getGmMetaModel();
		GmMetaModel workbenchModel = null; // TODO let's see if this is needed, seems in cortex this could have been null

		modelEnvironment.setDataAccessId(accessId);
		modelEnvironment.setDataModel(dataModel);
		modelEnvironment.setServiceModel(serviceModel);
		modelEnvironment.setServiceModelName(configuredServiceModel.modelName());
		modelEnvironment.setWorkbenchModel(workbenchModel);

		return Maybe.complete(modelEnvironment);
	}

	// ###############################################
	// ## . . . . . GetMetaModelForTypes . . . . . .##
	// ###############################################

	private Maybe<GmMetaModel> getModelForTypes(GetMetaModelForTypes r) {
		return getModelForTypes(r.getTypeSignatures());
	}

	private Maybe<GmMetaModel> getModelForTypes(Collection<String> typeSignatures) {
		List<EntityType<?>> entityTypes = newList();

		for (String typeSignature : typeSignatures) {
			EntityType<?> entityType = GMF.getTypeReflection().findEntityType(typeSignature);
			if (entityType == null)
				return Reasons.build(InvalidArgument.T) //
						.text("Unknown entity type: " + typeSignature) //
						.toMaybe();

			entityTypes.add(entityType);
		}

		NewMetaModelGeneration newMmg = new NewMetaModelGeneration();
		GmMetaModel result = newMmg.buildMetaModel("reflex:virtual-model-for-given-types", entityTypes);
		result.setVersion("1.0");

		return Maybe.complete(result);
	}
}
