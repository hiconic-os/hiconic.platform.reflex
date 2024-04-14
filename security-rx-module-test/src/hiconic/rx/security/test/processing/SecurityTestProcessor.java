package hiconic.rx.security.test.processing;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.model.processing.service.api.ServiceRequestContext;
import com.braintribe.model.processing.service.impl.AbstractDispatchingServiceProcessor;
import com.braintribe.model.processing.service.impl.DispatchConfiguration;

import hiconic.rx.security.model.test.RunSecured;
import hiconic.rx.security.model.test.SecurityTestRequest;

public class SecurityTestProcessor extends AbstractDispatchingServiceProcessor<SecurityTestRequest, Object> {
	@Override
	protected void configureDispatching(DispatchConfiguration<SecurityTestRequest, Object> dispatching) {
		dispatching.registerReasoned(RunSecured.T, this::runSecured);
	}
	
	private Maybe<Object> runSecured(ServiceRequestContext context, RunSecured request) {
		return Maybe.complete(null);
	}
}
