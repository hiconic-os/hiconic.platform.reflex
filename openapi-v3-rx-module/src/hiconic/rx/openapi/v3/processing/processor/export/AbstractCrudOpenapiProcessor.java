package hiconic.rx.openapi.v3.processing.processor.export;

import com.braintribe.cfg.Required;
import com.braintribe.model.openapi.v3_0.api.OpenapiCrudRequest;
import com.braintribe.model.processing.service.api.ServiceRequestContext;

import hiconic.rx.access.module.api.AccessDomain;
import hiconic.rx.access.module.api.AccessDomains;
import hiconic.rx.module.api.service.ConfiguredModel;

/**
 * @author peter.gazdik
 */
public abstract class AbstractCrudOpenapiProcessor<R extends OpenapiCrudRequest> extends AbstractOpenapiProcessor<R> {

	private AccessDomains accessDomains;

	@Required
	public void setAccessDomains(AccessDomains accessDomains) {
		this.accessDomains = accessDomains;
	}

	@Override
	protected ConfiguredModel getConfiguredModel(ServiceRequestContext requestContext, R request) {
		AccessDomain accessDomain = accessDomains.byId(request.getAccessId());
		if (accessDomain == null)
			throw new IllegalArgumentException("Unknown access: " + request.getAccessId());

		return accessDomain.configuredDataModel();
	}

}
