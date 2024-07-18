package hiconic.rx.access.module.processing;

import com.braintribe.cfg.Required;
import com.braintribe.gm.model.persistence.reflection.api.GetModelEnvironment;
import com.braintribe.gm.model.persistence.reflection.api.PersistenceReflectionRequest;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.essential.InvalidArgument;
import com.braintribe.model.accessapi.ModelEnvironment;
import com.braintribe.model.processing.service.api.ServiceRequestContext;
import com.braintribe.model.processing.service.impl.AbstractDispatchingServiceProcessor;
import com.braintribe.model.processing.service.impl.DispatchConfiguration;

import hiconic.rx.access.model.configuration.Access;
import hiconic.rx.module.api.service.ConfiguredModel;

public class PersistenceReflectionProcessor extends AbstractDispatchingServiceProcessor<PersistenceReflectionRequest, Object>{
	
	private RxAccesses accesses;
	
	@Required
	public void setAccesses(RxAccesses accesses) {
		this.accesses = accesses;
	}

	@Override
	protected void configureDispatching(DispatchConfiguration<PersistenceReflectionRequest, Object> dispatching) {
		dispatching.registerReasoned(GetModelEnvironment.T, this::getModelEnvironment);
	}
	
	private Maybe<ModelEnvironment> getModelEnvironment(ServiceRequestContext context, GetModelEnvironment request) {
		String accessId = request.getAccessId();
		
		if (accessId == null)
			return Reasons.build(InvalidArgument.T).text("GetModelEnvironment.accessId must not be null").toMaybe();
		
		RxAccess rxAccess = accesses.getAccess(accessId);
		
		Access access = rxAccess.access();
		
		String domainId = access.getServiceDomainId();
		access.getServiceModelName();
		
		ModelEnvironment modelEnvironment = ModelEnvironment.T.create();
		
		ConfiguredModel configuredDataModel = rxAccess.configuredDataModel();
		ConfiguredModel configuredServiceModel = rxAccess.configuredServiceModel();
		
		modelEnvironment.setDataAccessId(accessId);
		
		modelEnvironment.setDataModel(configuredDataModel.modelOracle().getGmMetaModel());

		modelEnvironment.setServiceModelName(configuredServiceModel.modelName());
		modelEnvironment.setServiceModel(configuredServiceModel.modelOracle().getGmMetaModel());
		
		// TODO: complete model environment
		//modelEnvironment.setW
		
		return Maybe.complete(modelEnvironment);
	}
	
}
