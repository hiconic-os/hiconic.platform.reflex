package hiconic.platform.reflex.explorer.processing.bapi;

import java.util.List;
import java.util.stream.Collectors;

import com.braintribe.cfg.Required;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.model.accessdeployment.HardwiredAccess;
import com.braintribe.model.accessdeployment.IncrementalAccess;
import com.braintribe.model.bapi.AvailableAccesses;
import com.braintribe.model.bapi.AvailableAccessesRequest;
import com.braintribe.model.processing.service.api.ReasonedServiceProcessor;
import com.braintribe.model.processing.service.api.ServiceRequestContext;

import hiconic.rx.access.module.api.AccessDomain;
import hiconic.rx.access.module.api.AccessDomains;

/**
 * @author peter.gazdik
 */
public class AvailableAccessesProcessor implements ReasonedServiceProcessor<AvailableAccessesRequest, AvailableAccesses> {

	private AccessDomains accessDomains;

	@Required
	public void setAccessDomains(AccessDomains accessDomains) {
		this.accessDomains = accessDomains;
	}

	@Override
	public Maybe<? extends AvailableAccesses> processReasoned(ServiceRequestContext context, AvailableAccessesRequest request) {
		AvailableAccesses result = AvailableAccesses.T.create();

		if (request.getIncludeHardwired())
			result.setAccesses(listAccesses());

		return Maybe.complete(result);
	}

	private List<IncrementalAccess> listAccesses() {
		return accessDomains.domainIds().stream() //
				.map(this::accessIdToIncrementalAccess) //
				.collect(Collectors.toList());
	}

	private IncrementalAccess accessIdToIncrementalAccess(String accessId) {
		AccessDomain accessDomain = accessDomains.byId(accessId);
		
		HardwiredAccess result = HardwiredAccess.T.create();
		result.setExternalId(accessId);
		result.setGlobalId("rx-access:" + accessId);
		result.setName(accessId);
		result.setMetaModel(accessDomain.configuredDataModel().modelOracle().getGmMetaModel());
		result.setServiceModel(accessDomain.configuredServiceModel().modelOracle().getGmMetaModel());

		return result;
	}
}
